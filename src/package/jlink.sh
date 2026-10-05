#!/bin/sh
# Builds a runtime image with only the JDK modules the application needs: target/runtime-image.
# Run by the jlink profile after the dependencies were copied to target/libs.
set -eu
cd "$(dirname "$0")/../.."

JAVA_BIN="${JAVA_HOME:-$(dirname "$(dirname "$(command -v java)")")}/bin"

# jdeps finds the modules the code and its libraries call directly (the application is not modular).
DETECTED=$("$JAVA_BIN/jdeps" --multi-release 25 --ignore-missing-deps --print-module-deps -cp 'target/libs/*' target/classes)

# Loaded by name or through ServiceLoader, jdeps cannot see them: JDBC drivers (java.sql, java.naming for Oracle and
# SQL Server, java.xml.crypto and java.security.jgss for Kerberos), TLS (jdk.crypto.ec), all character sets and locales.
# The remaining modules are the closure of what the drivers need (checked with jdeps on the MySQL and Oracle drivers:
# java.management, java.naming, java.security.sasl, java.sql, jdk.net, jdk.security.jgss), so none can be dropped.
# jdk.localedata is by far the largest, so only the locales the user interface is likely to be used in are kept.
LOCALES=en,nl,de,fr,es
EXTRA=java.logging,java.management,java.naming,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported

rm -rf target/runtime-image
"$JAVA_BIN/jlink" \
	--add-modules "$DETECTED,$EXTRA" \
	--include-locales="$LOCALES" \
	--strip-debug \
	--compress zip-9 \
	--no-header-files \
	--no-man-pages \
	--output target/runtime-image

echo "Modules: $("$JAVA_BIN/java" --list-modules 2>/dev/null | wc -l | tr -d ' ') in the JDK, $(target/runtime-image/bin/java --list-modules | wc -l | tr -d ' ') in the image"
du -sh target/runtime-image
