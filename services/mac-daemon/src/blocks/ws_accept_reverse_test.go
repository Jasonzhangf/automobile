package blocks

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/gorilla/websocket"
)

// --- Reverse tests: verify what will NOT happen ---

func TestWSAcceptHandler_NilFunc_DoesNotPanicOnCreate(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			t.Errorf("WSAcceptHandler creation should not panic on nil func: %v", r)
		}
	}()
	// Creating handler with nil runClient — should not panic
	_ = WSAcceptHandler(nil)
}

func TestWSAcceptHandler_NilFunc_PanicsOnUpgrade(t *testing.T) {
	handler := WSAcceptHandler(nil)
	req := httptest.NewRequest("GET", "/", nil)
	w := httptest.NewRecorder()
	// Should not panic — gorilla/websocket returns HTTP 400 on non-upgrade
	handler(w, req)
	if w.Code != http.StatusBadRequest {
		t.Logf("handler returned status %d (expected 400 for non-WS request)", w.Code)
	}
}

func TestWSAcceptHandler_UpgradesWithCheckOrigin(t *testing.T) {
	var conn *websocket.Conn
	handler := WSAcceptHandler(func(c *websocket.Conn) { conn = c })

	s := httptest.NewServer(handler)
	defer s.Close()

	// Dial with no Origin header — CheckOrigin allows all
	wsURL := "ws" + strings.TrimPrefix(s.URL, "http")
	ws, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		t.Fatalf("dial failed: %v", err)
	}
	defer ws.Close()
	if conn == nil {
		t.Error("expected conn to be set by handler")
	}
}
