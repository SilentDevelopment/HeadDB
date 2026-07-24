#!/usr/bin/env bash
set -euo pipefail

./mvnw -B -ntp help:evaluate -Dexpression=project.version -q -DforceStdout
