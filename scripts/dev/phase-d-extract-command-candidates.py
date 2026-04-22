#!/usr/bin/env python3
import argparse
import json
from datetime import datetime
from pathlib import Path
from typing import Dict, List

SCHEMA_VERSION = "exp01-command-candidates-v1"


def read_json(path: Path) -> dict:
    return json.loads(path.read_text())


def confidence_for(element: dict) -> str:
    role = element.get("role")
    label = element.get("label")
    element_id = element.get("elementId") or ""
    if role in {"button", "input", "switch", "scroll_container"} and label:
        return "high"
    if element_id.startswith("view:") and label:
        return "high"
    if role == "action" and label:
        return "medium"
    return "low"


def priority_for(kind: str, element: dict) -> int:
    role = element.get("role")
    label = element.get("label")
    base = {
        "tap": 80,
        "long-press": 70,
        "scroll": 75,
        "input-text": 85,
        "toggle": 82,
    }[kind]
    if role == "button":
        base += 20
    elif role == "scroll_container":
        base += 15
    elif role == "input":
        base += 18
    elif role == "switch":
        base += 16
    elif role == "action":
        base += 8
    if label:
        base += 5
    if (element.get("elementId") or "").startswith("view:"):
        base += 4
    top = (((element.get("boundsInScreen") or {}).get("top")))
    if isinstance(top, int):
        if top < 900:
            base += 8
        elif top < 1400:
            base += 5
        elif top < 1900:
            base += 2
    return base


def target_for(element: dict) -> dict:
    return {
        "elementId": element.get("elementId"),
        "role": element.get("role"),
        "label": element.get("label"),
    }


def rationale_for(kind: str, element: dict) -> List[str]:
    reasons = []
    actions = element.get("actions") or []
    if kind == "tap" and "click" in actions:
        reasons.append("element exposes click action")
    if kind == "long-press" and "long-click" in actions:
        reasons.append("element exposes long-click action")
    if kind == "scroll" and "scroll" in actions:
        reasons.append("element exposes scroll action")
    if kind == "input-text" and "input" in actions:
        reasons.append("element exposes input action")
    if kind == "toggle" and "toggle" in actions:
        reasons.append("element exposes toggle action")
    role = element.get("role")
    if role:
        reasons.append(f"role={role}")
    if element.get("label"):
        reasons.append("label present")
    return reasons


def candidate(command_id: str, kind: str, element: dict, args: dict) -> dict:
    return {
        "commandId": command_id,
        "kind": kind,
        "target": target_for(element),
        "args": args,
        "confidence": confidence_for(element),
        "priority": priority_for(kind, element),
        "rationale": rationale_for(kind, element),
    }


def generate(page_state: dict) -> dict:
    commands: Dict[str, dict] = {}
    for element in page_state.get("elements") or []:
        element_id = element.get("elementId")
        if not element_id:
            continue
        actions = element.get("actions") or []
        generated: List[dict] = []
        if "click" in actions:
            generated.append(candidate(f"tap:{element_id}", "tap", element, {}))
        if "long-click" in actions:
            generated.append(candidate(f"long-press:{element_id}", "long-press", element, {}))
        if "scroll" in actions:
            generated.append(candidate(f"scroll-forward:{element_id}", "scroll", element, {"direction": "forward"}))
            generated.append(candidate(f"scroll-backward:{element_id}", "scroll", element, {"direction": "backward"}))
        if "input" in actions:
            generated.append(candidate(f"input-text:{element_id}", "input-text", element, {"text": "<TEXT>"}))
        if "toggle" in actions:
            generated.append(candidate(f"toggle:{element_id}", "toggle", element, {"mode": "toggle"}))

        for item in generated:
            existing = commands.get(item["commandId"])
            if existing is None or item["priority"] > existing["priority"]:
                commands[item["commandId"]] = item

    ordered = sorted(
        commands.values(),
        key=lambda item: (-item["priority"], item["commandId"]),
    )
    return {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": datetime.now().astimezone().isoformat(),
        "source": {
            "kind": "page-state",
            "runId": ((page_state.get("source") or {}).get("runId")),
            "pageSignature": ((page_state.get("page") or {}).get("signature")),
        },
        "page": {
            "packageName": ((page_state.get("app") or {}).get("packageName")),
            "title": ((page_state.get("page") or {}).get("title")),
            "signature": ((page_state.get("page") or {}).get("signature")),
        },
        "stats": {
            "elementCount": len(page_state.get("elements") or []),
            "commandCount": len(ordered),
        },
        "commands": ordered,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--page-state", required=True)
    parser.add_argument("--output")
    args = parser.parse_args()

    rendered = json.dumps(generate(read_json(Path(args.page_state))), ensure_ascii=False, indent=2) + "\n"
    if args.output:
        Path(args.output).write_text(rendered)
        print(args.output)
        return
    print(rendered, end="")


if __name__ == "__main__":
    main()
