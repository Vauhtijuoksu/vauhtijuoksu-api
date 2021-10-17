#!/usr/bin/env bash
# Prints a version string for current commit

set -euo pipefail

git describe --dirty
