#!/bin/bash
# audit-mainline-binding.sh
# Checks that mainline-call-map.yml edges have matching function-map.yml features

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$HERE/../.."
CALL_MAP="$ROOT/docs/architecture/mainline-call-map.yml"
FUNC_MAP="$ROOT/docs/architecture/function-map.yml"

errors=0

# Extract all owner_feature_ids from call map
echo "=== Checking mainline-call-map.yml owner_feature_ids against function-map.yml ==="
call_map_features=$(grep -E "owner_feature_id:" "$CALL_MAP" | sed 's/.*owner_feature_id: //' | sort -u)
func_map_features=$(grep -E "^  - feature_id:" "$FUNC_MAP" | sed 's/.*feature_id: //' | sort -u)

missing_in_func=()
while IFS= read -r f; do
    if ! echo "$func_map_features" | grep -qF "$f"; then
        missing_in_func+=("$f")
    fi
done <<< "$call_map_features"

if [ ${#missing_in_func[@]} -gt 0 ]; then
    echo "ERROR: owner_feature_ids in mainline-call-map.yml not found in function-map.yml:"
    for m in "${missing_in_func[@]}"; do echo "  - $m"; done
    ((errors++))
else
    echo "OK: All call map owner_feature_ids found in function map"
fi

# Check binding_pending edges
pending_edges=$(grep -c "binding_pending: true" "$CALL_MAP" 2>/dev/null || echo 0)
echo ""
echo "=== Binding Status ==="
anchored=$(grep -c "status: anchored" "$CALL_MAP" 2>/dev/null || echo 0)
echo "Anchored edges: $anchored"
echo "Binding-pending edges: $pending_edges"
echo "Total edges: $((anchored + pending_edges))"

# Check 500-line cap
echo ""
echo "=== 500-line cap violations (services/mac-daemon) ==="
over=0
for f in "$ROOT/services/mac-daemon/src/blocks"/*.go "$ROOT/services/mac-daemon/src/flows"/*.go "$ROOT/services/mac-daemon/src/foundation"/*.go; do
    [ -f "$f" ] || continue
    lines=$(wc -l < "$f")
    if [ "$lines" -gt 500 ]; then
        echo "VIOLATION: $f ($lines lines)"
        ((over++))
    fi
done
if [ "$over" -eq 0 ]; then
    echo "OK: No 500-line violations"
fi

echo ""
echo "=== Summary ==="
if [ "$errors" -gt 0 ]; then
    echo "FAILED: $errors error(s) found"
    exit 1
else
    echo "PASSED"
fi
