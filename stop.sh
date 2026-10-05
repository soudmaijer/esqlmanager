#!/usr/bin/env bash
# Stops and removes the local PostgreSQL container that start.sh created, including its data.
set -euo pipefail

CONTAINER=esql-pg

if [ "$(docker ps -aq -f "name=^${CONTAINER}$")" ]; then
	docker rm -f "${CONTAINER}" >/dev/null
	echo "Removed ${CONTAINER}."
else
	echo "${CONTAINER} does not exist."
fi
