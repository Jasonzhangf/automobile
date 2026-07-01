package blocks

import (
	"net/http"
	"testing"

	"github.com/gorilla/websocket"
)

func TestWSAcceptHandler_ReturnsHandler(t *testing.T) {
	var calledConn *websocket.Conn
	handler := WSAcceptHandler(func(conn *websocket.Conn) {
		calledConn = conn
	})

	if handler == nil {
		t.Fatal("expected non-nil handler")
	}

	// Verify handler implements http.Handler
	var _ http.HandlerFunc = handler
	_ = calledConn
}

func TestRunClientFunc_Type(t *testing.T) {
	// Verify RunClientFunc is the expected type
	var fn RunClientFunc = func(conn *websocket.Conn) {}
	_ = fn
}
