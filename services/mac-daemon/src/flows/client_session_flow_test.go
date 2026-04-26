package flows

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func TestClientHelloRefreshUpdatesCapabilities(t *testing.T) {
	app := state.New(t.TempDir())
	mux := http.NewServeMux()
	mux.Handle("/exp01/ws", blocks.WSAcceptHandler(func(conn *websocket.Conn) {
		go RunClientSession(app, conn)
	}))
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

	first := proto.ClientHello{
		Type:            "client_hello",
		ProtocolVersion: "exp01",
		DeviceID:        "android-local-01",
		RuntimeVersion:  "0.1.0058",
		AppID:           "com.flowy.explore",
		Capabilities:    []string{"ping", "fetch-logs", "capture-screenshot"},
		SentAt:          time.Now().Format(time.RFC3339),
	}
	if err := conn.WriteJSON(first); err != nil {
		t.Fatalf("first hello write failed: %v", err)
	}

	second := first
	second.Capabilities = []string{
		"ping", "fetch-logs", "capture-screenshot",
		"dump-accessibility-tree", "tap", "scroll", "input-text", "back", "press-key",
	}
	second.SentAt = time.Now().Add(time.Second).Format(time.RFC3339)
	if err := conn.WriteJSON(second); err != nil {
		t.Fatalf("second hello write failed: %v", err)
	}

	time.Sleep(100 * time.Millisecond)
	clients := app.Clients()
	if len(clients) != 1 {
		t.Fatalf("expected 1 client, got %d", len(clients))
	}
	if len(clients[0].Capabilities) != len(second.Capabilities) {
		t.Fatalf("expected capabilities to refresh to %d items, got %d", len(second.Capabilities), len(clients[0].Capabilities))
	}
}
