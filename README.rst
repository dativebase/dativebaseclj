================================================================================
  DativeBase in Clojure
================================================================================

DativeBase is an application for linguistic data management. It is designed to
be useful for linguists, language revitalizers, teachers, linguaphiles, and
anybody who needs to manage language-focused data. DativeBase facilitates
storing, searching, sharing, and analyzing linguistic data.


TODOs
================================================================================

- TODO: As a user of DativeBase, I should be able to request access to an OLD.
- TODO: refactor and DRY-ify these things
  - DVB Client (split out into separate namespaces and/or create helper
    abstractions)
  - E2E test ns is one single large file. Break it up
- TODO: Support password reset
- TODO: Consider introducing rate limiting on user creation to help prevent
  abuse, i.e., overloading the system with user creation requests from bots.


Authorization
================================================================================

This section details the authorization rules in DativeBase. The guiding
principle underlying the authorization design decisions described below is that
anyone should be able to sign up for a free DativeBase plan and get started
using DativeBase right away.

Our first important, foundational distinction is that between OLD-specific
resources, like forms, and OLD-independent resources, like users, plans and OLDs.

In general, if a user has the contributor or administrator role for a given OLD,
then that user will be authorized to make mutative (data-changing) requests on
any resource under said OLD. A user with the viewer role under an OLD can only
make read requests on that OLD.

A second salient distinction is superuser status. Each user is a superuser or a
non-superuser. Most users are non-superusers. A superuser can, in general,
perform any read or write action in the system. All entities must reference a
user as their creator and updater. The only exception to this is the user
entity itself, which must be boostrap-able without a user.

An OLD must have an active plan in order to be usable. If an action on an OLD is
not covered by the entitlements granted by the plan, then the action will be
prohibited.


Authorization for the User Resource
--------------------------------------------------------------------------------

Users are the entrypoint to DativeBase. Anyone can create a user and then use
that user to create a free plan and then a number of OLDs running under that
free plan.


User Creation
````````````````````````````````````````````````````````````````````````````````

A new user may be created via the create-user operation, i.e., POST /users.

Authentication is not required in order to create a user in DativeBase. Such
user creation is effectively signup. Anybody on the public internet should be
able to sign up to DativeBase. They should be able to create a user, a free
plan, and one or more OLDs (with restricted entitlements) under said plan.

Obviously, since a superuser has unlimited access, a user created without
authentication may never be a superuser.

In addition, a user created without authentication must be activated before it
can be used. User activation means hitting a specific endpoint with a specific,
randomly generated UUID in its path. In production, this URL will be emailed to
the user.


User Update & Delete
````````````````````````````````````````````````````````````````````````````````

User update is only allowed to superusers and the target user itself. Only a
superuser can update a user into a superuser. Therefore, a superuser can only be
created by someone with backend access to the DativeBase system.

User deletion is prohibited. Soft deletion may be supported in the future. Lossy
or non-lossy user redaction may also be supported in the future. The challenge
with user deletion is that provenance is crucial to a knowledge base such as
DativeBase. Therefore, full user deletion, without careful attention, would
corrupt the data.

It would probably be wise to support user deactivation (as a minimal user
deletion strategy) in the short term. It should be noted that a user could still
exist in the system while having access to no OLDs and no plans, which in itself
is a form of deactivation.


User Read
````````````````````````````````````````````````````````````````````````````````

Any authenticated and activated user can view (read) the set of users in
DativeBase. Users need to be able to view other users in order to be able to add
these users to their OLDs and/or to their plans.

On the other hand, users should be able to submit a request for access to an OLD
and administrators should be able to view such requests.

Note that non-superusers receive limited user data. They are not able to view the
email addresses of users, for example. A non-superuser can view their own data
in full, howeever.



Authorization for the Plan Resource
--------------------------------------------------------------------------------

Once a non-superuser has been created, the typical next step is to create a free
plan with that user. A free plan allows limited access to the DativeBase
service. The details are still to-be-developed. However, we may provisionally
assume that each free plan allows for 3 OLDs building under it, each with a
maximum number of forms. Further restrictions may be enabled later.


Plan Creation
````````````````````````````````````````````````````````````````````````````````

Any user may create a new, free plan. This is accomplished via a POST /plans
request.

However, each (non-superuser) user is permitted to be the manager of at most 1
plan. Given that creating a plan also entails the creator receiving a manager
role on said plan, this means, in effect, that each (non-superuser) user can
only create one plan. (If the user revokes their manager role over the plan,
then they may create a new plan.)


Plan Update & Delete
````````````````````````````````````````````````````````````````````````````````

Plan update is not currently supported. The only property of a plan that can
meaningfully be updated is the tier and upgrading the tier from free to higher
requires a billing event.

A plan can be deleted by a superuser or one of the plan's managers. However, a
plan cannot be deleted while it is supporting OLDs. If any OLDs are running
under a plan, then these OLDs must first be removed from the plan before it can
be deleted. To remove an OLD from a plan, update the OLD (PUT /olds/:id) while
setting the plan ID to nil.


Authorization for the OLD Resource
--------------------------------------------------------------------------------

OLDs are a core resource in DativeBase. Each OLD (= Online Linguistic Database)
is a data set, usually focused on a particular language, but sometimes on a
research topic.


OLD Creation
````````````````````````````````````````````````````````````````````````````````

Any user may create an OLD via the POST /olds operation. Creation of an OLD
automatically entails making the creating user an administrator of the
newly-created OLD.

An OLD that is not covered by a plan is not usable. An OLD can be configured to
be paid for under a plan during OLD creation or OLD update. In either case, the
authenticated user must be a manager of the plan in question (or a superuser of
the system) in order for the request to be authorized.


OLD Update & Deletion
````````````````````````````````````````````````````````````````````````````````

An OLD can be updated or deleted only by its administrators and by superusers.

All users can read the collection of OLDs (index) and get details on a specific
OLD (show). Users need to be able to browse the set of OLDs in order for
DativeBase to work.


Authorization for Forms and Other OLD-Dependent Resources
--------------------------------------------------------------------------------

Forms belong to OLDs. As do tags, corpora, files, phonologies, etc. A user's
authorization to read or write OLD-specific resources depends on that user's
role within the OLD.

An administrator can perform any action. A contributor can perform most write
actions and all reads. A viewer can perform all read actions but no writes.


User Flows
================================================================================

- Signup: person creates a DativeBase user
- Plan Creation: User creates a plan for managing OLDs.
- Grant Access: Administrator of an OLD grants access to a user to an OLD.
- Cover OLD: Administrator of a plan covers an OLD under that plan.

Signup
--------------------------------------------------------------------------------

As a prospective user of DativeBase, I can create an account (a user) in
DativeBase. As a result of signing up, a new user is created for me in
DativeBase.

Implications:

- Anybody on the public internet can create a new account.
- Email verification must be required. Therefore, signup is a two-step process.

  1. First, the user signs up by entering their PII and desired credentials.
     DativeBase then emails the user a registration confirmation link containing
     a key, which expires.
  2. Then, the user visits the link, which triggers authentiction. If the
     authentication test passes, the user is verified.


Steps to implement:

- All users must have a registration-status attribute. Its default is pending.
  It can transition from pending to registered.
- A pending user cannot perform any actions except verification. Once
  verification succeeds, the user becomes registered.


Plan Creation
--------------------------------------------------------------------------------

As a user of DativeBase, I can create a plan. A plan lets me pay for and manage
OLDs. If I have a plan, I can create new OLDs that are covered by that plan,
insofar as the entitlements of my plan allow for this. If I have a plan, I can
cover existing with that plan. I can transfer coverage of an OLD from its
existing plan to my plan.


Grant Access
--------------------------------------------------------------------------------


Data Model
================================================================================

There are four basic entities:

- Users
- OLDs
- Plans
- Forms

Users have inherent roles. All users are either regular users or superusers.
Superusers have unlimited access to all public APIs.

A user may have access to an OLD or not. In order for a user to have access to
an OLD, there must be an active ``users_olds`` row linking said user to said OLD.
The ``role`` value of this row determines the user's level of access to the OLD.
An administrator can perform all actions on an OLD. A contributor can perform
nearly all actions on an OLD. A viewer can only perform read actions on an OLD;
no writes are permitted.

A plan pays for an OLD. Every OLD must be covered by a plan. If an OLD exceeds
the entitlements of its plan, then the OLD becomes non-operational. In order to
re-enable the OLD, the plan must be upgraded or the OLD must be moved under
another, more entitled plan.


Continuous Integration & Deployment
================================================================================


TODOs
================================================================================

- Ensure that the commands in the ``Docker`` section are working.
- I need to more clearly justify the inserted vs created distinction. Are both
  of these columns really necessary?
- Add stats infrastructure. See https://www.metricfire.com/blog/monitoring-your-infrastructure-with-statsd-and-graphite/.
- Add specs for database tables.


Principles
================================================================================

- Sustainability
- Open Data
- Immutability


Sustainability
--------------------------------------------------------------------------------

DativeBase must be sustainable. That is why it is both open-source and
monetizable as a service.

The source code of DativeBase is, and always will be, open-source and free. This
means that even if the maintainers and developers of DativeBase change, its
inner workings are always available for inspection, adoption, and future
development.

Software requires maintenance and non-remunerated maintenance is almost
inevitably short-lived. If DativeBase provides value to its users, then those
users should be happy to pay a modest fee for its use. If a prospective user
lacks the funds, they may reach out and be granted an exemption from the
subscription fee.


Open Data
--------------------------------------------------------------------------------

DativeBase will never hold your data hostage. DativeBase will provide full
exports of data to the owners or stewards of that data, in open formats, i.e.,
formats that do not require proprietary software to be read and manipulated.

DativeBase will provide standard OpenAPI-compliant HTTP REST endpoints for
fetching data sets. Datasets will be available in standard, open formats:
primarily JSON, .zip archives, and CSV files.

DativeBase will include local-first functionality. This may be a fully-fledged
Desktop application or it may be a progressive web app that stores data locally
in the browser's local storage. Whatever the case, DativeBase will give users
access to the data on their own machines. DativeBase will provide seamless
synchronization between local data and shared datasets on the server.


Immutability
--------------------------------------------------------------------------------

DativeBase will provide immutable data. This means data that both changes yet
also preserves its history. All previous states of all data points are preserved.

This strategy facilitates synchronization between local datasets and their
remote counterparts. However, it also preserves the history and provenance of
data, which may itself have scientific utility.


How Immutable Data Works in DativeBase
================================================================================

The data in DativeBase is immutable. This means that the data changes yet its
history is never lost. The effect of this is that updated or destroyed data can
be restored. Another, perhaps more important, consequence is that two versions
of a dataset (i.e., an OLD) can diverge and can later be merged (or
synchronized).

All immutable entities have their current state stored in traditional database
tables. For example, the current state of a form with ID "A" is stored in table
``forms``.

When an entity, such as a form, is deleted, we do not actually drop the row from
the database. Instead, we update its ``destroyed_at`` value, changing it from
``NULL`` to the timestamp of deletion.

To see the database schema of the OLD server, inspect the top-level file
``schema.sql``. Alternatively, interact with the database directly via PSQL
using ``make db`` and run commands like ``\dt`` and ``\d+ events``.


The ``events`` Table
--------------------------------------------------------------------------------

The histories of all immutable entities are stored in the ``events`` table.
Every time an entity is created, updated, or deleted, we store an event in this
table.

The data in the ``events`` table is (and must be) sufficient to fully
reconstruct all of the data within the DativeBase instance. That is, we should
be able to drop all rows from all other tables and then perfectly reconstruct
the data in those tables using only the data in the events table.

The ``events`` table is an append-only log. No SQL ``UPDATE`` or ``DELETE``
operations should ever be run on this table. Only ``INSERT`` oeprations are
permitted.

In order to fully understand the events table, one must first internalize the
basic relationship between users, OLDs, and OLD-internal types, prototypically
forms. Every user has access to zero or more OLDs. Every OLD contains zero or
more forms.

Here is the schema of the ``events`` table::

  CREATE TABLE public.events (
      id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
      created_at timestamp with time zone DEFAULT now(),
      old_slug text,
      table_name text NOT NULL,
      row_id uuid,
      row_data text NOT NULL,
      CONSTRAINT events_check_old_slug_or_row_id
        CHECK (((old_slug IS NOT NULL)
                OR (row_id IS NOT NULL)))
  );

Details on the columns of the ``events`` table are provided below.

- ``id``: This is the unique identifier and primary key of the event. Its value
  is A UUID.
- ``created_at``: This is a (UTC) timestamp indicating when the event was
  created in DativeBase.
- ``old_slug``: This is the slug (unique identifier) of the OLD to which the
  event applies.

  - Some entities, such as users, are not specific to a single OLD. The events
    of such non-OLD-specific entities will have a value of ``NULL`` in this
    column.
  - Other entities, such as forms, are specific to a single OLD. The events
    of such non-OLD-specific entities will have the slug of the entity's OLD in
    this column.

    - The OLDs themselves do have a non-null value in the ``events.old_slug``
      column. This value is the ``slug`` value of the OLD itself.

- ``table_name``: This is the name of the table where the entity's current state
  is held. The table defines the type of the entity. Forms, for example, are
  stored in the ``forms`` table and mutation events on forms have a value of
  ``"forms"`` in the ``table_name`` column of the ``events`` table.
- ``row_id``: This column holds the unique ID of the entity. Typically, this is
  the value of the ``id`` column in the corresponding entity table, e.g.,
  ``forms.id`` or ``users.id``.

  - Since OLDs use ``slug`` as their ID, mutation events on OLDs have a ``NULL``
    value in ``events.row_id``.

- ``row_data``: This column holds a serialized representation of the state of
  the entity at the ``created_at`` date.

  - The data in ``row_data`` is serialized using EDN.
  - Example:

    - If a new form is created with transcription ``"a"``, an event will be
      created where ``row_data`` contains an EDN-serialized representation of
      the form with transcription ``"a"``.
    - If a our form is updated to have transcription ``"b"``, an event will be
      created where ``row_data`` contains an EDN-serialized representation of
      the form with transcription ``"b"``.
    - Finally, if a our form is deleted, an event will be created where
      ``row_data`` contains an EDN-serialized representation of the form with a
      ``destroyed_at`` value of the timestamp of deletion.


The ``forms`` Table
--------------------------------------------------------------------------------

Forms are an example of an immutable and OLD-specific entity type. Forms are
stored in the ``forms`` table. See below.::

  CREATE TABLE public.forms (
      id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
      old_slug text NOT NULL,
      transcription text NOT NULL,
      inserted_at timestamp with time zone DEFAULT now() NOT NULL,
      created_at timestamp with time zone DEFAULT now() NOT NULL,
      updated_at timestamp with time zone DEFAULT now() NOT NULL,
      destroyed_at timestamp with time zone,
      created_by uuid NOT NULL
  );

Each form belongs to a specific OLD. The ``forms.old_slug`` value is the
``olds.slug`` value of the OLD to which the form belongs.

The ``inserted_at`` and ``created_at`` columns are similar in that both are
timestamps that default to the time of insertion. However, they are importantly
different. The ``created_at`` value indicates when the form was created by the
user. The ``created_at`` value should never change.

The ``inserted_at`` value is generally identical to ``created_at``. However,
when a changeset (i.e., an ordered set of events) is ingested into the OLD, the
``inserted_at`` value will be the time of insertion.


History of DativeBase
================================================================================

DativeBase is a complete rewrite (in Clojure & ClojureScript) of the existing
Dative/OLD suite of linguistic data management tools.

Dative is already 1/3 rewritten in ClojureScript. See DativeReFrame. That project
will become a submodule of this one.

The motivation behind this rewrite is twofold. First, DativeBase must be
monetizable. Second, DativeBase must be a local-first application. (Third,
Python is not as good as Clojure.)


Components
================================================================================

- common: Common code between components: specs, OpenAPI schemata, etc.
- server: HTTP OpenAPI JSON service
  - One set of users managing multiple OLDs, each containing forms.
  - Monetization built in: plans cover the costs of OLDs. Plans have free,
    subscriber, and supporter tiers. Users manage plans.
- client: HTTP client conveniences for interacting with server. Can be required
  by desktop, synchronizer, gui, etc.
- gui: Dative ReFrame SPA
  - Uses the API to provide user-friendly access to a user's OLDs.
  - Uses the API to allow manager users to manage OLD plans.
- TODO: desktop: DativeTop: Desktop-native, or Electron-like, desktop app that
  interacts with local OLDs and allows synchronization.
  - Similar experience to Dative, but as a native app built on JVM CLJ-F
    (https://github.com/cljfx/cljfx), ClojureDart, Electron with ClojureScript,
    or other.
- TODO: synchronizer: library for synchronizaing follower OLDs with leaders. Can
  be used by desktop.
- TODO: morphoparser: separate, queue-based service for morphological parser
  compilation, parsing, serving, etc.


Proof-of-concept Feature Brief for Read-only Offline Functionality
================================================================================

Proof-of-concept feature brief::

  Given DativeTopCLJ running on a local machine
    And OLDCLJ running as a service on a local machine
    And an OLD data set that is synced across DativeTopCLJ and OLDCLJ
  When the user disconnects their wifi
  Then the user can still read their OLD data set in DativeTopCLJ


Local Development
================================================================================

Follow these detailed steps to get the server (API) running locally and to
confirm that it is working as expected.

Construct the OpenAPI YAML from the OpenAPI EDN source and validate it::

  $ make openapi
  $ make lint-openapi
  No results with a severity of 'error' found!

The first command generates the OpenAPI YAML specification file
``resources/public/openapi/api.yaml`` from the Clojure source of truth at
``dvb.server.http.openapi.spec/api``. The second command lints the YAML file using
the spectral library.

Start the PostgreSQL database in a container and create the tables::

  $ docker compose up -d --build

Run the tests (optional)::

  $ make tests

Connect to the database via PSQL (optional)::

  $ make db

The default configuration for the application is in ``dev-config.edn``.

The recommended way to run the server code while developing is from a
Clojure-integrated REPL, e.g., Emacs with Cider. See the expressions in the
comment block of ``dvb.server.repl``. Executing the following expression in that
code block will restart the system after reloading any code changes::

  => (component.repl/reset)
  :ok

To serve the application from the command line (i.e., a fresh Java process) with
the default config, the following are equivalent::

  $ make run
	$ clj -X:run

No matter how the app was started up, you may access the API at
``http://localhost:8080`` and the Swagger UI at
``http://localhost:8080/swagger-ui/dist/index.html``.

To serve the application with a different configuration file::

  $ clj -X:run :config-path '"/path/to/other/config.edn"'


Creating a User and Authenticating to the API
--------------------------------------------------------------------------------

Create a user with a specified email and password (optional)::

  $ clj -X:init :password abc :email '"abc@bmail.com"'
  {:user
   {:id #uuid "9af83804-2354-4884-8600-f4699794a468",
    :first_name "Anne",
    :last_name "Boleyn",
    :email "abc@bmail.com",
    :password "HASH"})}

We can also create a new user from the REPL. In the ``dvb.server.repl`` ns,
search for ``Create a new user, so we can login`` and define a ``user`` while
creating it in the database, as shown there.

FOX

Current issue: we cannot authenticate API requests because we cannot yet create
a user and an API key (machine user). See above.

The following log message is emitted when we attempt an API call with an app ID
that is not valid, i.e., does not exist in the DB::

  Unable to locate the referenced machine-user.
  {:x-app-id "7ffb9182-f7f9-4a32-a931-0e9ad303e830"}

This happens when the app ID is not a valid UUID string::

  Exception thrown when attempting to query machine user based on X-APP-ID
  {:x-app-id "def"}

This happens when one has not provided X-API-KEY (or X-APP-ID) in the request,
i.e., has not "authorized" in the SwaggerUI interface::

  A required API key value was not provided in the request.
  {:name "X-API-KEY", :in :header}


Local SwaggerUI
================================================================================

If you have DativeBase running locally, you can interact with its HTTP API via
the SwaggerUI at http://localhost:8080/swagger-ui/dist/index.html.

First, you must ensure that you have a valid user in the database and that you
have identified an API key and ID for that server.


Docker
================================================================================

Build a docker image for DativeBase::

  $ docker build -t dativebase .

Run DativeBase in a docker container::

  $ docker run -it --rm --name my-running-dativebase dativebase

Note that the last command above currently fails because the DativeBase server is
unable to make a connection to PostgreSQL at ``localhost:5432``. TODO


The Online Linguistic Database (OLD)
================================================================================

The code under ``src/dvb/server`` corresponds to the Online Linguistic Database
(OLD) of the original Python Dative system.

A major sub-component of the server is an HTTP REST API that conforms to the
OpenAPI spec.

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


Database Migrations
================================================================================

To create a database migration, first create a new migration file under
``migrator/sql`` with::

  $ ./scripts/create-migration.sh replace_me_with_migration_name

Then rebuild the docker images and bring up the containers in order to trigger
the Flyway container ``migrator`` into creating the database schema in the
``postgres`` container::

  $ docker compose up -d --build --force-recreate

Verify that the migrator exited successfully, with either of the following::

  $ docker compose logs -f migrator
  $ docker compose ps

Finally, write the schema to ``schema.sql`` so that the revised schema (post
migration application) can be checked into version control::

  $ make schema.sql

If the above works, you should see changes in the ``schema.sql`` file that
reflect your migration.
