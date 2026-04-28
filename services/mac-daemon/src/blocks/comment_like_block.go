package blocks

import (
	"fmt"
	"strings"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// CommentLikeTarget identifies which comment's like button to operate on.
// Priority: Index (0-based among comment like buttons) > AuthorContains > ContentContains.
type CommentLikeTarget struct {
	Index           int    `json:"index,omitempty"`           // 0-based index among all comment like buttons
	AuthorContains  string `json:"authorContains,omitempty"` // match comment author text
	ContentContains string `json:"contentContains,omitempty"` // match comment content text
}

// CommentLikeInfo describes a detected comment like button on screen.
type CommentLikeInfo struct {
	Index      int  `json:"index"`
	Selected   bool `json:"selected"`   // ImageView.selected
	Count      int  `json:"count"`      // TextView number
	CenterX    int  `json:"centerX"`
	CenterY    int  `json:"centerY"`
}

// DetectCommentLikes scans the flat node list for comment like buttons.
// Pattern: LinearLayout(clickable=true, left>1000) with child ImageView + child TextView(digit).
func DetectCommentLikes(nodes []foundation.UiNode) []CommentLikeInfo {
	var results []CommentLikeInfo
	idx := 0
	for _, n := range nodes {
		if n.ClassName != "android.widget.LinearLayout" || !n.Flags.Clickable || n.BoundsInScreen == nil {
			continue
		}
		if n.BoundsInScreen.Left <= 1000 {
			continue
		}
		// Found a candidate LinearLayout. Look for overlapping ImageView and digit-TextView.
		hasImg := false
		imgSelected := false
		count := 0
		for _, m := range nodes {
			if m.BoundsInScreen == nil {
				continue
			}
			if !boundsOverlap(m.BoundsInScreen, n.BoundsInScreen) {
				continue
			}
			if m.ClassName == "android.widget.ImageView" {
				hasImg = true
				imgSelected = m.Flags.Selected
			}
			if m.ClassName == "android.widget.TextView" && isDigitString(m.Text) {
				count = parseCount(m.Text)
			}
		}
		if hasImg {
			cx, cy := n.BoundsInScreen.Center()
			results = append(results, CommentLikeInfo{
				Index:    idx,
				Selected: imgSelected,
				Count:    count,
				CenterX:  cx,
				CenterY:  cy,
			})
			idx++
		}
	}
	return results
}

// CommentLikeBlock toggles a comment's like state and verifies.
func CommentLikeBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	target CommentLikeTarget,
	desired bool,
	backend OperationBackend,
	timeoutMs int,
) (*CommentLikeInfo, error) {
	// 1. Observe
	obs, err := ObservePage(session, app, artifactRoot, timeoutMs)
	if err != nil {
		return nil, fmt.Errorf("comment-like observe: %w", err)
	}

	// 2. Find target comment like button
	likes := DetectCommentLikes(obs.Nodes)
	if len(likes) == 0 {
		return nil, fmt.Errorf("comment-like: no comment like buttons found")
	}

	btn, err := selectCommentLike(obs.Nodes, likes, target)
	if err != nil {
		return nil, err
	}

	// 3. Check if already in desired state
	if btn.Selected == desired {
		return btn, nil
	}

	// 4. Tap
	if err := TapBlock(session, app, artifactRoot,
		TapTarget{X: &btn.CenterX, Y: &btn.CenterY}, backend, timeoutMs); err != nil {
		return nil, fmt.Errorf("comment-like tap: %w", err)
	}
	time.Sleep(foundation.OperationDelay())

	// 5. Verify
	obs2, err := ObservePage(session, app, artifactRoot, timeoutMs)
	if err != nil {
		return nil, fmt.Errorf("comment-like verify observe: %w", err)
	}
	likes2 := DetectCommentLikes(obs2.Nodes)
	btn2, err := selectCommentLike(obs2.Nodes, likes2, target)
	if err != nil {
		return nil, fmt.Errorf("comment-like verify select: %w", err)
	}
	if btn2.Selected != desired {
		return btn2, fmt.Errorf("comment-like verify failed: wanted selected=%v, got %v", desired, btn2.Selected)
	}
	return btn2, nil
}

func selectCommentLike(nodes []foundation.UiNode, likes []CommentLikeInfo, target CommentLikeTarget) (*CommentLikeInfo, error) {
	if target.Index > 0 && target.Index < len(likes) {
		return &likes[target.Index], nil
	}
	if target.AuthorContains != "" || target.ContentContains != "" {
		// Find which comment index corresponds to the author/content match.
		// Walk the node list: each comment has author (clickable TextView above the like area),
		// then content (longer TextView), then the like button area.
		// We match by proximity: find the comment text, then pick the closest like button below it.
		matchTop := -1
		for _, n := range nodes {
			if n.BoundsInScreen == nil {
				continue
			}
			if target.AuthorContains != "" && strings.Contains(n.Text, target.AuthorContains) && n.Flags.Clickable {
				matchTop = n.BoundsInScreen.Top
				break
			}
			if target.ContentContains != "" && strings.Contains(n.Text, target.ContentContains) {
				matchTop = n.BoundsInScreen.Top
				break
			}
		}
		if matchTop >= 0 {
			// Pick the first like button whose top is >= matchTop
			for i := range likes {
				if likes[i].CenterY >= matchTop {
					return &likes[i], nil
				}
			}
		}
	}
	// Default: first like button
	if len(likes) > 0 {
		return &likes[0], nil
	}
	return nil, fmt.Errorf("comment-like: target not found")
}

func boundsOverlap(a, b *foundation.Bounds) bool {
	return a.Left <= b.Right && a.Right >= b.Left && a.Top <= b.Bottom && a.Bottom >= b.Top
}

func isDigitString(s string) bool {
	if s == "" {
		return false
	}
	for _, c := range s {
		if c < '0' || c > '9' {
			return false
		}
	}
	return true
}

func parseCount(s string) int {
	n := 0
	for _, c := range s {
		n = n*10 + int(c-'0')
	}
	return n
}
