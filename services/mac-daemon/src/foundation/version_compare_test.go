package foundation

import "testing"

func TestCompareVersion(t *testing.T) {
	if CompareVersion("0.1.0034", "0.1.0034") != 0 {
		t.Fatalf("expected equal versions")
	}
	if CompareVersion("0.1.0034", "0.1.0035") >= 0 {
		t.Fatalf("expected left < right")
	}
	if CompareVersion("0.2.0001", "0.1.9999") <= 0 {
		t.Fatalf("expected left > right")
	}
}
