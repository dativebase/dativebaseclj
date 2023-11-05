FROM flyway/flyway:9.10-alpine

USER root

RUN apk update

ADD scripts/run-migrations /usr/local/bin/
ADD ./migrator /flyway/sql

RUN rm /flyway/sql/put-your-sql-migrations-here.txt

ENV FLYWAY_POSTGRESQL_TRANSACTIONAL_LOCK=false

ENTRYPOINT ["/usr/local/bin/run-migrations"]