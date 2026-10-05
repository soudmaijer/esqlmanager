#!/bin/sh
# Packages the application with jpackage into target/dist.
# Usage: jpackage.sh <pom version> [type]. The type is app-image (default), dmg, pkg, exe, msi, deb or rpm.
set -eu
cd "$(dirname "$0")/../.."

POM_VERSION="$1"
TYPE="${2:-app-image}"
NAME=eSQLManager

JAVA_BIN="${JAVA_HOME:-$(dirname "$(dirname "$(command -v java)")")}/bin"

# jpackage wants a numeric version: 1.0-SNAPSHOT becomes 1.0.0
NUMERIC=$(echo "$POM_VERSION" | sed 's/[-+].*//')
case "$NUMERIC" in *.*.*) ;; *.*) NUMERIC="$NUMERIC.0" ;; *) NUMERIC="$NUMERIC.0.0" ;; esac

[ -d target/runtime-image ] || sh src/package/jlink.sh

# The jar, its dependencies and a copy of runtime/ (conf/, credits.txt) which the application seeds its data directory from
INPUT=target/jpackage-input
rm -rf "$INPUT" target/dist
mkdir -p "$INPUT" target/dist
cp target/esqlmanager-"$POM_VERSION".jar target/libs/*.jar "$INPUT"/
cp -R runtime "$INPUT"/seed

# The icon format depends on the platform, the app is built without one where it cannot be made
ICON=""
if [ "$(uname)" = Darwin ] && command -v rsvg-convert >/dev/null && command -v iconutil >/dev/null; then
	ICONSET=target/icon/eSQLManager.iconset
	rm -rf target/icon && mkdir -p "$ICONSET"
	for size in 16 32 128 256 512; do
		rsvg-convert -w $size -h $size src/main/resources/icons/logo.svg -o "$ICONSET/icon_${size}x${size}.png"
		rsvg-convert -w $((size * 2)) -h $((size * 2)) src/main/resources/icons/logo.svg -o "$ICONSET/icon_${size}x${size}@2x.png"
	done
	iconutil -c icns "$ICONSET" -o target/icon/eSQLManager.icns
	ICON="--icon target/icon/eSQLManager.icns"
else
	echo "No icon: needs macOS with rsvg-convert and iconutil, the default Java icon is used"
fi

# $APPDIR is replaced by jpackage with the directory of the jar files
# shellcheck disable=SC2086
"$JAVA_BIN/jpackage" \
	--type "$TYPE" \
	--name "$NAME" \
	--app-version "$NUMERIC" \
	--vendor "Errorsoft" \
	--description "Database manager for MySQL and PostgreSQL" \
	--input "$INPUT" \
	--main-jar esqlmanager-"$POM_VERSION".jar \
	--main-class nl.errorsoft.esql.Main \
	--runtime-image target/runtime-image \
	--java-options '-Desql.seed=$APPDIR/seed' \
	--java-options '--enable-native-access=ALL-UNNAMED' \
	$ICON \
	--dest target/dist

du -sh target/dist/*
