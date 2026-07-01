package blocks

import (
	"testing"

	"flowy/services/mac-daemon/src/proto"
)

// OBSERVE REVERSE: error response must NOT be masked by Send error or empty artifacts.
func TestObservePageWithTransport_ErrorResponse_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "dump-ui-tree-root",
		Status:          "error",
		Error:           &proto.ErrorObject{Code: "OBSERVE_FAILED", Message: "device locked"},
	}
	tr := newTestTransportErr(nil, resp)
	_, err := observePageWithTransport(tr, "/tmp/artifact_test", 1000)
	if err == nil {
		t.Fatal("expected error for observe error response, got nil")
	}
}

// OBSERVE REVERSE: empty artifacts must NOT silently return empty result.
func TestObservePageWithTransport_NoArtifacts_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "dump-ui-tree-root",
		Status:          "ok",
		// Artifacts intentionally empty
	}
	tr := newTestTransportErr(nil, resp)
	_, err := observePageWithTransport(tr, "/tmp/artifact_test", 1000)
	if err == nil {
		t.Fatal("expected error when response has no artifacts, got nil")
	}
}

// OBSERVE REVERSE: malformed JSON artifact must NOT silently return empty result.
func TestObservePageWithTransport_MalformedArtifact_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "dump-ui-tree-root",
		Status:          "ok",
		Artifacts: []proto.ArtifactDescriptor{
			{Kind: "tree", FileName: "tree.json", ContentType: "application/json"},
		},
	}
	tr := newTestTransportErr(nil, resp)
	// Use a directory that DOES NOT contain the artifact file -> read error path
	_, err := observePageWithTransport(tr, "/tmp/nonexistent_artifacts", 1000)
	if err == nil {
		t.Fatal("expected error when artifact path does not exist, got nil")
	}
}
