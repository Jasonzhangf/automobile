package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/flows"
	"flowy/services/mac-daemon/src/state"
)

func main() {
	artifactRoot := envOrDefault("FLOWY_ARTIFACT_ROOT", filepath.Clean("../../artifacts"))
	bindAddr := envOrDefault("FLOWY_MAC_DAEMON_ADDR", "0.0.0.0:8787")
	versionFilePath := envOrDefault("FLOWY_ANDROID_LAB_VERSION_FILE", filepath.Clean("../../explore/android-daemon-lab/config/runtime-version.json"))
	apkPath := envOrDefault("FLOWY_ANDROID_LAB_APK_PATH", filepath.Clean("../../explore/android-daemon-lab/app/build/outputs/apk/debug/app-debug.apk"))
	app := state.New(artifactRoot)

	mux := http.NewServeMux()
	mux.Handle("GET /health", jsonHandler(func(_ *http.Request) any { return map[string]string{"status": "ok"} }))
	mux.Handle("GET /exp01/clients", jsonHandler(func(_ *http.Request) any { return app.Clients() }))
	mux.Handle("GET /flowy/upgrade/check", flows.UpgradeCheckHandler(versionFilePath, ""))
	mux.Handle("GET /flowy/upgrade/apk", flows.UpgradeApkManifestHandler(versionFilePath, apkPath, ""))
	mux.Handle("GET /flowy/upgrade/apk/download", http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		http.ServeFile(writer, request, apkPath)
	}))
	mux.Handle("/exp01/ws", blocks.WSAcceptHandler(func(conn *websocket.Conn) {
		go flows.RunClientSession(app, conn)
	}))
	mux.Handle("POST /exp01/artifacts", flows.ArtifactUploadHandler(app))
	mux.Handle("POST /exp01/command", flows.CommandRoundtripHandler(app))
	mux.Handle("POST /exp01/collection/run", flows.CollectionRunHandler(app))
	mux.Handle("POST /exp01/collection/stop", jsonHandler(func(r *http.Request) any {
		stopped := app.StopActiveRun()
		return map[string]any{"stopped": stopped}
	}))

	log.Printf("flowy mac daemon listening on %s", bindAddr)
	log.Fatal(http.ListenAndServe(bindAddr, mux))
}

type jsonPayloadFunc func(*http.Request) any

func jsonHandler(fn jsonPayloadFunc) http.Handler {
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(writer).Encode(fn(request))
	})
}

func envOrDefault(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
