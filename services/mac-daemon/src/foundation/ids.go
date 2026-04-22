package foundation

import (
	"fmt"
	"regexp"
	"strings"
	"time"
)

var runIDPattern = regexp.MustCompile(`^\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}_[a-z0-9-]+_[a-z0-9-]+$`)

func NewRequestID(now time.Time, seq int64) string {
	return fmt.Sprintf("req_%s_%04d", now.Format("20060102_150405"), seq)
}

func NewRunID(now time.Time, command, purpose string) string {
	clean := func(input string) string {
		input = strings.ToLower(input)
		input = regexp.MustCompile(`[^a-z0-9]+`).ReplaceAllString(input, "-")
		return strings.Trim(input, "-")
	}
	return fmt.Sprintf("%s_%s_%s", now.Format("2006-01-02T15-04-05"), clean(command), clean(purpose))
}

func IsValidRunID(runID string) bool {
	return runIDPattern.MatchString(runID)
}
