package blocks

import (
	"net/http"

	"github.com/gorilla/websocket"
)

type RunClientFunc func(*websocket.Conn)

func WSAcceptHandler(runClient RunClientFunc) http.HandlerFunc {
	upgrader := websocket.Upgrader{
		ReadBufferSize:  4096,
		WriteBufferSize: 4096,
		CheckOrigin:     func(*http.Request) bool { return true },
	}

	return func(writer http.ResponseWriter, request *http.Request) {
		conn, err := upgrader.Upgrade(writer, request, nil)
		if err != nil {
			http.Error(writer, err.Error(), http.StatusBadRequest)
			return
		}
		runClient(conn)
	}
}
