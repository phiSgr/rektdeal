#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export KOTLIN_JUPYTER_JAVA_HOME="${KOTLIN_JUPYTER_JAVA_HOME:-${JAVA_HOME:?JAVA_HOME is not set}}"
export KOTLIN_JUPYTER_JAVA_OPTS="${KOTLIN_JUPYTER_JAVA_OPTS:---enable-native-access=ALL-UNNAMED}"

cd "${repo_root}/examples"
exec jupyter notebook --ip=0.0.0.0 --port=8888 --no-browser
