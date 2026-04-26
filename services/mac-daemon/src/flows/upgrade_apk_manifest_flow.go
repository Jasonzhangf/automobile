package flows

import (
	"encoding/json"
	"net/http"
	"os"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

type RuntimeVersionConfig struct {
	VersionName string `json:"versionName"`
}

func UpgradeCheckHandler(versionFilePath, manifestURL string) http.HandlerFunc {
	return func(writer http.ResponseWriter, request *http.Request) {
		config, err := readRuntimeVersion(versionFilePath)
		if err != nil {
			http.Error(writer, err.Error(), http.StatusInternalServerError)
			return
		}
		currentVersion := request.URL.Query().Get("currentVersion")
		available := currentVersion == "" || foundation.CompareVersion(currentVersion, config.VersionName) < 0
		targetManifestURL := manifestURL
		if targetManifestURL == "" {
			targetManifestURL = requestBaseURL(request) + "/flowy/upgrade/apk"
		}
		writer.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(writer).Encode(proto.UpgradeCheckResponse{
			ProtocolVersion: "flowy-upgrade-v1",
			CurrentVersion:  currentVersion,
			LatestVersion:   config.VersionName,
			Available:       available,
			ManifestURL:     targetManifestURL,
		})
	}
}

func UpgradeApkManifestHandler(versionFilePath, apkPath, downloadURL string) http.HandlerFunc {
	return func(writer http.ResponseWriter, request *http.Request) {
		config, err := readRuntimeVersion(versionFilePath)
		if err != nil {
			http.Error(writer, err.Error(), http.StatusInternalServerError)
			return
		}
		info, err := os.Stat(apkPath)
		if err != nil {
			http.Error(writer, err.Error(), http.StatusInternalServerError)
			return
		}
		targetDownloadURL := downloadURL
		if targetDownloadURL == "" {
			targetDownloadURL = requestBaseURL(request) + "/flowy/upgrade/apk/download"
		}
		writer.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(writer).Encode(proto.UpgradeApkManifest{
			ProtocolVersion: "flowy-upgrade-v1",
			VersionName:     config.VersionName,
			FileName:        "flowy-daemon-lab-" + config.VersionName + ".apk",
			DownloadURL:     targetDownloadURL,
			SizeBytes:       info.Size(),
		})
	}
}

func readRuntimeVersion(versionFilePath string) (RuntimeVersionConfig, error) {
	var config RuntimeVersionConfig
	raw, err := os.ReadFile(versionFilePath)
	if err != nil {
		return config, err
	}
	return config, json.Unmarshal(raw, &config)
}

func requestBaseURL(request *http.Request) string {
	scheme := "http"
	if request.TLS != nil {
		scheme = "https"
	}
	return scheme + "://" + request.Host
}
