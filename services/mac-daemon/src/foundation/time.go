package foundation

import "time"

func NowRFC3339() string {
	return time.Now().Format(time.RFC3339)
}

func ParseRFC3339(value string) (time.Time, error) {
	return time.Parse(time.RFC3339, value)
}

func DatePartition(value string) string {
	parsed, err := ParseRFC3339(value)
	if err != nil {
		return time.Now().Format("2006-01-02")
	}
	return parsed.Format("2006-01-02")
}
