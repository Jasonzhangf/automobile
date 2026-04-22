package foundation

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"

	"flowy/services/mac-daemon/src/proto"
)

const maxArtifactUploadBytes = 20 << 20

func DecodeArtifactUpload(request *http.Request) (proto.ArtifactUploadMeta, []byte, error) {
	if err := request.ParseMultipartForm(maxArtifactUploadBytes); err != nil {
		return proto.ArtifactUploadMeta{}, nil, err
	}
	metaJSON := request.FormValue("meta")
	if metaJSON == "" {
		return proto.ArtifactUploadMeta{}, nil, errors.New("missing meta form field")
	}
	var meta proto.ArtifactUploadMeta
	if err := json.Unmarshal([]byte(metaJSON), &meta); err != nil {
		return proto.ArtifactUploadMeta{}, nil, err
	}
	file, _, err := request.FormFile("file")
	if err != nil {
		return proto.ArtifactUploadMeta{}, nil, err
	}
	defer file.Close()
	content, err := io.ReadAll(io.LimitReader(file, maxArtifactUploadBytes))
	if err != nil {
		return proto.ArtifactUploadMeta{}, nil, err
	}
	return meta, content, nil
}
