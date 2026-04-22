package flows

import (
	"encoding/json"
	"net/http"
	"time"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func CommandRoundtripHandler(app *state.AppState) http.HandlerFunc {
	return func(writer http.ResponseWriter, request *http.Request) {
		var dispatch proto.CommandDispatchRequest
		if err := json.NewDecoder(request.Body).Decode(&dispatch); err != nil {
			http.Error(writer, err.Error(), http.StatusBadRequest)
			return
		}
		now := time.Now()
		if dispatch.Command.RequestID == "" {
			dispatch.Command.RequestID = foundation.NewRequestID(now, app.NextRequestSeq())
		}
		if dispatch.Command.RunID == "" {
			dispatch.Command.RunID = foundation.NewRunID(now, dispatch.Command.Command, "manual-dispatch")
		}
		if dispatch.Command.ProtocolVersion == "" {
			dispatch.Command.ProtocolVersion = "exp01"
		}
		if dispatch.Command.SentAt == "" {
			dispatch.Command.SentAt = now.Format(time.RFC3339)
		}
		if dispatch.Command.TimeoutMs <= 0 {
			dispatch.Command.TimeoutMs = 10000
		}

		session, ok := app.Client(dispatch.DeviceID)
		if !ok {
			http.Error(writer, "device not connected", http.StatusNotFound)
			return
		}

		pending := app.RegisterPending(dispatch.Command.RequestID)
		defer app.CancelPending(dispatch.Command.RequestID)
		if err := blocks.SendCommand(session, dispatch.Command); err != nil {
			http.Error(writer, err.Error(), http.StatusBadGateway)
			return
		}

		select {
		case response := <-pending:
			if _, err := FinalizeRun(app.ArtifactRoot(), response); err != nil {
				http.Error(writer, err.Error(), http.StatusInternalServerError)
				return
			}
			writer.Header().Set("Content-Type", "application/json")
			_ = json.NewEncoder(writer).Encode(response)
		case <-time.After(time.Duration(dispatch.Command.TimeoutMs) * time.Millisecond):
			http.Error(writer, "command timeout", http.StatusGatewayTimeout)
		}
	}
}
