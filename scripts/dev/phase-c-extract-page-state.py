#!/usr/bin/env python3
import argparse
import hashlib
import json
from pathlib import Path
from typing import Dict, List, Optional

SCHEMA_VERSION = "exp01-page-state-v1"


def read_json(path: Path) -> dict:
    return json.loads(path.read_text())


def label_for(node: dict) -> str:
    parts = [node.get("text"), node.get("contentDescription"), node.get("hintText"), node.get("paneTitle")]
    return " / ".join(part.strip() for part in parts if isinstance(part, str) and part.strip())


def infer_role(node: dict) -> str:
    class_name = (node.get("className") or "").split(".")[-1]
    flags = node.get("flags") or {}
    if class_name in {"ImageButton", "Button"}:
      return "button"
    if class_name in {"EditText"}:
      return "input"
    if class_name in {"Switch", "SwitchCompat"}:
      return "switch"
    if class_name in {"CheckBox", "RadioButton"}:
      return "check"
    if class_name in {"RecyclerView", "ListView", "ScrollView"} or flags.get("scrollable"):
      return "scroll_container"
    if flags.get("clickable") or flags.get("longClickable") or flags.get("checkable"):
      return "action"
    return "text"


def slug(value: str) -> str:
    clean = "".join(ch.lower() if ch.isalnum() else "-" for ch in value.strip())
    parts = [part for part in clean.split("-") if part]
    return "-".join(parts[:8])[:48] or "unnamed"


def short_view_id(value: Optional[str]) -> Optional[str]:
    if not value:
      return None
    return value.split("/")[-1]


def build_index(nodes: List[dict]) -> Dict[str, dict]:
    return {node["nodeId"]: node for node in nodes}


def descendant_labels(node: dict, node_by_id: Dict[str, dict]) -> List[str]:
    labels: List[str] = []
    stack = list(node.get("childIds") or [])
    seen = set()
    while stack:
      node_id = stack.pop(0)
      if node_id in seen or node_id not in node_by_id:
        continue
      seen.add(node_id)
      child = node_by_id[node_id]
      if not (child.get("flags") or {}).get("visibleToUser", False):
        continue
      direct = label_for(child)
      if direct:
        labels.append(direct)
      stack.extend(child.get("childIds") or [])
    deduped: List[str] = []
    seen_text = set()
    for text in labels:
      if text not in seen_text:
        deduped.append(text)
        seen_text.add(text)
    return deduped


def choose_title(anchors: List[dict], page_context: dict) -> Optional[str]:
    page_title = (((page_context or {}).get("app") or {}).get("windowTitle"))
    if page_title:
      return page_title
    for anchor in anchors:
      if anchor["kind"] == "title" and anchor["text"] not in {"返回", "Back", "back"}:
        return anchor["text"]
    return None


def anchor_kind(node: dict, screen_height: Optional[int]) -> str:
    view_id = node.get("viewIdResourceName") or ""
    top = ((node.get("boundsInScreen") or {}).get("top")) or 0
    text = label_for(node)
    flags = node.get("flags") or {}
    class_name = (node.get("className") or "").split(".")[-1]
    if (
      screen_height and top < screen_height * 0.35 and text and len(text) <= 20
      and not flags.get("clickable")
      and class_name not in {"ImageButton", "Button"}
    ):
      return "title"
    if view_id.endswith("/title") or view_id.endswith(":id/title"):
      return "section"
    return "text"


def normalize(accessibility_raw: dict, page_context: Optional[dict]) -> dict:
    nodes = accessibility_raw.get("nodes") or []
    node_by_id = build_index(nodes)
    screen = (page_context or {}).get("screen") or {}
    screen_height = screen.get("heightPx")
    visible_nodes = [node for node in nodes if (node.get("flags") or {}).get("visibleToUser", False)]

    anchors = []
    for node in visible_nodes:
      text = label_for(node)
      if not text:
        continue
      anchors.append({
        "nodeId": node["nodeId"],
        "text": text,
        "kind": anchor_kind(node, screen_height),
        "boundsInScreen": node.get("boundsInScreen"),
      })

    seen_anchor = set()
    deduped_anchors = []
    for anchor in anchors:
      key = (anchor["kind"], anchor["text"])
      if key in seen_anchor:
        continue
      seen_anchor.add(key)
      deduped_anchors.append(anchor)
    anchors = deduped_anchors[:12]

    elements = []
    for node in visible_nodes:
      flags = node.get("flags") or {}
      role = infer_role(node)
      actionable = any([flags.get("clickable"), flags.get("longClickable"), flags.get("editable"), flags.get("scrollable"), flags.get("checkable")])
      if role == "scroll_container" and not flags.get("scrollable"):
        continue
      if not actionable and role == "text":
        continue
      texts = [label_for(node)] if label_for(node) else []
      texts.extend(descendant_labels(node, node_by_id))
      deduped = []
      for item in texts:
        if item and item not in deduped:
          deduped.append(item)
      label = deduped[0] if deduped else None
      value = " / ".join(deduped[1:]) if len(deduped) > 1 else None
      view_id = node.get("viewIdResourceName")
      if short_view_id(view_id):
        element_id = f"view:{short_view_id(view_id)}"
      elif label:
        element_id = f"role:{role}|label:{slug(label)}"
      else:
        element_id = f"role:{role}|node:{node['nodeId']}"
      actions = []
      if flags.get("clickable"):
        actions.append("click")
      if flags.get("longClickable"):
        actions.append("long-click")
      if flags.get("editable"):
        actions.append("input")
      if flags.get("scrollable"):
        actions.append("scroll")
      if flags.get("checkable"):
        actions.append("toggle")
      elements.append({
        "elementId": element_id,
        "nodeId": node["nodeId"],
        "role": role,
        "label": label,
        "value": value,
        "hint": node.get("hintText"),
        "actions": actions,
        "boundsInScreen": node.get("boundsInScreen"),
        "locator": {
          "viewIdResourceName": view_id,
          "className": node.get("className"),
          "parentNodeId": node.get("parentId"),
        },
      })

    title = choose_title(anchors, page_context or {})
    anchor_texts = [anchor["text"] for anchor in anchors[:6]]
    signature_seed = "|".join([item for item in [accessibility_raw.get("packageName"), title, *anchor_texts] if item])
    signature = hashlib.sha1(signature_seed.encode("utf-8")).hexdigest()[:12] if signature_seed else None

    return {
      "schemaVersion": SCHEMA_VERSION,
      "generatedAt": __import__("datetime").datetime.now().astimezone().isoformat(),
      "source": {
        "kind": "accessibility-raw",
        "requestId": (page_context or {}).get("requestId"),
        "runId": (page_context or {}).get("runId"),
        "capturedAt": accessibility_raw.get("capturedAt"),
      },
      "app": {
        "packageName": accessibility_raw.get("packageName") or (((page_context or {}).get("app") or {}).get("packageName")),
        "windowTitle": (((page_context or {}).get("app") or {}).get("windowTitle")),
      },
      "screen": screen or None,
      "page": {
        "title": title,
        "signatureSeed": signature_seed or None,
        "signature": signature,
        "anchorTexts": anchor_texts,
        "stats": {
          "nodeCount": accessibility_raw.get("nodeCount"),
          "anchorCount": len(anchors),
          "elementCount": len(elements),
          "actionableCount": sum(1 for element in elements if element["actions"]),
          "scrollableCount": sum(1 for element in elements if "scroll" in element["actions"]),
        },
      },
      "anchors": anchors,
      "elements": elements,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--accessibility-raw", required=True)
    parser.add_argument("--page-context")
    parser.add_argument("--output")
    args = parser.parse_args()

    page_context = read_json(Path(args.page_context)) if args.page_context else None
    page_state = normalize(read_json(Path(args.accessibility_raw)), page_context)
    rendered = json.dumps(page_state, ensure_ascii=False, indent=2) + "\n"
    if args.output:
      Path(args.output).write_text(rendered)
      print(args.output)
      return
    print(rendered, end="")


if __name__ == "__main__":
    main()
