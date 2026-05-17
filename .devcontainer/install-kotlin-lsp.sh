#!/usr/bin/env bash
# kotlin-lsp is not on the VS Code Marketplace; install the JetBrains VSIX once per codespace.
set -euo pipefail

KOTLIN_LSP_VERSION="${KOTLIN_LSP_VERSION:-262.4739.0}"

case "$(uname -m)" in
    x86_64) lsp_arch="amd64" ;;
    aarch64) lsp_arch="aarch64" ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

vsix="kotlin-server-${KOTLIN_LSP_VERSION}-linux-${lsp_arch}.vsix"
primary="${HOME}/.vscode-remote/extensions"
secondary="${HOME}/.vscode-server/extensions"

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

curl -fsSL -o "${tmpdir}/${vsix}" \
    "https://download-cdn.jetbrains.com/kotlin-lsp/${KOTLIN_LSP_VERSION}/${vsix}"

unzip -q "${tmpdir}/${vsix}" -d "${tmpdir}/unpack"

publisher="$(jq -r .publisher "${tmpdir}/unpack/extension/package.json")"
name="$(jq -r .name "${tmpdir}/unpack/extension/package.json")"
version="$(jq -r .version "${tmpdir}/unpack/extension/package.json")"
ext_id="${publisher}.${name}-${version}"

dest="${primary}/${ext_id}"
if [[ -d "${dest}" ]]; then
    echo "kotlin-lsp already installed: ${ext_id}"
else
    mkdir -p "${primary}"
    mv "${tmpdir}/unpack/extension" "${dest}"
    echo "Installed ${ext_id} (${lsp_arch}, ~$(du -sh "${dest}" | cut -f1))"
fi

mkdir -p "${secondary}"
link="${secondary}/${ext_id}"
rm -rf "${link}"
ln -sfn "${dest}" "${link}"

echo "kotlin-lsp ready (primary: ${dest})"
