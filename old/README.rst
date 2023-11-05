================================================================================
  The Online Linguistic Database (OLD)
================================================================================

This project is the Online Linguistic Database (OLD).

The OLD is an HTTP REST API that conforms to the OpenAPI spec.

This project is written in Clojure. This is a rewrite of a previous project of
the same name, written in Python. See TODO. When it is important to distinguish
between the two projects, this one may be referred to as "OLD-CLJ".


Usage
================================================================================

To serve the OLD and a Swagger UI for interacting with it::

  $ lein run

Now visit the Swagger UI at::

  http://localhost:8080/swagger-ui/dist/index.html

Click the "Authorize" button and enter the API key "olddative".

Now click "GET /api/v1/forms", then "Try it out", then "Execute". The Swagger UI
will make a request to the OLD and will receive a mock response.


How To Create Database Migrations
================================================================================

First create a new migration file under ``migrator/sql`` with::

  $ ./scripts/create-migration.sh replace_me_with_migration_name

Then rebuild the docker images and bring up the containers in order to trigger
the Flyway container ``migrator`` into creating the database schema in the
``postgres`` container::

  $ docker-compose up -d --build --force-recreate

Verify that the migrator exited successfully, with either of the following::

  $ docker-compose logs -f migrator
  $ docker-compose ps

Finally, write the schema to ``schema.sql`` so that the revised schema (post
migration application) can be checked into version control::

  $ make schema.sql


TODOs
================================================================================

- stats infra https://www.metricfire.com/blog/monitoring-your-infrastructure-with-statsd-and-graphite/
- dativetop cljfx https://github.com/cljfx/cljfx
- Specs
  - DB specs
- DONE. Distinguish inserted_at from created_at in "local" models.
- DONE. postgresql docker image
- DONE. hugsql [com.layerware/hugsql "0.5.3"]
