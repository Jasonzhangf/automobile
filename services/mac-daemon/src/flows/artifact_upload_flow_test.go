package flows

import (
	"bytes"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func TestArtifactUploadPersistsFile(t *testing.T) {
	artifactRoot := t.TempDir()
	app := state.New(artifactRoot)
	mux := http.NewServeMux()
	mux.HandleFunc("POST /exp01/artifacts", ArtifactUploadHandler(app))
	server := httptest.NewServer(mux)
	defer server.Close()

	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	meta := proto.ArtifactUploadMeta{
		ProtocolVersion: "exp01",
		RequestID:       "req_20260422_100000_0001",
		RunID:           "2026-04-22T10-00-00_capture-screenshot_home-screen",
		DeviceID:        "android-local-01",
		Command:         "capture-screenshot",
		Kind:            "screenshot",
		FileName:        "screenshot.png",
		ContentType:     "image/png",
	}
	metaJSON, _ := json.Marshal(meta)
	if err := writer.WriteField("meta", string(metaJSON)); err != nil {
		t.Fatalf("write meta failed: %v", err)
	}
	filePart, err := writer.CreateFormFile("file", meta.FileName)
	if err != nil {
		t.Fatalf("create file failed: %v", err)
	}
	content := []byte("png-bytes")
	if _, err := filePart.Write(content); err != nil {
		t.Fatalf("write content failed: %v", err)
	}
	if err := writer.Close(); err != nil {
		t.Fatalf("close writer failed: %v", err)
	}

	request, err := http.NewRequest(http.MethodPost, server.URL+"/exp01/artifacts", &body)
	if err != nil {
		t.Fatalf("new request failed: %v", err)
	}
	request.Header.Set("Content-Type", writer.FormDataContentType())
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatalf("post failed: %v", err)
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		t.Fatalf("expected 200, got %d", response.StatusCode)
	}

	var result proto.ArtifactUploadResponse
	if err := json.NewDecoder(response.Body).Decode(&result); err != nil {
		t.Fatalf("decode failed: %v", err)
	}
	if result.Stored.Kind != "screenshot" || result.Stored.FileName != "screenshot.png" {
		t.Fatalf("unexpected stored descriptor: %#v", result.Stored)
	}

	storedPath := filepath.Join(
		artifactRoot,
		"2026-04-22",
		meta.RunID,
		meta.FileName,
	)
	storedContent, err := os.ReadFile(storedPath)
	if err != nil {
		t.Fatalf("expected stored file: %v", err)
	}
	if string(storedContent) != string(content) {
		t.Fatalf("expected %q, got %q", string(content), string(storedContent))
	}
}
