package flows

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestUpgradeCheckHandler(t *testing.T) {
	root := t.TempDir()
	versionFile := filepath.Join(root, "runtime-version.json")
	if err := os.WriteFile(versionFile, []byte(`{"versionName":"0.1.0034"}`), 0o644); err != nil {
		t.Fatalf("write version file: %v", err)
	}
	server := httptest.NewServer(UpgradeCheckHandler(versionFile, "http://test/flowy/upgrade/apk"))
	defer server.Close()

	response, err := http.Get(server.URL + "?currentVersion=0.1.0033")
	if err != nil {
		t.Fatalf("get response: %v", err)
	}
	defer response.Body.Close()

	var payload map[string]any
	if err := json.NewDecoder(response.Body).Decode(&payload); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if payload["available"] != true {
		t.Fatalf("expected available=true, got %#v", payload["available"])
	}
}

func TestUpgradeApkManifestHandler(t *testing.T) {
	root := t.TempDir()
	versionFile := filepath.Join(root, "runtime-version.json")
	apkPath := filepath.Join(root, "app-debug.apk")
	if err := os.WriteFile(versionFile, []byte(`{"versionName":"0.1.0034"}`), 0o644); err != nil {
		t.Fatalf("write version file: %v", err)
	}
	if err := os.WriteFile(apkPath, []byte("apk"), 0o644); err != nil {
		t.Fatalf("write apk file: %v", err)
	}
	server := httptest.NewServer(UpgradeApkManifestHandler(versionFile, apkPath, "http://test/flowy/upgrade/apk/download"))
	defer server.Close()

	response, err := http.Get(server.URL)
	if err != nil {
		t.Fatalf("get response: %v", err)
	}
	defer response.Body.Close()

	var payload map[string]any
	if err := json.NewDecoder(response.Body).Decode(&payload); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	if payload["downloadUrl"] != "http://test/flowy/upgrade/apk/download" {
		t.Fatalf("unexpected downloadUrl: %#v", payload["downloadUrl"])
	}
}
