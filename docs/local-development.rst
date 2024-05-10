================================================================================
  Local Development
================================================================================

URL for local development: http://localhost:63000.


Initialization
================================================================================

To initialize a local development environment, see ``dvb.client.init-local``.


End-to-end Local Development
================================================================================

Because DativeBase is composed of a Clojure API and an independent ClojureScript
UI, and because the local deployments run on separate ports, the browser will
give us CORS errors when we try to issue requests from the UI to the server.
Therefore, we need to run a local Nginx server. We do this with Nginx in a
docker container.

Just running::

  $ docker compose up -d

should do this automatically::

  $ docker compose ps
  NAME                      IMAGE                   COMMAND                  SERVICE    CREATED             STATUS             PORTS
  dativebaseclj-nginx-1      nginx:stable-alpine     "/docker-entrypoint.…"   nginx      About an hour ago   Up About an hour   0.0.0.0:61000->80/tcp, 0.0.0.0:61001->8000/tcp
  dativebaseclj-postgres-1   dativebaseclj-postgres   "docker-entrypoint.s…"   postgres   46 hours ago        Up 46 hours        127.0.0.1:5432->5432/tcp

The Nginx configuration which is pulled into the Nginx Docker container is in ``etc/``::

  etc
  └── nginx
      ├── conf.d
      │   ├── default.conf
      │   ├── dativebase.conf
      └── nginx.conf

The ``dativebase.conf`` Nginx config file proxies requests beginning with ``/api/`` to
the upstream ``host_api``, which resolves to ``host.docker.internal:8080`` in
the container, which is ``localhost:8080`` on the host. (This works on Mac OS,
but I have not tested it on Linux.) Port ``8080`` is where the local API is
running by default. See ``dvb.server.repl``.

Other requests (i.e., to the UI) are proxied to ``host_ui``, which resolves to
``localhost:8280`` on the host. This is where the local UI is running by
default; see ``shadow-cljs.edn``.

For the mapping from proxy names like host_api to domain:port, see
``etc/nginx/nginx.conf``.
