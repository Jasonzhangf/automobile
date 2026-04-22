#!/usr/bin/env python3
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
EXAMPLES = ROOT / "packages" / "blocks" / "examples"
BOUNDARY_FIXTURES = ROOT / "packages" / "blocks" / "fixtures" / "boundary"
INVALID_FIXTURES = ROOT / "packages" / "blocks" / "fixtures" / "invalid"

CORE_EXAMPLES = {
    "observe-page": "observe-page.example.json",
    "filter-targets": "filter-targets.search-entry.example.json",
    "evaluate-anchor": "evaluate-anchor.pre-open-search.example.json",
    "execute-operation": "execute-operation.tap-search.example.json",
    "emit-event": "emit-event.workflow-succeeded.example.json",
}

SUCCESS_RESULTS = {
    "observe-page": "observe-page.success.result.json",
    "filter-targets": "filter-targets.success.result.json",
    "evaluate-anchor": "evaluate-anchor.success.result.json",
    "execute-operation": "execute-operation.success.result.json",
    "emit-event": "emit-event.success.result.json",
}

ERROR_RESULTS = {
    "observe-page": "observe-page.error.result.json",
    "filter-targets": "filter-targets.error.result.json",
    "evaluate-anchor": "evaluate-anchor.error.result.json",
    "execute-operation": "execute-operation.error.result.json",
    "emit-event": "emit-event.error.result.json",
}

BOUNDARY_BLOCK_FIXTURES = {
    "observe-page": "observe-page.screenshot-required.boundary.example.json",
    "filter-targets": "filter-targets.min-timeout.boundary.example.json",
    "evaluate-anchor": "evaluate-anchor.post.boundary.example.json",
    "execute-operation": "execute-operation.min-timeout.boundary.example.json",
    "emit-event": "emit-event.empty-data.boundary.example.json",
}

INVALID_BLOCK_FIXTURES = {
    "observe-page": (
        "observe-page.invalid-timeout.example.json",
        "timeoutMs must be a positive integer",
    ),
    "filter-targets": (
        "filter-targets.invalid-filter-ref.example.json",
        "filterRef must start with filter:",
    ),
    "evaluate-anchor": (
        "evaluate-anchor.invalid-phase.example.json",
        "input.phase must be pre or post",
    ),
    "execute-operation": (
        "execute-operation.invalid-target-ref.example.json",
        "targetRef must start with target:",
    ),
    "emit-event": (
        "emit-event.invalid-request-id.example.json",
        "input.requestId must start with req_",
    ),
}

INVALID_RESULT_FIXTURES = {
    "block-result.invalid-status.json": "status must be ok or error",
    "block-result.invalid-ok-with-error.json": "error must be null when status=ok",
    "block-result.invalid-error-with-output.json": "output must be null when status=error",
    "block-result.invalid-error-missing-retryable.json": "retryable must be bool",
    "block-result.invalid-negative-duration.json": "durationMs must be a non-negative integer",
}

ALL_CORE_KINDS = set(CORE_EXAMPLES)


def load_json(path: Path) -> dict:
    return json.loads(path.read_text())


def load_example(name: str) -> dict:
    return load_json(EXAMPLES / name)


def load_boundary_fixture(name: str) -> dict:
    return load_json(BOUNDARY_FIXTURES / name)


def load_invalid_fixture(name: str) -> dict:
    return load_json(INVALID_FIXTURES / name)


def require_string(payload: dict, field: str) -> str:
    value = payload.get(field)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field} must be a non-empty string")
    return value


def require_bool(payload: dict, field: str) -> bool:
    value = payload.get(field)
    if not isinstance(value, bool):
        raise ValueError(f"{field} must be bool")
    return value


def require_dict(payload: dict, field: str) -> dict:
    value = payload.get(field)
    if not isinstance(value, dict):
        raise ValueError(f"{field} must be dict")
    return value


def require_positive_int(payload: dict, field: str) -> int:
    value = payload.get(field)
    if not isinstance(value, int) or value <= 0:
        raise ValueError(f"{field} must be a positive integer")
    return value


def require_non_negative_int(payload: dict, field: str) -> int:
    value = payload.get(field)
    if not isinstance(value, int) or value < 0:
        raise ValueError(f"{field} must be a non-negative integer")
    return value


def require_ref(payload: dict, field: str, prefix: str) -> str:
    value = require_string(payload, field)
    if not value.startswith(prefix):
        raise ValueError(f"{field} must start with {prefix}")
    return value


def validate_block_spec(payload: dict) -> None:
    if payload.get("schemaVersion") != "flowy-block-spec-v1":
        raise ValueError("schemaVersion must be flowy-block-spec-v1")
    require_string(payload, "blockId")
    kind = require_string(payload, "kind")
    if kind not in ALL_CORE_KINDS:
        raise ValueError(f"kind must be one of {sorted(ALL_CORE_KINDS)}")
    input_payload = require_dict(payload, "input")
    require_positive_int(payload, "timeoutMs")
    validate_block_input(kind, input_payload)


def validate_block_input(kind: str, input_payload: dict) -> None:
    if kind == "observe-page":
        observer = require_dict(input_payload, "observerSpec")
        require_bool(observer, "requireAccessibility")
        require_bool(observer, "requireScreenshot")
        return

    if kind == "filter-targets":
        require_ref(input_payload, "pageStateRef", "page-state:")
        require_ref(input_payload, "filterRef", "filter:")
        return

    if kind == "evaluate-anchor":
        require_ref(input_payload, "pageStateRef", "page-state:")
        require_ref(input_payload, "anchorRef", "anchor:")
        phase = require_string(input_payload, "phase")
        if phase not in {"pre", "post"}:
            raise ValueError("input.phase must be pre or post")
        return

    if kind == "execute-operation":
        require_ref(input_payload, "operationRef", "operation:")
        require_ref(input_payload, "targetRef", "target:")
        return

    if kind == "emit-event":
        require_string(input_payload, "eventType")
        request_id = require_string(input_payload, "requestId")
        if not request_id.startswith("req_"):
            raise ValueError("input.requestId must start with req_")
        run_id = require_string(input_payload, "runId")
        if not run_id.startswith("run_"):
            raise ValueError("input.runId must start with run_")
        require_dict(input_payload, "data")
        return

    raise ValueError(f"unsupported block kind: {kind}")


def validate_block_result(payload: dict) -> None:
    if payload.get("schemaVersion") != "flowy-block-result-v1":
        raise ValueError("schemaVersion must be flowy-block-result-v1")
    status = payload.get("status")
    if status not in {"ok", "error"}:
        raise ValueError("status must be ok or error")
    require_string(payload, "startedAt")
    require_string(payload, "finishedAt")
    require_non_negative_int(payload, "durationMs")
    artifacts = payload.get("artifacts")
    if not isinstance(artifacts, list):
        raise ValueError("artifacts must be list")

    if status == "ok":
        if not isinstance(payload.get("output"), dict):
            raise ValueError("output must be dict when status=ok")
        if payload.get("error") is not None:
            raise ValueError("error must be null when status=ok")
        return

    if payload.get("output") is not None:
        raise ValueError("output must be null when status=error")
    error = payload.get("error")
    if not isinstance(error, dict):
        raise ValueError("error must be dict when status=error")
    require_string(error, "code")
    require_string(error, "message")
    require_bool(error, "retryable")
