#!/usr/bin/env bash
set -euo pipefail

version="${1:-}"
expected="${2:-}"

if [[ -z "$version" ]]; then
  echo "Version is required." >&2
  exit 1
fi

semver='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-((0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)(\.(0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*))*))?$'

if [[ ! "$version" =~ $semver ]]; then
  echo "Version is not a clean SemVer value: $version" >&2
  exit 1
fi

if [[ "$version" == v* || "$version" == *+* ]]; then
  echo "Public versions must not use a v prefix or build metadata: $version" >&2
  exit 1
fi

if [[ -n "$expected" && "$version" != "$expected" ]]; then
  echo "Version does not match expected value: actual=$version, expected=$expected" >&2
  exit 1
fi
