#!/usr/bin/env python3
import json
import unittest

from packages.blocks.tests.block_fixture_validation import (
    ALL_CORE_KINDS,
    BOUNDARY_BLOCK_FIXTURES,
    BOUNDARY_FIXTURES,
    CORE_EXAMPLES,
    ERROR_RESULTS,
    EXAMPLES,
    INVALID_BLOCK_FIXTURES,
    INVALID_FIXTURES,
    INVALID_RESULT_FIXTURES,
    SUCCESS_RESULTS,
)


class BlockCoverageTest(unittest.TestCase):
    def test_every_core_block_has_example(self) -> None:
        existing = {path.name for path in EXAMPLES.glob("*.json")}
        missing = [name for name in CORE_EXAMPLES.values() if name not in existing]
        self.assertEqual(missing, [], f"missing examples: {missing}")

    def test_every_core_block_has_boundary_fixture(self) -> None:
        existing = {path.name for path in BOUNDARY_FIXTURES.glob("*.json")}
        missing = [name for name in BOUNDARY_BLOCK_FIXTURES.values() if name not in existing]
        self.assertEqual(missing, [], f"missing boundary fixtures: {missing}")

    def test_every_core_block_has_invalid_fixture(self) -> None:
        existing = {path.name for path in INVALID_FIXTURES.glob("*.json")}
        missing = [name for name, _ in INVALID_BLOCK_FIXTURES.values() if name not in existing]
        self.assertEqual(missing, [], f"missing invalid block fixtures: {missing}")

    def test_all_spec_fixtures_use_known_core_kinds(self) -> None:
        observed_kinds = set()
        for directory in (EXAMPLES, BOUNDARY_FIXTURES, INVALID_FIXTURES):
            for path in directory.glob("*.json"):
                payload = json.loads(path.read_text())
                if "kind" in payload:
                    observed_kinds.add(payload["kind"])
        self.assertEqual(observed_kinds, ALL_CORE_KINDS)


class BlockResultCoverageTest(unittest.TestCase):
    def test_every_core_block_has_success_result(self) -> None:
        existing = {path.name for path in EXAMPLES.glob("*.json")}
        missing = [name for name in SUCCESS_RESULTS.values() if name not in existing]
        self.assertEqual(missing, [], f"missing success results: {missing}")

    def test_every_core_block_has_error_result(self) -> None:
        existing = {path.name for path in EXAMPLES.glob("*.json")}
        missing = [name for name in ERROR_RESULTS.values() if name not in existing]
        self.assertEqual(missing, [], f"missing error results: {missing}")

    def test_invalid_result_fixture_inventory_is_complete(self) -> None:
        existing = {path.name for path in INVALID_FIXTURES.glob("*.json")}
        missing = [name for name in INVALID_RESULT_FIXTURES if name not in existing]
        self.assertEqual(missing, [], f"missing invalid result fixtures: {missing}")


if __name__ == "__main__":
    unittest.main()
