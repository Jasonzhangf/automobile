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

// ActiveRun holds a reference to a currently running collection flow.
type ActiveRun struct {
	StopFn  func()
	Running bool
}

type AppState struct {
	mu           sync.RWMutex
	clients      map[string]*ClientSession
	pending      map[string]chan proto.ResponseEnvelope
	artifactRoot string
	requestSeq   atomic.Int64
	activeRun    *ActiveRun
	activeRunMu  sync.Mutex
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

func (a *AppState) UpdateClientHello(deviceID string, hello proto.ClientHello) {
	a.mu.Lock()
	defer a.mu.Unlock()
	session, ok := a.clients[deviceID]
	if !ok {
		return
	}
	session.Hello = hello
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

// SetActiveRun registers a running collection flow.
func (a *AppState) SetActiveRun(stopFn func()) {
	a.activeRunMu.Lock()
	defer a.activeRunMu.Unlock()
	a.activeRun = &ActiveRun{StopFn: stopFn, Running: true}
}

// ClearActiveRun removes the active run registration.
func (a *AppState) ClearActiveRun() {
	a.activeRunMu.Lock()
	defer a.activeRunMu.Unlock()
	if a.activeRun != nil {
		a.activeRun.Running = false
	}
	a.activeRun = nil
}

// StopActiveRun stops the active run if one exists. Returns true if stopped.
func (a *AppState) StopActiveRun() bool {
	a.activeRunMu.Lock()
	defer a.activeRunMu.Unlock()
	if a.activeRun != nil && a.activeRun.Running {
		a.activeRun.StopFn()
		a.activeRun.Running = false
		return true
	}
	return false
}

// HasActiveRun returns true if a collection flow is running.
func (a *AppState) HasActiveRun() bool {
	a.activeRunMu.Lock()
	defer a.activeRunMu.Unlock()
	return a.activeRun != nil && a.activeRun.Running
}
