package foundation

import "testing"

func TestValidateFilterSpec(t *testing.T) {
	v := NewSpecValidator()
	cases := []struct {
		name    string
		spec    FilterSpec
		wantErr bool
	}{
		{"empty", FilterSpec{}, false},
		{"valid select strategy", FilterSpec{SelectStrategy: "top_most"}, false},
		{"invalid select strategy", FilterSpec{SelectStrategy: "random"}, true},
		{"valid regex", FilterSpec{TextMatches: "^hello"}, false},
		{"invalid regex", FilterSpec{TextMatches: "[invalid"}, true},
		{"valid range", FilterSpec{MinTextLength: 1, MaxTextLength: 10}, false},
		{"invalid range", FilterSpec{MinTextLength: 10, MaxTextLength: 5}, true},
		{"valid desc regex", FilterSpec{DescMatches: "test.*"}, false},
		{"invalid desc regex", FilterSpec{DescMatches: "[bad"}, true},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			err := v.ValidateFilterSpec(tc.spec)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for %s", tc.name)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("unexpected error for %s: %v", tc.name, err)
			}
		})
	}
}

func TestValidateAnchorSpec(t *testing.T) {
	v := NewSpecValidator()
	cases := []struct {
		name    string
		spec    AnchorSpec
		wantErr bool
	}{
		{"empty", AnchorSpec{}, false},
		{"valid min only", AnchorSpec{MinNodeCount: 5}, false},
		{"valid range", AnchorSpec{MinNodeCount: 1, MaxNodeCount: 100}, false},
		{"invalid range", AnchorSpec{MinNodeCount: 100, MaxNodeCount: 5}, true},
		{"valid package", AnchorSpec{PackageName: "com.example"}, false},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			err := v.ValidateAnchorSpec(tc.spec)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for %s", tc.name)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("unexpected error for %s: %v", tc.name, err)
			}
		})
	}
}

func TestValidateOperationBackend(t *testing.T) {
	v := NewSpecValidator()
	cases := []struct {
		backend string
		wantErr bool
	}{
		{"accessibility", false},
		{"root", false},
		{"", false},
		{"auto", true},
		{"shell", true},
	}
	for _, tc := range cases {
		t.Run(tc.backend, func(t *testing.T) {
			err := v.ValidateOperationBackend(tc.backend)
			if tc.wantErr && err == nil {
				t.Errorf("expected error for backend %q", tc.backend)
			}
			if !tc.wantErr && err != nil {
				t.Errorf("unexpected error for %q: %v", tc.backend, err)
			}
		})
	}
}
