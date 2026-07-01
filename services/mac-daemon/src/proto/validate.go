package proto

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sync"
)

// SchemaValidator validates WS envelopes against JSON schema.
// In dev, schemas are loaded from packages/protocol/control/*.schema.json.
// In prod, schemas can be embedded.
type SchemaValidator struct {
	mu        sync.RWMutex
	schemas   map[string][]byte
	strict    bool
	commandRe *regexp.Regexp
	protoVer  string
}

// NewSchemaValidator constructs a validator with default settings.
func NewSchemaValidator() *SchemaValidator {
	return &SchemaValidator{
		schemas:   make(map[string][]byte),
		strict:    true,
		commandRe: regexp.MustCompile(`^[a-z][a-z0-9-]+$`),
		protoVer:  "exp01",
	}
}

// LoadSchemasFromDir reads all .schema.json files in the given directory.
func (v *SchemaValidator) LoadSchemasFromDir(dir string) error {
	v.mu.Lock()
	defer v.mu.Unlock()
	entries, err := os.ReadDir(dir)
	if err != nil {
		return fmt.Errorf("read schema dir: %w", err)
	}
	for _, e := range entries {
		if e.IsDir() || filepath.Ext(e.Name()) != ".json" {
			continue
		}
		path := filepath.Join(dir, e.Name())
		data, err := os.ReadFile(path)
		if err != nil {
			return fmt.Errorf("read %s: %w", e.Name(), err)
		}
		v.schemas[e.Name()] = data
	}
	return nil
}

// ValidateCommandEnvelope checks a CommandEnvelope against the command-envelope schema.
// Lightweight in-process validation (no full JSON schema engine).
func (v *SchemaValidator) ValidateCommandEnvelope(cmd CommandEnvelope) error {
	v.mu.RLock()
	defer v.mu.RUnlock()

	if !v.strict {
		return nil
	}
	if cmd.ProtocolVersion != v.protoVer {
		return fmt.Errorf("invalid protocolVersion: %q (want %q)", cmd.ProtocolVersion, v.protoVer)
	}
	if cmd.RequestID == "" {
		return fmt.Errorf("requestId is required")
	}
	if !isValidRequestID(cmd.RequestID) {
		return fmt.Errorf("invalid requestId format: %q (want req-<seq>-<rand>-<ms>)", cmd.RequestID)
	}
	if cmd.RunID == "" {
		return fmt.Errorf("runId is required")
	}
	if !isValidRunID(cmd.RunID) {
		return fmt.Errorf("invalid runId format: %q (want run-<ms>-<rand>-<purpose>)", cmd.RunID)
	}
	if !v.commandRe.MatchString(cmd.Command) {
		return fmt.Errorf("invalid command name: %q (want lowercase-kebab)", cmd.Command)
	}
	if cmd.TimeoutMs < 0 || cmd.TimeoutMs > 600000 {
		return fmt.Errorf("invalid timeoutMs: %d (must be 0-600000)", cmd.TimeoutMs)
	}
	return nil
}

// ValidateResponseEnvelope checks a ResponseEnvelope.
func (v *SchemaValidator) ValidateResponseEnvelope(resp ResponseEnvelope) error {
	v.mu.RLock()
	defer v.mu.RUnlock()

	if !v.strict {
		return nil
	}
	if resp.ProtocolVersion != v.protoVer {
		return fmt.Errorf("invalid protocolVersion: %q", resp.ProtocolVersion)
	}
	if resp.RequestID == "" {
		return fmt.Errorf("requestId is required")
	}
	if resp.Status != "ok" && resp.Status != "error" && resp.Status != "timeout" && resp.Status != "aborted" {
		return fmt.Errorf("invalid status: %q", resp.Status)
	}
	if resp.Error != nil {
		if resp.Error.Code == "" {
			return fmt.Errorf("error.code is required when error is set")
		}
		if !isValidErrorCode(resp.Error.Code) {
			return fmt.Errorf("invalid error.code: %q (want UPPER_SNAKE)", resp.Error.Code)
		}
	}
	return nil
}

var requestIDRe = regexp.MustCompile(`^req-\d+-[a-z0-9-]+-\d+$`)
var runIDRe = regexp.MustCompile(`^run-\d+-[a-z0-9-]+-.+$`)
var errorCodeRe = regexp.MustCompile(`^[A-Z_]+$`)

func isValidRequestID(s string) bool { return requestIDRe.MatchString(s) }
func isValidRunID(s string) bool     { return runIDRe.MatchString(s) }
func isValidErrorCode(s string) bool { return errorCodeRe.MatchString(s) }

// MarshalWithValidation marshals and validates a CommandEnvelope.
func (v *SchemaValidator) MarshalWithValidation(cmd CommandEnvelope) ([]byte, error) {
	if err := v.ValidateCommandEnvelope(cmd); err != nil {
		return nil, fmt.Errorf("validation failed: %w", err)
	}
	return json.Marshal(cmd)
}

// UnmarshalWithValidation unmarshals and validates a CommandEnvelope.
func (v *SchemaValidator) UnmarshalWithValidation(data []byte) (CommandEnvelope, error) {
	var cmd CommandEnvelope
	if err := json.Unmarshal(data, &cmd); err != nil {
		return cmd, fmt.Errorf("unmarshal: %w", err)
	}
	if err := v.ValidateCommandEnvelope(cmd); err != nil {
		return cmd, fmt.Errorf("validation failed: %w", err)
	}
	return cmd, nil
}

// defaultValidator is the package-level validator.
var defaultValidator = NewSchemaValidator()

// DefaultValidator returns the package-level validator.
func DefaultValidator() *SchemaValidator { return defaultValidator }
