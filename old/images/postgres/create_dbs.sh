#!/bin/bash
set -e

POSTGRES="psql --username ${POSTGRES_USER}"

for DB in $POSTGRES_DBS; do
    echo "Creating database: ${DB}"

    $POSTGRES <<EOSQL
CREATE DATABASE ${DB} OWNER ${POSTGRES_USER};
EOSQL

done
