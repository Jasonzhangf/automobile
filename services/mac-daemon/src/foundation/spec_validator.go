package foundation

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sync"
)

// SpecValidator validates page-model specs against JSON schema definitions.
type SpecValidator struct {
	mu      sync.RWMutex
	schemas map[string][]byte
	strict  bool
}

// NewSpecValidator constructs a validator.
func NewSpecValidator() *SpecValidator {
	return &SpecValidator{
		schemas: make(map[string][]byte),
		strict:  true,
	}
}

// LoadSchemasFromDir loads JSON schema files from a directory.
func (v *SpecValidator) LoadSchemasFromDir(dir string) error {
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
		data, err := os.ReadFile(filepath.Join(dir, e.Name()))
		if err != nil {
			return fmt.Errorf("read %s: %w", e.Name(), err)
		}
		v.schemas[e.Name()] = data
	}
	return nil
}

// ValidateFilterSpec checks a FilterSpec against the filter-spec schema.
func (v *SpecValidator) ValidateFilterSpec(spec FilterSpec) error {
	if !v.strict {
		return nil
	}
	if spec.MaxTextLength > 0 && spec.MinTextLength > spec.MaxTextLength {
		return fmt.Errorf("minTextLength (%d) > maxTextLength (%d)", spec.MinTextLength, spec.MaxTextLength)
	}
	if spec.TextMatches != "" {
		if _, err := regexp.Compile(spec.TextMatches); err != nil {
			return fmt.Errorf("invalid textMatches regex: %w", err)
		}
	}
	if spec.DescMatches != "" {
		if _, err := regexp.Compile(spec.DescMatches); err != nil {
			return fmt.Errorf("invalid descMatches regex: %w", err)
		}
	}
	if spec.SelectStrategy != "" {
		switch spec.SelectStrategy {
		case "first_best", "top_most", "longest_text", "all_visible":
		default:
			return fmt.Errorf("invalid selectStrategy: %q (want first_best|top_most|longest_text|all_visible)", spec.SelectStrategy)
		}
	}
	return nil
}

// ValidateAnchorSpec checks an AnchorSpec against the anchor-spec schema.
func (v *SpecValidator) ValidateAnchorSpec(spec AnchorSpec) error {
	if !v.strict {
		return nil
	}
	if spec.MinNodeCount > 0 && spec.MaxNodeCount > 0 && spec.MinNodeCount > spec.MaxNodeCount {
		return fmt.Errorf("minNodeCount (%d) > maxNodeCount (%d)", spec.MinNodeCount, spec.MaxNodeCount)
	}
	return nil
}

// ValidateOperationBackend checks a backend string.
func (v *SpecValidator) ValidateOperationBackend(backend string) error {
	if !v.strict {
		return nil
	}
	switch backend {
	case "accessibility", "root", "":
		return nil
	default:
		return fmt.Errorf("invalid operation backend: %q (want accessibility|root)", backend)
	}
}

// defaultSpecValidator is the package-level validator.
var defaultSpecValidator = NewSpecValidator()

// DefaultSpecValidator returns the package-level spec validator.
func DefaultSpecValidator() *SpecValidator { return defaultSpecValidator }

// MustMarshalJSON is a helper to confirm JSON output is parseable.
func (v *SpecValidator) MustMarshalJSON(spec any) ([]byte, error) {
	return json.Marshal(spec)
}
