#!/usr/bin/env bash
# Adds one version to a composite p2 update site.
#
# Usage: ./publish.sh <site-dir> <zip-with-update-site> <version-dir-name>
#
# The site directory keeps one sub directory per published version, so Eclipse
# can see all of them at the same URL and update across versions.
set -euo pipefail

SITE_DIR="${1:?usage: publish.sh <site-dir> <zip> <version-dir-name>}"
SOURCE_ZIP="${2:?usage: publish.sh <site-dir> <zip> <version-dir-name>}"
VERSION_DIR="${3:?usage: publish.sh <site-dir> <zip> <version-dir-name>}"
VERSION_DIR="$(printf '%s' "${VERSION_DIR}" | sed 's/[^a-zA-Z0-9._-]/_/g')"

mkdir -p "${SITE_DIR}"
rm -rf "${SITE_DIR:?}/${VERSION_DIR}"
mkdir -p "${SITE_DIR}/${VERSION_DIR}"
unzip -q "${SOURCE_ZIP}" -d "${SITE_DIR}/${VERSION_DIR}"

for required in content.jar artifacts.jar; do
  if [ ! -f "${SITE_DIR}/${VERSION_DIR}/${required}" ]; then
    echo "ERROR: ${required} not found in ${SOURCE_ZIP}" >&2
    exit 1
  fi
done

TIMESTAMP="$(date +%s)000"

CHILDREN="$(cd "${SITE_DIR}" && for d in */; do d="${d%/}"; if [ -f "${d}/content.jar" ] || [ -f "${d}/content.xml" ]; then printf '%s\n' "${d}"; fi; done | sort -V)"

{
  echo "<?compositeMetadataRepository version='1.0.0'?>"
  echo "<repository name='SmartyPDT Update Site' type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>"
  echo "  <properties size='1'>"
  echo "    <property name='p2.timestamp' value='${TIMESTAMP}'/>"
  echo "  </properties>"
  echo "  <children size='$(printf '%s\n' "${CHILDREN}" | wc -l | tr -d ' ')'>"
  printf '%s\n' "${CHILDREN}" | while read -r child; do
    [ -n "${child}" ] && echo "    <child location='${child}'/>"
  done
  echo "  </children>"
  echo "</repository>"
} > "${SITE_DIR}/compositeContent.xml"

{
  echo "<?compositeArtifactRepository version='1.0.0'?>"
  echo "<repository name='SmartyPDT Update Site' type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>"
  echo "  <properties size='1'>"
  echo "    <property name='p2.timestamp' value='${TIMESTAMP}'/>"
  echo "  </properties>"
  echo "  <children size='$(printf '%s\n' "${CHILDREN}" | wc -l | tr -d ' ')'>"
  printf '%s\n' "${CHILDREN}" | while read -r child; do
    [ -n "${child}" ] && echo "    <child location='${child}'/>"
  done
  echo "  </children>"
  echo "</repository>"
} > "${SITE_DIR}/compositeArtifacts.xml"

cat > "${SITE_DIR}/p2.index" <<'EOF'
version=1
metadata.repository.factory.order=compositeContent.xml,\!
artifact.repository.factory.order=compositeArtifacts.xml,\!
EOF

echo "Published ${VERSION_DIR} in ${SITE_DIR}"
printf '%s\n' "${CHILDREN}" | sed 's/^/  - /'
