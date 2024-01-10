================================================================================
  Edges
================================================================================

Edges means boundaries. We want all entities to be in a Clojure-idiomatic
format when handled in Clojure code. Other media require other formats. For
example, PostgreSQL has different conventions than Clojure and the REST API has
different conventions still.

The edges we care about are CLJ <=> PG and CLJ <=> API.


Clojure Conventions
================================================================================

- Use keywords for enum values, e.g., status.
- All keys are kebab-case keywords with punctuation possible.
- Dates and timestamps are Java time objects.
- UUIDs are UUID objects.


API Conventions
================================================================================

- Use strings for enum values.
- Keys are snake_case strings without punctuation.
- Dates and timestamps are ISO-formatted strings.
- UUIDs are strings.


PostgreSQL Conventions
================================================================================

- Use strings for enum values.
- Keys are snake_case strings without punctuation.
- Dates and timestamps are JDBC objects.
- UUIDs are UUID objects (I believe).
