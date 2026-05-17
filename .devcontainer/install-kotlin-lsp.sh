#!/usr/bin/env bash
# kotlin-lsp is not on the VS Code Marketplace; install the JetBrains VSIX into extension dirs.
set -euo pipefail

KOTLIN_LSP_VERSION="${KOTLIN_LSP_VERSION:-262.4739.0}"

case "$(uname -m)" in
    x86_64) lsp_arch="amd64" ;;
    aarch64) lsp_arch="aarch64" ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

vsix="kotlin-server-${KOTLIN_LSP_VERSION}-linux-${lsp_arch}.vsix"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

curl -fsSL -o "${tmpdir}/${vsix}" \
    "https://download-cdn.jetbrains.com/kotlin-lsp/${KOTLIN_LSP_VERSION}/${vsix}"

unzip -q "${tmpdir}/${vsix}" -d "${tmpdir}/unpack"

publisher="$(jq -r .publisher "${tmpdir}/unpack/extension/package.json")"
name="$(jq -r .name "${tmpdir}/unpack/extension/package.json")"
version="$(jq -r .version "${tmpdir}/unpack/extension/package.json")"
ext_id="${publisher}.${name}-${version}"

for base in "${HOME}/.vscode-server" "${HOME}/.vscode-remote"; do
    dest="${base}/extensions/${ext_id}"
    mkdir -p "$(dirname "$dest")"
    rm -rf "$dest"
    cp -a "${tmpdir}/unpack/extension" "$dest"
done

echo "Installed ${ext_id} for kotlin-lsp ${KOTLIN_LSP_VERSION} (${lsp_arch})"
