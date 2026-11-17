#!/usr/bin/env bash
# Renders the deployment diagram from its Graphviz source. Needs Graphviz (`apt-get install graphviz`).
set -euo pipefail
cd "$(dirname "$0")"
dot -Tpng -Gdpi=110 deployment-diagram.dot -o deployment-diagram.png
dot -Tsvg deployment-diagram.dot -o deployment-diagram.svg
