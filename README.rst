================================================================================
  DativeBase in Clojure
================================================================================

DativeBase is a complete rewrite of Dative/OLD in Clojure.

Dative is already 1/3 rewritten in ClojureScript. See DativeReFrame. That project
will become a submodule of this one.

The motivation behind this rewrite is twofold. First, DativeBase must be
monetizable. Second, DativeBase must be a local-first application. (Third,
Python is not as good as Clojure.)

Components:

- OLD = OLDCLJ
  - Rewrite of OLD-Pyramid in Clojure
    - OpenAPI, Ring, Reitit
    - MySQL / SQLite (/ PG?), HugSQL, migrations!
  - One set of users, multiple OLDs
  - Plans cover the costs of OLDs. Plans have free, subscriber, and supporter
    tiers.
- Dative = DativeRF
- DativeTop = DativeTopCLJ
  - CLJ-FX interface?
  - Same logic as Dative, but as a native app built on JVM CLJ-FX ...
    - Or using that new ClojureDart thing ...
- OLD Synchronizer
- Morphological Parser Service ...

Proof-of-concept feature brief::

  Given DativeTopCLJ running on a local machine
    And OLDCLJ running as a service on a local machine
    And an OLD data set that is synced across DativeTopCLJ and OLDCLJ
  When the user disconnects their wifi
  Then the user can still read their OLD data set in DativeTopCLJ
