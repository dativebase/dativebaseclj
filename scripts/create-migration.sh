#!/bin/bash
set -e

NAME="${1}"
TIMESTAMP="$(date +%Y%m%d%H%M%S)"

touch "migrator/sql/V${TIMESTAMP}__${NAME}.sql"
