#!/usr/bin/env python3
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
EXAMPLES = ROOT / "packages" / "blocks" / "examples"

CORE_KINDS = {
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


def load_json(name: str) -> dict:
    return json.loads((EXAMPLES / name).read_text())


class BlockExampleTest(unittest.TestCase):
    def assert_base_shape(self, payload: dict, expected_kind: str) -> None:
        self.assertEqual(payload["schemaVersion"], "flowy-block-spec-v1")
        self.assertIsInstance(payload["blockId"], str)
        self.assertEqual(payload["kind"], expected_kind)
        self.assertIsInstance(payload["input"], dict)
        self.assertIsInstance(payload["timeoutMs"], int)
        self.assertGreater(payload["timeoutMs"], 0)

    def test_observe_page_example(self) -> None:
        payload = load_json(CORE_KINDS["observe-page"])
        self.assert_base_shape(payload, "observe-page")
        observer = payload["input"]["observerSpec"]
        self.assertIn("requireAccessibility", observer)
        self.assertIn("requireScreenshot", observer)

    def test_filter_targets_example(self) -> None:
        payload = load_json(CORE_KINDS["filter-targets"])
        self.assert_base_shape(payload, "filter-targets")
        self.assertEqual(payload["input"]["pageStateRef"], "page-state:current")
        self.assertTrue(payload["input"]["filterRef"].startswith("filter:"))

    def test_evaluate_anchor_example(self) -> None:
        payload = load_json(CORE_KINDS["evaluate-anchor"])
        self.assert_base_shape(payload, "evaluate-anchor")
        self.assertEqual(payload["input"]["pageStateRef"], "page-state:current")
        self.assertTrue(payload["input"]["anchorRef"].startswith("anchor:"))
        self.assertIn(payload["input"]["phase"], {"pre", "post"})

    def test_execute_operation_example(self) -> None:
        payload = load_json(CORE_KINDS["execute-operation"])
        self.assert_base_shape(payload, "execute-operation")
        self.assertTrue(payload["input"]["operationRef"].startswith("operation:"))
        self.assertTrue(payload["input"]["targetRef"].startswith("target:"))

    def test_emit_event_example(self) -> None:
        payload = load_json(CORE_KINDS["emit-event"])
        self.assert_base_shape(payload, "emit-event")
        self.assertIsInstance(payload["input"]["eventType"], str)
        self.assertTrue(payload["input"]["requestId"].startswith("req_"))
        self.assertTrue(payload["input"]["runId"].startswith("run_"))
        self.assertIsInstance(payload["input"]["data"], dict)


class BlockCoverageTest(unittest.TestCase):
    def test_every_core_block_has_example(self) -> None:
        existing = {path.name for path in EXAMPLES.glob("*.json")}
        missing = [name for name in CORE_KINDS.values() if name not in existing]
        self.assertEqual(missing, [], f"missing examples: {missing}")

    def test_all_examples_use_known_core_kinds(self) -> None:
        expected_kinds = set(CORE_KINDS.keys())
        observed_kinds = set()
        for path in EXAMPLES.glob("*.json"):
            payload = json.loads(path.read_text())
            if "kind" in payload:
                observed_kinds.add(payload["kind"])
        self.assertEqual(observed_kinds, expected_kinds)


class BlockResultShapeTest(unittest.TestCase):
    def assert_result_shape(self, payload: dict, status: str) -> None:
        self.assertEqual(payload["schemaVersion"], "flowy-block-result-v1")
        self.assertEqual(payload["status"], status)
        self.assertIsInstance(payload["startedAt"], str)
        self.assertIsInstance(payload["finishedAt"], str)
        self.assertIsInstance(payload["durationMs"], int)
        self.assertIsInstance(payload["artifacts"], list)
        if status == "ok":
            self.assertIsInstance(payload["output"], dict)
            self.assertIsNone(payload["error"])
        else:
            self.assertIsNone(payload["output"])
            self.assertIsInstance(payload["error"], dict)
            self.assertIsInstance(payload["error"]["code"], str)
            self.assertIsInstance(payload["error"]["message"], str)
            self.assertIsInstance(payload["error"]["retryable"], bool)

    def test_success_result_examples(self) -> None:
        for _, name in SUCCESS_RESULTS.items():
            with self.subTest(name=name):
                self.assert_result_shape(load_json(name), "ok")

    def test_error_result_examples(self) -> None:
        for _, name in ERROR_RESULTS.items():
            with self.subTest(name=name):
                self.assert_result_shape(load_json(name), "error")


class BlockResultCoverageTest(unittest.TestCase):
    def test_every_core_block_has_success_result(self) -> None:
        existing = {path.name for path in EXAMPLES.glob("*.json")}
        missing = [name for name in SUCCESS_RESULTS.values() if name not in existing]
        self.assertEqual(missing, [], f"missing success results: {missing}")

    def test_every_core_block_has_error_result(self) -> None:
        existing = {path.name for path in EXAMPLES.glob("*.json")}
        missing = [name for name in ERROR_RESULTS.values() if name not in existing]
        self.assertEqual(missing, [], f"missing error results: {missing}")


if __name__ == "__main__":
    unittest.main()
