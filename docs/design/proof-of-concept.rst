================================================================================
  Design Doc: Proof-of-Concept
================================================================================

Problems
================================================================================

Dative and the OLD are used by linguists and researchers for linguistic
fieldwork and data management. However, Dative can no longer be updated, due to
bit rot. The OLD does not allow for easy creation of new instances. Finally,
users want to be able to (but cannot currently) use Dative in the field without
Internet access and then have their offline work automatically sync up with the
online OLD.


Solution
================================================================================

We will re-write the OLD and Dative (in Clojure and ClojureScript, respectively)
from the ground up in order to address the above problems from the outset. The
initial deliverable will be a continuously delivered proof-of-concept
online/offline linguistic data management system from the outset. The PoC will
have breadth but not depth. It will support a data model that is a subset of
that supported by the current Dative/OLD; that is, it will support management of
OLD instances, users with privileges on them, and linguistic forms with only
transcriptions and metadata.

Once this is complete, a second phase will deliver nearly full feature parity
with the current Dative/OLD. Once this Phase II is complete, the OLD, Dative,
and DativeTop will support forms, files, collections, corpora, tags, reified
searches, etc. This phase will not implement the morphological parsing machinery
of the current OLD, which will be left for a potential Phase III epic.


Solution Details: the Online Linguistic Database (OLD)
--------------------------------------------------------------------------------

The OLD will be an OpenAPI-compliant REST API. It will allow authentication via
time-boxed API keys. It will use PostgreSQL instead of MySQL. It will support
multiple OLDs within a single database (using an indexed old_slug column on all
tables) as well as a single table for all users, with each user having
role-based access to zero or more OLDs. The OLD will support a "sync" (or
"rebase") endpoint which will allow a client to POST a changeset and receive a
rebased changeset to support read/write sync. The OLD will have observability
built in, using statsd for metrics and queryable logs. The OLD will not run
potentially massive synchronous updates on multiple forms in response to an
update on a single form (as the current OLD does). Finally, the OLD will have
indexes on queryable form attributes and will not expose non-query-able
attributes in the search API.


Solution Details: Dative
--------------------------------------------------------------------------------

Dative will allow interaction with an OLD. It will support cookie-based
authentication, reading and writing of proto-forms, users and OLDs, and viewing
of API keys. Dative will be deployed on the same (a subdomain of) URL as the
OLD, to avoid CORS issues. We may need to fork the current DativeRF project in
order to accomplish all of this.


Solution Details: DativeTop
--------------------------------------------------------------------------------

DativeTop will allow interaction with local OLDs stored on the user's machine
(in the file system and in SQLite databases.) It will allow for sync
configuration such that a background process will use the OLD "sync" API to
seamlessly sync local changes with remote. Configuring synchronization will
require initial autentication via Dative in order to retrieve the API key and
store it locally. It will work on Mac and Windows.

All of the above projects will be continuously integrated and delivered via
CircleCI. They will be hosted on CIPL servers (or Digital Ocean servers, if that
is not possible).


Details of the Phase I: Proof-of-Concept DativeBase
================================================================================

The proof-of-concept goal is for DativeBase to do the following. It models
OLDs, users and forms by exposing CRUD operations on each via an
OpenAPI-compliant HTTP REST API, the OLD. It also exposes a "rebase" API which
will allow an external OLD to request that a supplied changeset be rebased on
the target OLD's changeset, in the same manner as Git rebase works. It provides
Dative to interact with the OLDs. It provides DativeTop to interact with local
OLDs and to sync them with remote ones. To accomplish this, the following
components must meet the following requirements.

- OLD(CLJ). The OLD exposes an OpenAPI-compliant REST API for performing CRUD
  operations on OLDs, users and forms. It also exposes a rebase endpoint which
  accepts an arbitrary changeset and returns a rebased changeset which the local
  OLD must use to update its state to match that of the lead OLD.
- Dative(CLJ). This is the currently under development front-end Re-frame app
  dativerf. It is basically a rewrite of the Dative CoffeeScript app. It should
  be able to use the REST API of OLD(CLJ) to manage OLDs, users and forms.
- DativeTop(CLJ). This will be a CLJFX (JavaFX) desktop application. It
  basically works like Dative(CLJ) but with locally-stored SQLite databases
  (instead of PostgreSQL). It also runs asynchronous processes to synchronize
  its local OLDs with any leader OLDs that they are following.
- Migrator. This will be a tool that can migrate a legacy OLD to the current OLD
  version. It should be able to run idempotently and continuously. That is, we
  should be able to partially migrate legacy OLD data to a WIP current OLD
  service, at will.


Epic: Phase I: Proof-of-Concept DativeBase
================================================================================

Once this epic is complete, the following will be true.

1. OLDCLJ will be live.
2. DativeCLJ will be live.
3. DativeTopCLJ cross-platform images will be downloadable from
   dativebase.dative.ca.
4. Operators will be able to migrate legacy OLD data to OLDCLJ.
5. Users will be able to access OLDCLJ from DativeCLJ.
6. Users will be able to construct minimal local OLDs in DativeTopCLJ.
7. Read/write offline/online synchronization with OLDCLJ will work in DativeTop.


Estimate
--------------------------------------------------------------------------------

Assuming that two points of complexity is approximately one day of developer
work and assuming 100 complexity points (see below), I estimate that the PoC
DativeBase epic can be completed in 50 full-time developer days or ten weeks of
full-time developer work.

At eight hours per day and $50 per hour, the estimated development cost would be
$20,000.


Tickets
--------------------------------------------------------------------------------

Here I break down the tickets according to the five components/project of
DativeBase: OLD, Dative, DativeTop, Migrator and the DativeBase static site.

- Total complexity points: 100

  - OLD Tickets total complexity points: 40
  - Dative Tickets total complexity points: 11
  - DativeTop Tickets total complexity points: 28
  - Migrator Tickets total complexity points: 5
  - DativeBase Web Site Tickets total complexity points: 6


OLD Tickets
````````````````````````````````````````````````````````````````````````````````

Total ticket count: 17
Total complexity points: 40

- Implement CI/CD for the OLD (2)
- Add observability deployment infrastructure for metrics (3)
  - See https://www.metricfire.com/blog/monitoring-your-infrastructure-with-statsd-and-graphite/
- Add observability deployment infrastructure for logs (2)
- Add observability application logic infrastructure for metrics (2)
- Add RabbitMQ external dependency (2)
- Add RabbitMQ application logic handler infrastructure (2)
- Add RabbitMQ dead letter queue (2)
- Add specs for database tables (2)
- Expose OpenAPI-compliant REST endpoints for CRUD operations on OLDs (2)
- Expose OpenAPI-compliant REST endpoints for CRUD operations on forms (2)
- Expose OpenAPI-compliant REST endpoints for CRUD operations on users (2)
- Allow authentication via both API key and cookies (3)
- Ensure passwords can be reset (3)
- Expose a minimal search API for forms (with regex-compatible indices) (3)
- Expose an async endpoint to synchronize events (3)
  - This must allow a local OLD to send a payload of mutation events to be
    rebased upon a remote leader OLD's events.
- Expose a sync endpoint to poll for completed synchronization events (2)
- Add on-call alerting around metrics and DLQ presence (3)


Dative Tickets
````````````````````````````````````````````````````````````````````````````````

Total ticket count: 6
Total complexity points: 11

- Determine whether DativeRF must be forked (1)
- Implement CI/CD for Dative (same domain as OLD) (2)
- Ensure username/password (cookie) authentication works (2)
- Ensure OLDs can be viewed and managed (2)
- Ensure users can be viewed and managed (API keys retrieved) (2)
- Ensure forms can be viewed and managed (2)


DativeTop Tickets
````````````````````````````````````````````````````````````````````````````````

Total ticket count: 11
Total complexity points: 28

- Create PoC CLJFX DativeTop desktop app (3)
- Ensure DativeTop can build Mac OS images in CI (3)
- Ensure DativeTop can build Windows images in CI (3)
- Copy the OLD db Clojure spec to a local SQLite version, modifying as needed (2)
- Copy the OLD db schema to a local SQLite version, modifying as needed (3)
- Ensure db migrations work on local SQLite dbs (3)
- Ensure API key authentication to remote OLDs works (2)
- Ensure OLDs can be viewed and managed (2)
- Ensure users can be viewed and managed (2)
- Ensure forms can be viewed and managed (2)
- Confirm with real users that DativeTop works as expected on both platforms (3)


Migrator Tickets
````````````````````````````````````````````````````````````````````````````````

Total ticket count: 1
Total complexity points: 5

- Operator admin commands and docs to migrate a legacy OLD to current (5)

  - Ensure character encoding and unicode normalization work
  - Ensure rsync works efficiently for file transfer (if applicable)
  - Synchronize this work with the migration of legacy OLDs to CIPL


DativeBase Web Site Tickets
````````````````````````````````````````````````````````````````````````````````

Total ticket count: 2
Total complexity points: 6

- Create minimal static DativeBase web site (3)

  - Its static content should be an updated synthesis of www.dative.ca and
    www.onlinelinguisticdatabase.org.
  - Decide on the name "DativeBase" for this project and use it consistently (or
    choose another).

- Ensure that CI/CD in DativeTop results in build artifacts uploaded to the
  DativeBase web site (3)


References
================================================================================

- cljfx: https://github.com/cljfx/cljfx
