#!/usr/bin/env bash
# Changes the base part of the plug-in version, i.e. the PDT version that this
# build tracks. Our own release number lives in the build qualifier (the git
# tag), so only this base version has to be bumped when PDT moves on.
#
# Usage: ./set-pdt-version.sh 8.5.0
set -euo pipefail
cd "$(dirname "$0")"

NEW_VERSION="${1:-}"
if ! printf '%s' "${NEW_VERSION}" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "usage: ./set-pdt-version.sh <pdt-major>.<pdt-minor>.<pdt-micro>   e.g. 8.5.0" >&2
  exit 1
fi

CURRENT_VERSION="$(sed -n 's/^Bundle-Version: \([0-9.]*\)\.qualifier$/\1/p' org.eclipse.php.smarty.core/META-INF/MANIFEST.MF | head -1)"
if [ -z "${CURRENT_VERSION}" ]; then
  echo "ERROR: cannot read the current Bundle-Version" >&2
  exit 1
fi
echo "==> Bumping ${CURRENT_VERSION} -> ${NEW_VERSION}"

FILES="pom.xml
org.eclipse.php.smarty.core/pom.xml
org.eclipse.php.smarty.ui/pom.xml
org.eclipse.php.smarty-feature/pom.xml
org.eclipse.php.smarty.repository/pom.xml
org.eclipse.php.smarty-feature/feature.xml
org.eclipse.php.smarty.core/META-INF/MANIFEST.MF
org.eclipse.php.smarty.ui/META-INF/MANIFEST.MF
README.md"

printf '%s\n' "${FILES}" | while read -r file; do
  [ -f "${file}" ] || continue
  sed -i.bak "s/${CURRENT_VERSION//./\\.}/${NEW_VERSION}/g" "${file}"
  rm -f "${file}.bak"
  echo "    updated ${file}"
done

echo "==> Done. Now also update the target platform URL in pom.xml if PDT moved to a newer release train."
