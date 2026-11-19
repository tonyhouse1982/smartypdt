#!/usr/bin/env bash
# Build smartypdt and produce a p2 update-site archive installable from Eclipse.
set -euo pipefail
cd "$(dirname "$0")"

# Java 21+ is required: Eclipse 2026-03 / PDT 8.4 bundles declare JavaSE-21.
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
fi
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"

QUALIFIER="${QUALIFIER:-$(date +%Y%m%d%H%M)}"
# OSGi build qualifiers may only contain [0-9a-zA-Z_-]
QUALIFIER="$(printf '%s' "${QUALIFIER}" | sed 's/[^a-zA-Z0-9_-]/_/g')"

echo "==> Java: $(java -version 2>&1 | head -1)"
echo "==> Build qualifier: ${QUALIFIER}"

mvn -B clean verify -DforceContextQualifier="${QUALIFIER}" -Dtycho.localArtifacts=ignore

REPO_DIR="org.eclipse.php.smarty.repository/target/repository"
OUT="dist/smartypdt-update-site-0.11.0-${QUALIFIER}.zip"
mkdir -p dist
rm -f dist/smartypdt-update-site-*.zip
( cd "${REPO_DIR}" && zip -q -r -X "../../../${OUT}" . )

echo "==> Update site: $(pwd)/${OUT}"
echo "    Eclipse: Help > Install New Software > Add > Archive... > select the zip"
