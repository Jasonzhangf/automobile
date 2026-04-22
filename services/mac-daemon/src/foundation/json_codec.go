package foundation

import "encoding/json"

func Decode[T any](data []byte) (T, error) {
	var value T
	err := json.Unmarshal(data, &value)
	return value, err
}

func EncodePretty(value any) ([]byte, error) {
	return json.MarshalIndent(value, "", "  ")
}
