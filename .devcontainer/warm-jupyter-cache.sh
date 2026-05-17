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

cache_root="${HOME}/.jupyter_kotlin/maven_repository"
jar_count="$(find "${cache_root}" -name '*.jar' | wc -l | tr -d ' ')"
cache_size="$(du -sh "${cache_root}" | cut -f1)"

echo "Kotlin Jupyter cache warmed: ${cache_root} (${jar_count} jars, ${cache_size})"
echo "  rektdeal + transitives (com.github.phisgr):"
find "${cache_root}" -path '*/com/github/phisgr/*' -name '*.jar' ! -name '*-sources.jar' \
  | sed "s|.*/.m2.cache/||" | sort | sed 's/^/    /'
echo "  (+ dataframe / %use libraries and other resolved deps in the same cache)"
