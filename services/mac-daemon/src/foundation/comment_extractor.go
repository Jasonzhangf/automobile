 package foundation

import (
	"regexp"
	"strings"
)

 // CommentEntry represents a single extracted comment.
 type CommentEntry struct {
 	Author  string `json:"author"`
 	Content string `json:"content"`
 	Time    string `json:"time,omitempty"`
 }

var (
	// Matches "N天前", "N小时前", "N分钟前", "刚刚", "昨天", etc.
	timePattern = regexp.MustCompile(`^(\d+[天时分秒]|刚刚|昨天|前天|\d+-\d+)(前|ago)?`)
)

 // ExtractComments parses a flat list of UiNodes from a comment panel dump
 // and returns structured CommentEntry slices.
 //
 // XHS comment structure per visible comment:
 //   - TextView: author name (clickable, short text)
 //   - TextView: comment content (may include time/location prefix like "3天前 北京")
 //   - LinearLayout > ImageView: like icon (optional)
 //
 // Since the dump is flat, we use heuristic grouping:
 //   - A "short" text (< 30 chars) that is NOT a known UI label and follows
 //     a content-like text is treated as the next author.
 //   - A "long" text (>= 10 chars) is treated as content.
 func ExtractComments(nodes []UiNode) []CommentEntry {
 	var comments []CommentEntry
 	var currentAuthor string
 	var contentBuf strings.Builder

 	flush := func() {
 		content := strings.TrimSpace(contentBuf.String())
 		if content != "" {
 			comments = append(comments, CommentEntry{
 				Author:  currentAuthor,
 				Content: content,
 			})
 		}
 		contentBuf.Reset()
 		currentAuthor = ""
 	}

 	uiLabels := map[string]bool{
 		"赞": true, "回复": true, "分享": true, "举报": true,
 		"展开": true, "收起": true, "查看更多回复": true,
 		"暂时没有更多了": true, "没有更多评论": true,
 	}

 	for _, n := range nodes {
 		text := strings.TrimSpace(n.Text)
 		if text == "" {
 			continue
 		}
 		// Skip known UI labels
 		if uiLabels[text] {
 			continue
 		}
 		// Skip very short numeric-only (like count "31")
 		if len(text) <= 5 && isNumericOnly(text) {
 			continue
 		}
 		// Detect time-only strings
 		if timePattern.MatchString(text) {
 			continue
 		}

 		// Heuristic: short text without content → likely author
 		// Long text with actual content → likely comment body
		runes := []rune(text)
		isShort := len(runes) < 20 && !strings.Contains(text, "，") && !strings.Contains(text, "。")
		if isShort {
			// If we already have both author and content, this is a new author → flush first
			if currentAuthor != "" && contentBuf.Len() > 0 {
				flush()
				currentAuthor = text
				continue
			}
			// If we have author but no content yet, this could be short content or a new author
			if currentAuthor != "" && contentBuf.Len() == 0 {
				// Treat as content (could be a short comment like "好文！")
				contentBuf.WriteString(text)
				continue
			}
			// No author yet → set as author
			if currentAuthor == "" {
				flush()
				currentAuthor = text
				continue
			}
		}

 		// Otherwise treat as content
 		if contentBuf.Len() > 0 {
 			contentBuf.WriteString("\n")
 		}
 		contentBuf.WriteString(text)
 	}
 	flush()
 	return comments
 }

 func isNumericOnly(s string) bool {
 	for _, c := range s {
 		if c < '0' || c > '9' {
 			return false
 		}
 	}
 	return len(s) > 0
 }
