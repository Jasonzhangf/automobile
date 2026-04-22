package blocks

import (
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func SendCommand(session *state.ClientSession, command proto.CommandEnvelope) error {
	session.SendMu.Lock()
	defer session.SendMu.Unlock()
	return session.Conn.WriteJSON(command)
}
