#!/usr/bin/env python3
import unittest

from packages.blocks.tests.block_fixture_validation import (
    BOUNDARY_BLOCK_FIXTURES,
    CORE_EXAMPLES,
    ERROR_RESULTS,
    INVALID_BLOCK_FIXTURES,
    INVALID_RESULT_FIXTURES,
    SUCCESS_RESULTS,
    load_boundary_fixture,
    load_example,
    load_invalid_fixture,
    validate_block_result,
    validate_block_spec,
)


class BlockExampleTest(unittest.TestCase):
    def test_core_block_examples(self) -> None:
        for kind, name in CORE_EXAMPLES.items():
            with self.subTest(kind=kind, name=name):
                validate_block_spec(load_example(name))

    def test_boundary_block_examples(self) -> None:
        for kind, name in BOUNDARY_BLOCK_FIXTURES.items():
            with self.subTest(kind=kind, name=name):
                validate_block_spec(load_boundary_fixture(name))


class BlockResultShapeTest(unittest.TestCase):
    def test_success_result_examples(self) -> None:
        for kind, name in SUCCESS_RESULTS.items():
            with self.subTest(kind=kind, name=name):
                validate_block_result(load_example(name))

    def test_error_result_examples(self) -> None:
        for kind, name in ERROR_RESULTS.items():
            with self.subTest(kind=kind, name=name):
                validate_block_result(load_example(name))


class BlockInvalidFixtureTest(unittest.TestCase):
    def test_invalid_block_examples_are_rejected(self) -> None:
        for kind, fixture in INVALID_BLOCK_FIXTURES.items():
            name, error_fragment = fixture
            with self.subTest(kind=kind, name=name):
                with self.assertRaisesRegex(ValueError, error_fragment):
                    validate_block_spec(load_invalid_fixture(name))

    def test_invalid_result_examples_are_rejected(self) -> None:
        for name, error_fragment in INVALID_RESULT_FIXTURES.items():
            with self.subTest(name=name):
                with self.assertRaisesRegex(ValueError, error_fragment):
                    validate_block_result(load_invalid_fixture(name))


if __name__ == "__main__":
    unittest.main()
