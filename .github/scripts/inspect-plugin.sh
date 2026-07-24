#!/usr/bin/env bash
set -euo pipefail

jar_path="${1:-}"
expected_version="${2:-}"
expected_channel="${3:-}"
expected_number="${4:-}"
expected_attempt="${5:-}"
expected_run_id="${6:-}"
expected_commit="${7:-}"

if [[ -z "$jar_path" || -z "$expected_version" || -z "$expected_channel" ]]; then
  echo "Usage: inspect-plugin.sh <jar> <version> <channel> [number] [attempt] [run-id] [commit]" >&2
  exit 1
fi

python3 - "$jar_path" "$expected_version" "$expected_channel" "$expected_number" "$expected_attempt" "$expected_run_id" "$expected_commit" <<'PY'
import re
import sys
import zipfile
from pathlib import Path

jar_path = Path(sys.argv[1])
expected_version = sys.argv[2]
expected_channel = sys.argv[3]
expected_number = sys.argv[4]
expected_attempt = sys.argv[5]
expected_run_id = sys.argv[6]
expected_commit = sys.argv[7]

if not jar_path.is_file() or jar_path.stat().st_size == 0:
    raise SystemExit(f"Plugin jar is missing or empty: {jar_path}")

required = {
    "paper-plugin.yml",
    "messages/en-US.yml",
    "gui.yml",
    "economy.yml",
    "git.properties",
    "META-INF/headdb-build.properties",
}


def parse_properties(value: str) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in value.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        key, separator, item = line.partition("=")
        if not separator:
            key, separator, item = line.partition(":")
        if separator:
            properties[key.strip()] = item.strip()
    return properties


def yaml_scalar(value: str, key: str) -> str | None:
    pattern = re.compile(rf"^\s*{re.escape(key)}\s*:\s*(.*?)\s*$")
    for raw_line in value.splitlines():
        line = raw_line.split("#", 1)[0]
        match = pattern.match(line)
        if not match:
            continue
        scalar = match.group(1).strip()
        if len(scalar) >= 2 and scalar[0] == scalar[-1] and scalar[0] in {"'", '"'}:
            scalar = scalar[1:-1]
        return scalar.strip()
    return None


with zipfile.ZipFile(jar_path) as jar:
    names = set(jar.namelist())
    missing = sorted(required - names)
    if missing:
        raise SystemExit(f"Plugin jar is missing required entries: {', '.join(missing)}")

    plugin_yaml = jar.read("paper-plugin.yml").decode("utf-8")
    build_properties = parse_properties(jar.read("META-INF/headdb-build.properties").decode("utf-8"))

plugin_version = yaml_scalar(plugin_yaml, "version")
if plugin_version != expected_version:
    raise SystemExit(f"paper-plugin.yml version mismatch: actual={plugin_version}, expected={expected_version}")

for placeholder in ("@project.version@", "${project.version}", "${headdb.build.version}"):
    if placeholder in plugin_yaml:
        raise SystemExit(f"paper-plugin.yml contains an unresolved placeholder: {placeholder}")

if yaml_scalar(plugin_yaml, "main") != "io.github.silentdevelopment.headdb.paper.HeadDBPlugin":
    raise SystemExit("paper-plugin.yml contains the wrong main class")

if yaml_scalar(plugin_yaml, "api-version") != "26.1.2":
    raise SystemExit("paper-plugin.yml contains the wrong Paper API version")

expected = {
    "headdb.build.channel": expected_channel,
    "headdb.build.number": expected_number,
    "headdb.build.attempt": expected_attempt,
    "headdb.build.run-id": expected_run_id,
    "headdb.build.commit": expected_commit,
}

for key, value in expected.items():
    if not value:
        continue
    actual = build_properties.get(key)
    if actual != value:
        raise SystemExit(f"Build metadata mismatch for {key}: actual={actual}, expected={value}")

print(f"Validated {jar_path} as HeadDB {expected_version} ({expected_channel}).")
PY
