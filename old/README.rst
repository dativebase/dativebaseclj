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

Click the "Authorize" button and enter the API key "dativeold".

Now click "GET /api/v1/forms", then "Try it out", then "Execute". The Swagger UI
will make a request to the OLD and will receive a mock response.
