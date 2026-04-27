package blocks

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func TestObservePage_ParsesTreeArtifact(t *testing.T) {
	artifactRoot := t.TempDir()
	app := state.New(artifactRoot)

	// Server-side WS handler: register client + loop reading responses.
	mux := http.NewServeMux()
	mux.Handle("/exp01/ws", WSAcceptHandler(func(conn *websocket.Conn) {
		go func() {
			defer conn.Close()
			_, data, err := conn.ReadMessage()
			if err != nil {
				return
			}
			var hello proto.ClientHello
			if json.Unmarshal(data, &hello) != nil || hello.DeviceID == "" {
				return
			}
			sess := &state.ClientSession{Hello: hello, Conn: conn, ConnectedAt: time.Now()}
			app.RegisterClient(sess)
			defer app.RemoveClient(hello.DeviceID)
			for {
				_, raw, readErr := conn.ReadMessage()
				if readErr != nil {
					return
				}
				var resp proto.ResponseEnvelope
				if json.Unmarshal(raw, &resp) == nil && resp.RequestID != "" {
					app.ResolvePending(resp)
				}
			}
		}()
	}))

	server := httptest.NewServer(mux)
	defer server.Close()

	wsURL, _ := url.Parse(server.URL)
	wsURL.Scheme = "ws"
	wsURL.Path = "/exp01/ws"
	clientConn, _, err := websocket.DefaultDialer.Dial(wsURL.String(), nil)
	if err != nil {
		t.Fatalf("dial failed: %v", err)
	}
	defer clientConn.Close()

	// Client side: send hello.
	hello := proto.ClientHello{
		Type:            "client_hello",
		ProtocolVersion: "exp01",
		DeviceID:        "test-device",
		RuntimeVersion:  "0.1.0108",
		AppID:           "com.flowy.explore",
		Capabilities:    []string{"dump-ui-tree-root"},
		SentAt:          time.Now().Format(time.RFC3339),
	}
	clientConn.WriteJSON(hello)
	time.Sleep(100 * time.Millisecond)

	// Client-side goroutine: reads command from server, writes fixture, sends response.
	go func() {
		var cmd proto.CommandEnvelope
		if err := clientConn.ReadJSON(&cmd); err != nil {
			return
		}
		runID := cmd.RunID
		runDir := filepath.Join(artifactRoot, runID[:10], runID)
		os.MkdirAll(runDir, 0755)
		fixture := map[string]any{
			"packageName": "com.xingin.xhs",
			"nodes": []map[string]any{
				{"text": "Hello", "className": "android.widget.TextView",
					"flags": map[string]bool{"clickable": false, "enabled": true}},
				{"text": "", "contentDescription": "Search", "className": "android.widget.ImageView",
					"flags": map[string]bool{"clickable": true, "enabled": true}},
				{"text": "Title", "className": "android.widget.LinearLayout",
					"flags": map[string]bool{"clickable": true, "enabled": true},
					"boundsInScreen": map[string]int{"left": 0, "top": 0, "right": 1216, "bottom": 2640}},
			},
		}
		fixtureJSON, _ := json.Marshal(fixture)
		os.WriteFile(filepath.Join(runDir, "root-ui-tree.json"), fixtureJSON, 0644)
		os.WriteFile(filepath.Join(runDir, "manifest.json"), []byte("{}"), 0644)
		os.WriteFile(filepath.Join(runDir, "response.json"), []byte("{}"), 0644)

		clientConn.WriteJSON(proto.ResponseEnvelope{
			ProtocolVersion: "exp01",
			RequestID:       cmd.RequestID,
			RunID:           runID,
			Command:         cmd.Command,
			Status:          "ok",
			StartedAt:       time.Now().Format(time.RFC3339),
			FinishedAt:      time.Now().Format(time.RFC3339),
			DurationMs:      100,
			Device:          proto.DeviceMeta{DeviceID: "test-device", Model: "PLZ110", AndroidVersion: "16"},
			App:             proto.AppMeta{PackageName: "com.flowy.explore", RuntimeVersion: "0.1.0108"},
			Artifacts: []proto.ArtifactDescriptor{
				{Kind: "root-ui-tree", FileName: "root-ui-tree.json", ContentType: "application/json"},
			},
			Message: "dump-ui-tree-root:3",
		})
	}()

	// Get the registered server-side session and call ObservePage.
	session, ok := app.Client("test-device")
	if !ok {
		t.Fatal("client not registered after hello")
	}

	result, err := ObservePage(session, app, artifactRoot, 10000)
	if err != nil {
		t.Fatalf("ObservePage failed: %v", err)
	}
	if result.PackageName != "com.xingin.xhs" {
		t.Fatalf("expected packageName com.xingin.xhs, got %s", result.PackageName)
	}
	if result.NodeCount != 3 {
		t.Fatalf("expected 3 nodes, got %d", result.NodeCount)
	}
	if result.Nodes[0].Text != "Hello" {
		t.Fatalf("expected first node text 'Hello', got %s", result.Nodes[0].Text)
	}
	if result.Nodes[1].ContentDescription != "Search" {
		t.Fatalf("expected second node desc 'Search', got %s", result.Nodes[1].ContentDescription)
	}
	if !result.Nodes[1].Flags.Clickable {
		t.Fatal("expected second node to be clickable")
	}
	if result.Nodes[2].BoundsInScreen == nil {
		t.Fatal("expected third node to have bounds")
	}
	cx, cy := result.Nodes[2].BoundsInScreen.Center()
	if cx != 608 || cy != 1320 {
		t.Fatalf("expected center (608,1320), got (%d,%d)", cx, cy)
	}
}
