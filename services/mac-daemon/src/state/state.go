package state

import (
	"sync"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/proto"
)

type ClientSession struct {
	Hello       proto.ClientHello
	Conn        *websocket.Conn
	ConnectedAt time.Time
	SendMu      sync.Mutex
}

type AppState struct {
	mu           sync.RWMutex
	clients      map[string]*ClientSession
	pending      map[string]chan proto.ResponseEnvelope
	artifactRoot string
	requestSeq   atomic.Int64
}

func New(artifactRoot string) *AppState {
	return &AppState{
		clients:      map[string]*ClientSession{},
		pending:      map[string]chan proto.ResponseEnvelope{},
		artifactRoot: artifactRoot,
	}
}

func (a *AppState) ArtifactRoot() string { return a.artifactRoot }

func (a *AppState) NextRequestSeq() int64 { return a.requestSeq.Add(1) }

func (a *AppState) RegisterClient(session *ClientSession) {
	a.mu.Lock()
	defer a.mu.Unlock()
	a.clients[session.Hello.DeviceID] = session
}

func (a *AppState) RemoveClient(deviceID string) {
	a.mu.Lock()
	defer a.mu.Unlock()
	delete(a.clients, deviceID)
}

func (a *AppState) Client(deviceID string) (*ClientSession, bool) {
	a.mu.RLock()
	defer a.mu.RUnlock()
	session, ok := a.clients[deviceID]
	return session, ok
}

func (a *AppState) Clients() []proto.ClientHello {
	a.mu.RLock()
	defer a.mu.RUnlock()
	result := make([]proto.ClientHello, 0, len(a.clients))
	for _, session := range a.clients {
		result = append(result, session.Hello)
	}
	return result
}

func (a *AppState) RegisterPending(requestID string) chan proto.ResponseEnvelope {
	ch := make(chan proto.ResponseEnvelope, 1)
	a.mu.Lock()
	defer a.mu.Unlock()
	a.pending[requestID] = ch
	return ch
}

func (a *AppState) ResolvePending(response proto.ResponseEnvelope) bool {
	a.mu.Lock()
	defer a.mu.Unlock()
	ch, ok := a.pending[response.RequestID]
	if !ok {
		return false
	}
	ch <- response
	delete(a.pending, response.RequestID)
	return true
}

func (a *AppState) CancelPending(requestID string) {
	a.mu.Lock()
	defer a.mu.Unlock()
	delete(a.pending, requestID)
}
