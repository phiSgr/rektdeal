#!/usr/bin/env bash
set -euo pipefail

export KOTLIN_JUPYTER_JAVA_HOME="${KOTLIN_JUPYTER_JAVA_HOME:-${JAVA_HOME:?JAVA_HOME is not set}}"
export KOTLIN_JUPYTER_JAVA_OPTS="${KOTLIN_JUPYTER_JAVA_OPTS:---enable-native-access=ALL-UNNAMED}"

warmup_dir="$(mktemp -d)"
trap 'rm -rf "$warmup_dir"' EXIT

cp /tmp/warmup.ipynb "$warmup_dir/"
cd "$warmup_dir"

jupyter nbconvert --to notebook --execute warmup.ipynb \
  --ExecutePreprocessor.kernel_name=kotlin \
  --ExecutePreprocessor.timeout=900

echo "Kotlin Jupyter cache warmed (${HOME}/.jupyter_kotlin/maven_repository)"
