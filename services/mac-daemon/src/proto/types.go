package proto

type ClientHello struct {
	Type            string   `json:"type"`
	ProtocolVersion string   `json:"protocolVersion"`
	DeviceID        string   `json:"deviceId"`
	RuntimeVersion  string   `json:"runtimeVersion"`
	AppID           string   `json:"appId"`
	Capabilities    []string `json:"capabilities"`
	SentAt          string   `json:"sentAt"`
}

type CommandEnvelope struct {
	ProtocolVersion string `json:"protocolVersion"`
	RequestID       string `json:"requestId"`
	RunID           string `json:"runId"`
	Command         string `json:"command"`
	SentAt          string `json:"sentAt"`
	TimeoutMs       int    `json:"timeoutMs"`
	Payload         any    `json:"payload"`
}

type DeviceMeta struct {
	DeviceID       string `json:"deviceId"`
	Model          string `json:"model"`
	AndroidVersion string `json:"androidVersion"`
}

type AppMeta struct {
	PackageName    string `json:"packageName"`
	RuntimeVersion string `json:"runtimeVersion"`
}

type ErrorObject struct {
	Code    string         `json:"code"`
	Message string         `json:"message"`
	Details map[string]any `json:"details,omitempty"`
}

type ArtifactDescriptor struct {
	Kind        string `json:"kind"`
	FileName    string `json:"fileName"`
	ContentType string `json:"contentType"`
	SHA256      string `json:"sha256,omitempty"`
	SizeBytes   int64  `json:"sizeBytes,omitempty"`
}

type LogEntry struct {
	TS        string `json:"ts"`
	Level     string `json:"level"`
	Event     string `json:"event"`
	RequestID string `json:"requestId,omitempty"`
	RunID     string `json:"runId,omitempty"`
	Command   string `json:"command,omitempty"`
	Message   string `json:"message"`
}

type ResponseEnvelope struct {
	ProtocolVersion string               `json:"protocolVersion"`
	RequestID       string               `json:"requestId"`
	RunID           string               `json:"runId"`
	Command         string               `json:"command"`
	Status          string               `json:"status"`
	StartedAt       string               `json:"startedAt"`
	FinishedAt      string               `json:"finishedAt"`
	DurationMs      int64                `json:"durationMs"`
	Device          DeviceMeta           `json:"device"`
	App             AppMeta              `json:"app"`
	Artifacts       []ArtifactDescriptor `json:"artifacts"`
	Error           *ErrorObject         `json:"error"`
	Message         string               `json:"message"`
	InlineLogs      []LogEntry           `json:"inlineLogs,omitempty"`
}

type RunManifest struct {
	RunID           string     `json:"runId"`
	RequestID       string     `json:"requestId"`
	ProtocolVersion string     `json:"protocolVersion"`
	Command         string     `json:"command"`
	CapturedAt      string     `json:"capturedAt"`
	Status          string     `json:"status"`
	Device          DeviceMeta `json:"device"`
	App             AppMeta    `json:"app"`
	Files           []string   `json:"files"`
}

type CommandDispatchRequest struct {
	DeviceID string          `json:"deviceId"`
	Command  CommandEnvelope `json:"command"`
}

type ArtifactUploadMeta struct {
	ProtocolVersion string `json:"protocolVersion"`
	RequestID       string `json:"requestId"`
	RunID           string `json:"runId"`
	DeviceID        string `json:"deviceId"`
	Command         string `json:"command,omitempty"`
	Kind            string `json:"kind"`
	FileName        string `json:"fileName"`
	ContentType     string `json:"contentType"`
}

type ArtifactUploadResponse struct {
	ProtocolVersion string             `json:"protocolVersion"`
	RequestID       string             `json:"requestId"`
	RunID           string             `json:"runId"`
	Stored          ArtifactDescriptor `json:"stored"`
}
