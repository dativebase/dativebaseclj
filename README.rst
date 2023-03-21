================================================================================
  DativeBase in Clojure
================================================================================

A complete rewrite of Dative/OLD in Clojure.

Dative is already 1/3 rewritten in ClojureScript. See DativeReFrame. That project
will become a submodule of this one.

The development strategy is to make this a local-first application.

Components:

- OLD = OLDCLJ
  - Rewrite of OLD-Pyramid in Clojure
    - OpenAPI, Ring, Reitit
    - MySQL / SQLite (/ PG?), HugSQL, migrations!
  - One set of users, multiple OLDs
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
