package flows

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func TestCommandRoundtripPersistsArtifacts(t *testing.T) {
	artifactRoot := t.TempDir()
	app := state.New(artifactRoot)
	mux := http.NewServeMux()
	mux.Handle("/exp01/ws", blocks.WSAcceptHandler(func(conn *websocket.Conn) {
		go RunClientSession(app, conn)
	}))
	mux.HandleFunc("POST /exp01/command", CommandRoundtripHandler(app))
	server := httptest.NewServer(mux)
	defer server.Close()

	wsURL, _ := url.Parse(server.URL)
	wsURL.Scheme = "ws"
	wsURL.Path = "/exp01/ws"
	conn, _, err := websocket.DefaultDialer.Dial(wsURL.String(), nil)
	if err != nil {
		t.Fatalf("dial failed: %v", err)
	}
	defer conn.Close()

	hello := proto.ClientHello{
		Type:            "client_hello",
		ProtocolVersion: "exp01",
		DeviceID:        "android-local-01",
		RuntimeVersion:  "0.1.0001",
		AppID:           "com.flowy.explore",
		Capabilities:    []string{"ping", "fetch-logs"},
		SentAt:          time.Now().Format(time.RFC3339),
	}
	if err := conn.WriteJSON(hello); err != nil {
		t.Fatalf("hello write failed: %v", err)
	}

	go func() {
		var command proto.CommandEnvelope
		_ = conn.ReadJSON(&command)
		response := proto.ResponseEnvelope{
			ProtocolVersion: "exp01",
			RequestID:       command.RequestID,
			RunID:           command.RunID,
			Command:         command.Command,
			Status:          "ok",
			StartedAt:       time.Now().Format(time.RFC3339),
			FinishedAt:      time.Now().Format(time.RFC3339),
			DurationMs:      50,
			Device:          proto.DeviceMeta{DeviceID: "android-local-01", Model: "Pixel", AndroidVersion: "14"},
			App:             proto.AppMeta{PackageName: "com.flowy.explore", RuntimeVersion: "0.1.0001"},
			Artifacts:       []proto.ArtifactDescriptor{},
			Message:         "pong",
			InlineLogs: []proto.LogEntry{{
				TS:      time.Now().Format(time.RFC3339),
				Level:   "info",
				Event:   "command_finished",
				RunID:   command.RunID,
				Command: command.Command,
				Message: "pong sent",
			}},
		}
		_ = conn.WriteJSON(response)
	}()

	body, _ := json.Marshal(proto.CommandDispatchRequest{
		DeviceID: "android-local-01",
		Command:  proto.CommandEnvelope{Command: "ping", Payload: map[string]any{}},
	})
	resp, err := http.Post(server.URL+"/exp01/command", "application/json", bytes.NewReader(body))
	if err != nil {
		t.Fatalf("post failed: %v", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("expected 200, got %d", resp.StatusCode)
	}

	dateDir := time.Now().Format("2006-01-02")
	entries, err := os.ReadDir(filepath.Join(artifactRoot, dateDir))
	if err != nil || len(entries) != 1 {
		t.Fatalf("expected one run dir, err=%v len=%d", err, len(entries))
	}
	runDir := filepath.Join(artifactRoot, dateDir, entries[0].Name())
	for _, name := range []string{"manifest.json", "response.json", "logs.txt"} {
		if _, statErr := os.Stat(filepath.Join(runDir, name)); statErr != nil {
			t.Fatalf("expected %s: %v", name, statErr)
		}
	}
}
