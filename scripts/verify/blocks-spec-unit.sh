#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root"
python3 -m unittest \
  packages.blocks.tests.test_block_examples.BlockExampleTest \
  packages.blocks.tests.test_block_examples.BlockResultShapeTest \
  packages.blocks.tests.test_block_examples.BlockInvalidFixtureTest
