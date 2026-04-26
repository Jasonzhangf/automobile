package foundation

import (
	"strconv"
	"strings"
)

func CompareVersion(left, right string) int {
	leftParts := parseVersion(left)
	rightParts := parseVersion(right)
	size := len(leftParts)
	if len(rightParts) > size {
		size = len(rightParts)
	}
	for index := 0; index < size; index++ {
		leftValue := partAt(leftParts, index)
		rightValue := partAt(rightParts, index)
		if leftValue < rightValue {
			return -1
		}
		if leftValue > rightValue {
			return 1
		}
	}
	return 0
}

func parseVersion(raw string) []int {
	parts := strings.Split(raw, ".")
	result := make([]int, 0, len(parts))
	for _, part := range parts {
		value, err := strconv.Atoi(part)
		if err != nil {
			result = append(result, 0)
			continue
		}
		result = append(result, value)
	}
	return result
}

func partAt(parts []int, index int) int {
	if index >= len(parts) {
		return 0
	}
	return parts[index]
}
