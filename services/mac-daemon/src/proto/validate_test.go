package proto

import (
	"strings"
	"testing"
)

func TestValidateCommandEnvelope_Success(t *testing.T) {
	v := NewSchemaValidator()
	cmd := CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req-1-abc-1700000000",
		RunID:           "run-1700000000-xyz-tap",
		Command:         "tap",
		SentAt:          "2026-07-01T00:00:00Z",
		TimeoutMs:       10000,
		Payload:         map[string]any{"x": 100, "y": 200},
	}
	if err := v.ValidateCommandEnvelope(cmd); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestValidateCommandEnvelope_ProtocolVersion(t *testing.T) {
	v := NewSchemaValidator()
	cases := []struct {
		name    string
		version string
		wantErr bool
	}{
		{"empty", "", true},
		{"wrong", "v1", true},
		{"correct", "exp01", false},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			cmd := CommandEnvelope{
				ProtocolVersion: tc.version,
				RequestID:       "req-1-abc-1700000000",
				RunID:           "run-1700000000-xyz-tap",
				Command:         "tap",
				SentAt:          "2026-07-01T00:00:00Z",
				TimeoutMs:       10000,
			}
			err := v.ValidateCommandEnvelope(cmd)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for version %q, got nil", tc.version)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("expected no error for %q, got: %v", tc.version, err)
			}
		})
	}
}

func TestValidateCommandEnvelope_RequestID(t *testing.T) {
	v := NewSchemaValidator()
	cases := []struct {
		id      string
		wantErr bool
	}{
		{"", true},
		{"req-1-abc-1700000000", false},
		{"req-1-ABC-1700000000", true},  // uppercase not allowed
		{"req-abc", true},                // too short
		{"requestid-1-abc-1700000000", true}, // wrong prefix
		{"req-1-abc", true},              // missing ms
	}
	for _, tc := range cases {
		t.Run(tc.id, func(t *testing.T) {
			cmd := CommandEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       tc.id,
				RunID:           "run-1700000000-xyz-tap",
				Command:         "tap",
				SentAt:          "2026-07-01T00:00:00Z",
				TimeoutMs:       10000,
			}
			err := v.ValidateCommandEnvelope(cmd)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for id %q, got nil", tc.id)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("expected no error for %q, got: %v", tc.id, err)
			}
		})
	}
}

func TestValidateCommandEnvelope_Command(t *testing.T) {
	v := NewSchemaValidator()
	cases := []struct {
		cmd     string
		wantErr bool
	}{
		{"tap", false},
		{"dump-ui-tree-root", false},
		{"press-key", false},
		{"", true},
		{"Tap", true},     // uppercase not allowed
		{"tap_button", true}, // underscore not allowed
		{"tap button", true}, // space not allowed
	}
	for _, tc := range cases {
		t.Run(tc.cmd, func(t *testing.T) {
			cmd := CommandEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       "req-1-abc-1700000000",
				RunID:           "run-1700000000-xyz-tap",
				Command:         tc.cmd,
				SentAt:          "2026-07-01T00:00:00Z",
				TimeoutMs:       10000,
			}
			err := v.ValidateCommandEnvelope(cmd)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for command %q, got nil", tc.cmd)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("expected no error for %q, got: %v", tc.cmd, err)
			}
		})
	}
}

func TestValidateCommandEnvelope_TimeoutMs(t *testing.T) {
	v := NewSchemaValidator()
	cases := []struct {
		timeoutMs int
		wantErr   bool
	}{
		{0, false},
		{10000, false},
		{600000, false},
		{-1, true},
		{600001, true},
		{9999999, true},
	}
	for _, tc := range cases {
		cmd := CommandEnvelope{
			ProtocolVersion: "exp01",
			RequestID:       "req-1-abc-1700000000",
			RunID:           "run-1700000000-xyz-tap",
			Command:         "tap",
			SentAt:          "2026-07-01T00:00:00Z",
			TimeoutMs:       tc.timeoutMs,
		}
		err := v.ValidateCommandEnvelope(cmd)
		if tc.wantErr && err == nil {
			t.Errorf("expected error for timeoutMs %d", tc.timeoutMs)
		}
		if !tc.wantErr && err != nil {
			t.Errorf("expected no error for %d, got: %v", tc.timeoutMs, err)
		}
	}
}

func TestValidateResponseEnvelope_Success(t *testing.T) {
	v := NewSchemaValidator()
	resp := ResponseEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req-1-abc-1700000000",
		RunID:           "run-1700000000-xyz-tap",
		Command:         "tap",
		Status:          "ok",
		StartedAt:       "2026-07-01T00:00:00Z",
		FinishedAt:      "2026-07-01T00:00:01Z",
		DurationMs:      1000,
		Device:          DeviceMeta{DeviceID: "OP64DDL1"},
		App:             AppMeta{PackageName: "com.example", RuntimeVersion: "0.1.0001"},
		Artifacts:       []ArtifactDescriptor{},
		Error:           nil,
		Message:         "success",
	}
	if err := v.ValidateResponseEnvelope(resp); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestValidateResponseEnvelope_Status(t *testing.T) {
	v := NewSchemaValidator()
	cases := []struct {
		status  string
		wantErr bool
	}{
		{"ok", false},
		{"error", false},
		{"timeout", false},
		{"aborted", false},
		{"", true},
		{"success", true},
		{"failed", true},
	}
	for _, tc := range cases {
		t.Run(tc.status, func(t *testing.T) {
			resp := ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       "req-1-abc-1700000000",
				RunID:           "run-1700000000-xyz-tap",
				Command:         "tap",
				Status:          tc.status,
				StartedAt:       "2026-07-01T00:00:00Z",
				FinishedAt:      "2026-07-01T00:00:01Z",
				DurationMs:      1000,
				Device:          DeviceMeta{DeviceID: "OP64DDL1"},
				App:             AppMeta{PackageName: "com.example", RuntimeVersion: "0.1.0001"},
				Artifacts:       []ArtifactDescriptor{},
				Error:           nil,
				Message:         "",
			}
			err := v.ValidateResponseEnvelope(resp)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for status %q", tc.status)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("expected no error for %q, got: %v", tc.status, err)
			}
		})
	}
}

func TestValidateResponseEnvelope_ErrorCode(t *testing.T) {
	v := NewSchemaValidator()
	cases := []struct {
		code    string
		wantErr bool
	}{
		{"OK", false},
		{"TARGET_NOT_FOUND", false},
		{"TIMEOUT", false},
		{"", true},
		{"TargetNotFound", true},   // mixed case not allowed
		{"TARGET_NOT-FOUND", true}, // dash not allowed
		{"target_not_found", true}, // lowercase not allowed
	}
	for _, tc := range cases {
		t.Run(tc.code, func(t *testing.T) {
			resp := ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       "req-1-abc-1700000000",
				RunID:           "run-1700000000-xyz-tap",
				Command:         "tap",
				Status:          "error",
				StartedAt:       "2026-07-01T00:00:00Z",
				FinishedAt:      "2026-07-01T00:00:01Z",
				DurationMs:      1000,
				Device:          DeviceMeta{DeviceID: "OP64DDL1"},
				App:             AppMeta{PackageName: "com.example", RuntimeVersion: "0.1.0001"},
				Artifacts:       []ArtifactDescriptor{},
				Error:           &ErrorObject{Code: tc.code, Message: "fail"},
				Message:         "",
			}
			err := v.ValidateResponseEnvelope(resp)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for code %q", tc.code)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("expected no error for %q, got: %v", tc.code, err)
			}
		})
	}
}

func TestMarshalWithValidation(t *testing.T) {
	v := NewSchemaValidator()
	cmd := CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req-1-abc-1700000000",
		RunID:           "run-1700000000-xyz-tap",
		Command:         "tap",
		SentAt:          "2026-07-01T00:00:00Z",
		TimeoutMs:       10000,
		Payload:         map[string]any{"x": 100, "y": 200},
	}
	data, err := v.MarshalWithValidation(cmd)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	if !strings.Contains(string(data), `"command":"tap"`) {
		t.Errorf("expected command=tap in output, got: %s", data)
	}
}

func TestMarshalWithValidation_Rejects(t *testing.T) {
	v := NewSchemaValidator()
	cmd := CommandEnvelope{
		ProtocolVersion: "v1", // wrong
		RequestID:       "req-1-abc-1700000000",
		RunID:           "run-1700000000-xyz-tap",
		Command:         "tap",
		SentAt:          "2026-07-01T00:00:00Z",
		TimeoutMs:       10000,
	}
	_, err := v.MarshalWithValidation(cmd)
	if err == nil {
		t.Fatal("expected error for invalid version")
	}
}

func TestUnmarshalWithValidation(t *testing.T) {
	v := NewSchemaValidator()
	data := []byte(`{"protocolVersion":"exp01","requestId":"req-1-abc-1700000000","runId":"run-1700000000-xyz-tap","command":"tap","sentAt":"2026-07-01T00:00:00Z","timeoutMs":10000,"payload":{"x":100}}`)
	cmd, err := v.UnmarshalWithValidation(data)
	if err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if cmd.Command != "tap" {
		t.Errorf("expected command=tap, got %q", cmd.Command)
	}
}

func TestUnmarshalWithValidation_Rejects(t *testing.T) {
	v := NewSchemaValidator()
	data := []byte(`{"protocolVersion":"v2","requestId":"req-1-abc-1700000000","runId":"run-1700000000-xyz-tap","command":"tap","sentAt":"2026-07-01T00:00:00Z","timeoutMs":10000}`)
	_, err := v.UnmarshalWithValidation(data)
	if err == nil {
		t.Fatal("expected error for v2 protocol version")
	}
}

func TestLoadSchemasFromDir(t *testing.T) {
	v := NewSchemaValidator()
	if err := v.LoadSchemasFromDir("../../../packages/protocol/control"); err != nil {
		t.Skipf("schema dir not accessible from test: %v", err)
	}
	if v.schemas["command-envelope.schema.json"] == nil {
		t.Error("expected command-envelope schema to be loaded")
	}
}
