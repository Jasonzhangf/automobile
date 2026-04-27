package foundation

import (
	"crypto/sha256"
	"fmt"
	"strings"
)

// ItemIDFromTitle produces a deterministic hex hash from a post title.
// Used as the primary dedup key for platforms where resourceId is obfuscated.
func ItemIDFromTitle(title string) string {
	trimmed := strings.TrimSpace(title)
	if trimmed == "" {
		return ""
	}
	h := sha256.Sum256([]byte(trimmed))
	return fmt.Sprintf("%x", h[:8]) // 16 hex chars, sufficient for dedup
}

// ItemIDFromComposite produces a deterministic hex hash from title + author.
// Fallback when title alone is too short or too common.
func ItemIDFromComposite(title, author string) string {
	combined := strings.TrimSpace(title) + "|" + strings.TrimSpace(author)
	if combined == "|" {
		return ""
	}
	h := sha256.Sum256([]byte(combined))
	return fmt.Sprintf("%x", h[:8])
}

// ItemIDWithFallback tries title-first, then composite, then returns empty.
func ItemIDWithFallback(title, author string) string {
	if id := ItemIDFromTitle(title); id != "" {
		return id
	}
	return ItemIDFromComposite(title, author)
}

// NormalizeForHash trims whitespace, collapses internal whitespace for hashing.
func NormalizeForHash(s string) string {
	s = strings.TrimSpace(s)
	s = strings.Join(strings.Fields(s), " ")
	return s
}
