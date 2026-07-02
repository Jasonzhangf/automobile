package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

// RecoveryConfig controls session recovery behavior.
type RecoveryConfig struct {
	MaxRetries  int
	PingTimeout int
}

// RecoveryResult summarizes the outcome of a recovery attempt.
type RecoveryResult struct {
	Recovered bool   `json:"recovered"`
	Reason    string `json:"reason"`
}

// DefaultRecoveryConfig returns sensible defaults.
func DefaultRecoveryConfig() RecoveryConfig {
	return RecoveryConfig{
		MaxRetries:  3,
		PingTimeout: 8000,
	}
}

// HealthCheck sends a ping command and waits for the response.
func HealthCheck(session *state.ClientSession, app *state.AppState, timeoutMs int) error {
	tr := makeTransport(session, app)
	return healthCheckWithTransport(tr, timeoutMs)
}

func healthCheckWithTransport(tr *Transport, timeoutMs int) error {
	if timeoutMs <= 0 {
		timeoutMs = 8000
	}
	cmd := NewCommand(tr, proto.CmdPing, map[string]any{})
	cmd.TimeoutMs = timeoutMs

	resp, err := transportRoundtrip(tr, cmd)
	if err != nil {
		return fmt.Errorf("health check failed: %w", err)
	}
	if resp.Status != "ok" {
		errMsg := ""
		if resp.Error != nil {
			errMsg = resp.Error.Message
		}
		return fmt.Errorf("health check returned error: %s", errMsg)
	}
	return nil
}

// WaitForReconnectTransport polls the device registry until the device reconnects or times out.
func WaitForReconnectTransport(tr *Transport, deviceID string, maxWaitMs int) bool {
	if maxWaitMs <= 0 {
		maxWaitMs = 30000
	}
	deadline := time.Now().Add(time.Duration(maxWaitMs) * time.Millisecond)
	for time.Now().Before(deadline) {
		if tr.ClientExists(deviceID) {
			return true
		}
		time.Sleep(1 * time.Second)
	}
	return false
}
