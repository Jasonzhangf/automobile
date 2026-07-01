package flows

import (
	"flowy/services/mac-daemon/src/foundation"
)

// extractField extracts a single detail field from UI nodes based on field spec.
func (fc *FlowContext) extractField(nodes []foundation.UiNode, field DetailField) string {
	if field.Filter != nil {
		matches := foundation.MatchNodes(nodes, *field.Filter)
		if len(matches) > 0 {
			node := matches[0].Node
			switch field.Source {
			case "contentDescription":
				return node.ContentDescription
			default:
				return node.Text
			}
		}
	}
	return ""
}
