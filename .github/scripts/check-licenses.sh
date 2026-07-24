#!/usr/bin/env bash
set -euo pipefail

test -f LICENSE
test -f LICENSES/Apache-2.0.txt
test -f LICENSES/GPL-3.0-or-later.txt

grep -Eq 'Apache License|Apache-2.0' LICENSES/Apache-2.0.txt
grep -Eq 'GNU GENERAL PUBLIC LICENSE|GPL' LICENSES/GPL-3.0-or-later.txt
