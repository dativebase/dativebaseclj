================================================================================
  OLD Migrator
================================================================================

The OLD uses flyway for database migrations.


Creating a Migration
================================================================================

From the ``old`` project root::

  $ ./scripts/create-migration.sh <migration_name>


Generating the Schema
================================================================================

Run::

  $ docker-compose up -d --build

After you've written a migration, dump the schema into ``schema.sql`` by
running::

  $ make schema.sql


Running Migrations
================================================================================

You must use ``--build`` to rebuild the image in order for flyway to recognize
new migrations.::

  $ docker-compose up -d --build
