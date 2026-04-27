package blocks

import (
	"fmt"
	"os/exec"
	"strings"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

// RecoveryConfig controls recovery behavior.
type RecoveryConfig struct {
	MaxRetries      int `json:"maxRetries"`      // max recovery attempts (default 3)
	ReconnectWaitMs int `json:"reconnectWaitMs"` // max wait for WS reconnect (default 30000)
	PingTimeoutMs   int `json:"pingTimeoutMs"`   // ping timeout (default 8000)
}

// DefaultRecoveryConfig returns sensible defaults.
func DefaultRecoveryConfig() RecoveryConfig {
	return RecoveryConfig{
		MaxRetries:      3,
		ReconnectWaitMs: 30000,
		PingTimeoutMs:   8000,
	}
}

// RecoveryResult describes what recovery did.
type RecoveryResult struct {
	Action  string `json:"action"`  // "none", "reconnected", "app_restarted"
	Retries int    `json:"retries"`
	Error   string `json:"error,omitempty"`
}

// HealthCheck pings the device via WS to confirm the connection is alive.
// Returns nil if healthy, error if not.
func HealthCheck(session *state.ClientSession, app *state.AppState, timeoutMs int) error {
	if timeoutMs <= 0 {
		timeoutMs = 8000
	}
	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       foundation.NewRequestID(time.Now(), app.NextRequestSeq()),
		RunID:           foundation.NewRunID(time.Now(), "ping", "health-check"),
		Command:         "ping",
		SentAt:          time.Now().Format(time.RFC3339),
		TimeoutMs:       timeoutMs,
		Payload:         map[string]any{},
	}
	resp, err := CommandRoundtrip(session, app, cmd)
	if err != nil {
		return fmt.Errorf("health check ping failed: %w", err)
	}
	if resp.Status != "ok" {
		return fmt.Errorf("health check ping returned: %s", resp.Status)
	}
	return nil
}

// RestartApp force-stops and relaunches the Flowy app on the Android device
// using adb. This is a best-effort operation; the WS connection will need
// to re-establish after the app restarts.
func RestartApp(adbSerial, packageName, activityName string) error {
	// Force stop
	if err := runAdb(adbSerial, "shell", "am", "force-stop", packageName); err != nil {
		return fmt.Errorf("force-stop failed: %w", err)
	}
	time.Sleep(2 * time.Second)

	// Relaunch
	component := packageName + "/" + activityName
	if err := runAdb(adbSerial, "shell", "am", "start", "-n", component); err != nil {
		return fmt.Errorf("relaunch failed: %w", err)
	}
	return nil
}

// WaitForReconnect polls the WS client list until a device reconnects.
// This is used when the WS dropped but we don't want to restart the app.
// The caller should have a reference to app.Client() polling.
// Since we don't have direct HTTP polling in blocks, this is a simple
// time-based wait that the caller orchestrates.
//
// In practice, the collection flow should call HealthCheck in a loop
// with jitter sleeps between attempts.
func WaitForReconnect(
	app *state.AppState,
	deviceID string,
	maxWaitMs int,
) bool {
	if maxWaitMs <= 0 {
		maxWaitMs = 30000
	}
	deadline := time.Now().Add(time.Duration(maxWaitMs) * time.Millisecond)
	for time.Now().Before(deadline) {
		if _, ok := app.Client(deviceID); ok {
			return true
		}
		time.Sleep(1 * time.Second)
	}
	return false
}

// RecoverSession attempts to recover a broken session.
// Strategy: ping → wait reconnect → restart app → wait reconnect again.
func RecoverSession(
	session *state.ClientSession,
	app *state.AppState,
	adbSerial string,
	cfg RecoveryConfig,
) *RecoveryResult {
	if cfg.MaxRetries <= 0 {
		cfg.MaxRetries = 3
	}
	if cfg.ReconnectWaitMs <= 0 {
		cfg.ReconnectWaitMs = 30000
	}
	if cfg.PingTimeoutMs <= 0 {
		cfg.PingTimeoutMs = 8000
	}

	deviceID := session.Hello.DeviceID

	for attempt := 0; attempt < cfg.MaxRetries; attempt++ {
		// Try ping first
		if err := HealthCheck(session, app, cfg.PingTimeoutMs); err == nil {
			return &RecoveryResult{Action: "none", Retries: attempt}
		}

		// Wait for auto-reconnect (watchdog on phone should trigger)
		if WaitForReconnect(app, deviceID, cfg.ReconnectWaitMs) {
			// Re-fetch session
			if newSession, ok := app.Client(deviceID); ok {
				if err := HealthCheck(newSession, app, cfg.PingTimeoutMs); err == nil {
					return &RecoveryResult{Action: "reconnected", Retries: attempt + 1}
				}
			}
		}

		// Last resort: restart the app
		if attempt == cfg.MaxRetries-1 {
			break
		}
		_ = RestartApp(adbSerial, "com.flowy.explore", ".ui.DevPanelActivity")

		// Wait for WS after restart
		if WaitForReconnect(app, deviceID, cfg.ReconnectWaitMs) {
			if newSession, ok := app.Client(deviceID); ok {
				if err := HealthCheck(newSession, app, cfg.PingTimeoutMs); err == nil {
					return &RecoveryResult{Action: "app_restarted", Retries: attempt + 1}
				}
			}
		}
	}

	return &RecoveryResult{
		Action:  "failed",
		Retries: cfg.MaxRetries,
		Error:   fmt.Sprintf("recovery failed after %d attempts", cfg.MaxRetries),
	}
}

// runAdb executes an adb command and returns any error.
func runAdb(serial string, args ...string) error {
	fullArgs := append([]string{"-s", serial}, args...)
	cmd := exec.Command("adb", fullArgs...)
	out, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("adb %s: %w (%s)", strings.Join(args, " "), err, strings.TrimSpace(string(out)))
	}
	return nil
}
