================================================================================
  DativeBase Runbook
================================================================================

Created: 2024-06-05.
Last updated: 2024-06-08.

This document describes how to maintain the production DativeBase deployment on
the INT server with IP 132.229.188.226.

By "DativeBase", I mean (as of 2024) the Dative frontend UI app, the OLD REST
API service, the Dative and OLD web sites, all supporting services (Nginx,
MySQL, and docker), external domain name servers (DNSs), and logging
functionality. This term here also covers other apps and services (i.e.,
rewrites) that may arise in the DativeBase ecosystem.

This document should be the point of entry for all those who need to figure out
"Why isn't Dative working?"


Table of Contents
================================================================================

- `Production Deployment Summary`_
- `Architectural Overview of Production DativeBase`_
- `Guided Walkthrough`_
- `How to Create a New OLD`_
- `Configuration of MySQL`_
- `Installation of OLD HTTP API`_
- `Installation of Dative SPA`_
- `IP Address`_
- `Domain Name Configuration`_
- `Firewall`_


Production Deployment Summary
================================================================================

DativeBase is served on the "INT server". INT is the Instituut voor de
Nederlandse Taal. INT and CIPL are institutional sponsors that are supporting
the deployment and maintenance of this DativeBase instance. CIPL is the Comité
International Permanent des Linguistes.

- IP Address: 132.229.188.226
- URL: https://dative.test.ivdnt.org

  - 2024-06-07: this is serving the Dative SPA at path /
  - 2024-06-08: this is serving the OLDs under path /olds

Processes & apps:

- System:          Alma Linux 8.7
- Dative app:      CoffeeScript SPA
- OLD service:     Pyramid Python RESTful web service (multiple instances in containers)
- MySQL server:    MariaDB 10.11
- Nginx:           Nginx 1.14; Web server

Processes & apps that still need to be deployed on the INT server:

- Dative app v2:   ClojureScript SPA
- Dative web site: Static HTML
- OLD web site:    Static HTML
- DativeBaseCLJ:   WIP monorepo containing a Clojure/ClojureScript rewrite of Dative and OLD
- PostgreSQL:      RDBMS
- RabbitMQ:        Message broker

URLs of Services that are still running on Joel's DigitalOcean server:

- Dative app: https://app.dative.ca
- Dative app v2: https://v2.dative.ca
- Dative web site: https://www.dative.ca
- OLD service: https://app.onlinelinguisticdatabase.org
- OLD web site: https://www.onlinelinguisticdatabase.org


Architectural Overview of Production DativeBase
================================================================================

DativeBase has a frontend, Dative, and a backend, the OLD, i.e., the Online
Linguistic Database.

Dative is a SPA, a single-page application. This means that it runs in the
browser and makes extensive use of JavaScript. As software, Dative is no longer
feasibly alterable: it is written in the now-arcane CoffeeScript and suffers
from JavaScript dependency hell breakdown. It is, however, relatively easy to
deploy Dative: simply serve its static and pre-compiled assets (HTML,
JavaScript, images, etc.) from a web server (Nginx).

The OLD is an HTTP API following REST-ish practices. It exposes resources like
"forms" and "tags" via a consistent URL pattern and usage of the HTTP methods
GET, POST, PUT, and DELETE for read, create, update, and destroy, respectively.
It is written in Python, in the Pyramid framework. It writes data to MySQL and
to the file system. It expects that foma (an FST toolkit) and MITLM (a language
model toolkit) are installed on the host. It is therefore easier to deploy on
docker on pre-built containers that have these difficult-to-install dependencies
pre-installed.

As the above implies, we need Nginx installed, running and properly configured
in order to serve Dative and the OLD. We also need a MySQL server instance
running, DNS configured correctly, SSL certs and Docker. Most of this was
already done for us on the INT server by the INT staff.


Guided Walkthrough
================================================================================

Connect to the VPN in order to enable SSH access. Open the ``OpenVPN Connect``
app, select the sole profile ``JDdative``, enter the private key password, and
click "Connect".

If you are not Joel Dunham, then you will first need to install a VPN client
like OpenVPN Connect and then you will need to request that INT technical
support provide you with credentials and instructions for connecting via SSH.

Access the CIPL server via SSH::

  $ ssh cipl

The above is an SSH alias for::

  sudo ssh -i /path/to/ssh/key joel@<<PRIVATE-IP>>

Note that the above will prompt you for the SSH key's passphrase.

View the Linux Alma Linux v. 8.9 details::

  $ cat /etc/redhat-release
  AlmaLinux release 8.9 (Midnight Oncilla)

Dative SPA source location::

  /home/joel/apps/dative

Dative release static files location::

  /nginx/dative

Check Nginx status::

  $ sudo systemctl status nginx

Check Nginx config validity::

  $ sudo nginx -t

Tail all the logs: Nginx generally, Nginx logs for requests to Dative SPA, and
OLD API service logs::

  $ sudo make log

See ``/home/joel/Makefile`` for details on where the logs are stored.


How to Create a New OLD
================================================================================

Creating a new OLD is currently a three-step process:

1. Create the MySQL database
2. Initialize: create the db tables and directory structure
3. Tell Dative about the OLD

Create the MySQL database::

  mysql -u admin -p
  MariaDB [(none)]> CREATE DATABASE <<OLD_NAME>> DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;

Initialize the OLD: have the OLD create the empty tables and directory
structure::

  $ docker exec -it old bash
  $ /venv/bin/initialize_old config.ini <<OLD_NAME>>

Tell Dative about the OLD::

  $ cat /nginx/dative/servers.json
  [..., {...},
   {"corpusServerURL": null,
    "name": "<<OLD_HUMAN_READABLE_NAME>>",
    "serverCode": null,
    "type": "OLD",
    "url": "https://app.onlinelinguisticdatabase.org/<<OLD_NAME>>",
    "website": "http://www.onlinelinguisticdatabase.org"}]

Finally, verify via Dative (at https://dative.test.ivdnt.org/) that you can
interact with the new OLD as expected. You may also want to set up some initial
users for the OLD.


Configuration of MySQL
================================================================================

This section explains how I configured MySQL on the INT server. The INT folks
had already installed MariaDB v. 10.11 on the server.

Check MySQL status::

  $ sudo systemctl status mariadb

I secured MySQL as follows. See
https://shape.host/resources/how-to-set-up-mariadb-on-almalinux-9::

  $ sudo mysql

Change the root user's password::

  > ALTER USER 'root'@'localhost' IDENTIFIED BY '<<REDACTED>>';

Remove anonymous users from the system by running the following command::

  > DELETE FROM mysql.user WHERE User='';

Restart MySQL::

  $ sudo systemctl restart mariadb

Create a new admin user with root privileges and password-based access::

  $ sudo mysql -u root -p
  > GRANT ALL ON *.* TO 'admin'@'localhost' IDENTIFIED BY '<<REDACTED>>' WITH GRANT OPTION;
  > FLUSH PRIVILEGES;

Create the old MySQL user that the OLD-Pyramid app will use. Principle of least
privilege::

  $ mysql -u admin -p
  > GRANT SELECT, INSERT, UPDATE, DELETE, CREATE ON *.* TO 'old'@'localhost' IDENTIFIED BY '<<REDACTED>>';
  > FLUSH PRIVILEGES;
  > SELECT user, authentication_string, plugin, host FROM mysql.user;
  > SHOW GRANTS FOR 'old'@'localhost';


Installation of OLD HTTP API
================================================================================

This section explains how I installed and configured the OLD HTTP API on the INT
server.

The legacy OLD (Online Linguistic Database) is a REST API written in Python,
using the Pyramid framework and MySQL. It is standardly deployed on Docker
containers because its OS dependencies are old and difficult to install on an
arbitrary Linux machine. Its source code can be found on GitHub at
https://github.com/dativebase/old-pyramid.

For Docker configuration on Alma Linux, I consulted
https://www.liquidweb.com/kb/install-docker-on-linux-almalinux/.

Initially, I added user joel to the docker group::

  $ id joel
  uid=1001(joel) gid=1001(joel) groups=1001(joel),10(wheel)
  $ sudo usermod -aG docker joel
  $ id joel
  uid=1001(joel) gid=1001(joel) groups=1001(joel),10(wheel),990(docker)

I tried the above in an attempt to run docker as a non-root user. However, the
above was insufficient. I then tried to install podman as an alternative to
Docker (following https://www.howtoforge.com/beginner-guide-to-install-and-use-podman-on-almalinux-9/)
but that failed. I suspect docker and podman have conflicting dependencies and
one can only easily install one or the other. I therefore decided to move
forward with using Docker with sudo.

Pull the OLD image from dockerhub::

  $ sudo docker pull jrwdunham/old-pyramid:<DOCKERTAG>
  $ sudo docker pull jrwdunham/old-pyramid:4d9089186d5f0bcff3e5a57ba0c28980c50718a7

The OLD data live under ``/home/joel/oldsdata/``::

  $ pwd
  /home/joel
  $ mkdir oldsdata

We are running the OLD in a container and connecting to a MySQL server instance
that is running on the host. I followed the advice at
https://stackoverflow.com/questions/24319662/from-inside-of-a-docker-container-how-do-i-connect-to-the-localhost-of-the-mach.

**WARNING: Don't use the following to re-deploy the OLD.** Instead use ``sudo
./deployold.sh``. See below.

The following runs the OLD container on the host network so that we can access
MySQL running on the host using host:port 127.0.0.1:3306. It also allows us to
access the OLD instance directories at ``~/oldsdata``::

  $ sudo docker run \
      -d \
      -v "/home/joel/oldsdata:/usr/src/old/store" \
      --network=host \
      --name old \
      --env OLD_DB_PASSWORD="<<REDACTED>>" \
      --env OLD_DB_HOST="127.0.0.1" \
      jrwdunham/old-pyramid:fix-readonly-local-config

**WARNING: the above is a different Docker image than the one I downloaded in a
previous command.**

Confirm that we can access MySQL from within the OLD container::

  $ sudo docker exec -it old bash
  $ mysql -h 127.0.0.1 -u admin -p
  $ mysql -h 127.0.0.1 -u old -p

Create a ``demo`` OLD database::

  $ mysql -u admin -p
  > CREATE DATABASE demo DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;

Initialize (create tables) for the ``demo`` OLD database from within the
container::

  $ sudo docker exec -it old bash
  $ /venv/bin/initialize_old config.ini demo
  2024-06-08 17:49:12,950 INFO Initializing OLD "demo".
  ...
  2024-06-08 17:49:15,919 INFO OLD "demo" successfully set up.

This is the Nginx configuration for both Dative and the OLD as of 2024-06-08. See
``/etc/nginx/sites-available/dative``::

  upstream old {
    server 127.0.0.1:8000;
    server 127.0.0.1:8002;
  }
  server {
    server_name app-cipl.dative.ca;
    root /nginx/dative;
    index index.html;
    autoindex on;
    access_log /var/log/nginx/dative/access.log;
    error_log /var/log/nginx/dative/error.log;
    location / {
      try_files $uri $uri/ =404;
      # Wide open CORS config for OPTIONS and GET
      if ($request_method = 'OPTIONS') {
        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Credentials' 'true';
        add_header 'Access-Control-Allow-Methods' 'GET, OPTIONS';
        add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type';
        add_header 'Access-Control-Max-Age' 1728000;
        add_header 'Content-Type' 'text/plain charset=UTF-8';
        add_header 'Content-Length' 0;
        return 204;
      }
      if ($request_method = 'GET') {
        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Credentials' 'true';
        add_header 'Access-Control-Allow-Methods' 'GET, OPTIONS';
        add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type';
      }
    }
    # Proxy requests under path /olds to OLD Pyramid Python server
    location /olds {
      proxy_set_header        Host $http_host;
      proxy_set_header        X-Real-IP $remote_addr;
      proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header        X-Forwarded-Proto $scheme;
      client_max_body_size    1000m;
      client_body_buffer_size 128k;
      proxy_connect_timeout   60s;
      proxy_send_timeout      90s;
      proxy_read_timeout      90s;
      proxy_buffering         off;
      proxy_buffer_size       128k;
      proxy_buffers           4 256k;
      proxy_busy_buffers_size 256k;
      proxy_temp_file_write_size 256k;
      proxy_redirect          off;
      proxy_pass_request_headers      on;
      proxy_pass              http://old/;
    }
  }

The OLD can be deployed idempotently with the following::

  $ sudo ./deployold.sh
  Deploying two instances of the OLD (from Docker tag fix-readonly-local-config) on ports 8000 and 8002
  ...
  $ sudo docker ps
  CONTAINER ID   IMAGE                                             COMMAND                  CREATED         STATUS         PORTS     NAMES
  8e4482405df1   jrwdunham/old-pyramid:fix-readonly-local-config   "/venv/bin/pserve --…"   4 seconds ago   Up 3 seconds             old
  20c9362fc831   jrwdunham/old-pyramid:fix-readonly-local-config   "/venv/bin/pserve --…"   4 seconds ago   Up 3 seconds             old2

Note that SELinux adds an extra challenge here. I had to set the following
configuration in order to get around a permission denied issue when using Nginx
to proxy requests to the OLD instances.::

  $ sudo setsebool -P httpd_can_network_connect 1

With the above and Nginx reloaded, the following URL returned JSON and hit the
OLD container, as expected:

  https://dative.test.ivdnt.org/olds/demo/login/authenticate

View the logs of the OLD instances::

  $ sudo docker logs --tail 500 -f old
  $ sudo docker logs --tail 500 -f old2

Alternatively, to tail all OLD logs::

  $ sudo make log-old

Alternatively, to tail all logs (Nginx, Dative, OLDs)::

  $ sudo make log


Installation of Dative SPA
================================================================================

The Dative UI is just static HTML and JavaScript. The static content being
served is located at ``/nginx/dative``.

This was simply copied from ``home/joel/apps/dative/releases/dist/`` after
cloning the source::

  $ pwd
  /home/joel/apps
  $ git clone https://github.com/dativebase/dative.git
  $ cd dative/releases
  $ tar -xvf release-315b7d9a8e2106612639caf13189eb2de8586278.tar.gz
  $ cp -r dist /nginx/dative2

The Nginx configuration for Dative is at ``/etc/nginx/sites-available/dative``.

The global Nginx configuration at ``/etc/nginx/nginx.conf`` runs with user
``nginx``. I therefore transferred the ownership of the Dative source and assets
to this user and set the SELinux context type to ``httpd_sys_content``, as
needed::

  $ sudo chown -R nginx:nginx /nginx
  $ sudo chmod -R 755 /nginx
  $ sudo chcon -R -t httpd_sys_content_t /nginx

The primary unexpected obstacle here (as indicated by the last line above) was
that the INT is running SELinux, which is Security-Enhanced Linux. The following
Stackoverflow post finally helped me to understand this:
https://stackoverflow.com/questions/31729212/nginx-root-index-html-forbidden-13-permission-denied

The ``chcon`` command is used to change the SELinux security context of a file.

We can see the security contexts of the Nginx static root directory currently
being used::

  $ ls -lZ /
  drwxr-xr-x. 3 nginx nginx unconfined_u:object_r:default_t:s0   20 Jun  8 15:30 nginx

  $ ls -lZ /nginx
  drwxr-xr-x. 8 nginx nginx unconfined_u:object_r:default_t:s0 4096 Jun  8 15:29 dative

As shown, both the ``/nginx`` and ``/nginx/dative`` directories have the
following SELinux context, which is not correct for static HTTP content::

  user:role:type:range
  unconfined_u:object_r:default_t:s0

The following command should recursively set ``/nginx`` to have
``httpd_sys_content_t`` SELinux type. Documentation indicates::

  Use this type for static web content, such as .html files used by a static
  website. Files labeled with this type are accessible (read only) to httpd and
  scripts executed by httpd. By default, files and directories labeled with this
  type cannot be written to or modified by httpd or other processes. Note that
  by default, files created in or copied into /var/www/html/ are labeled with
  the httpd_sys_content_t type.
  - See https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/6/html/managing_confined_services/sect-managing_confined_services-the_apache_http_server-types

Command run::

  $ sudo chcon -R -t httpd_sys_content_t /nginx

The following changed the SELinux type in the context to ``httpd_sys_content``,
as expected::

  $ ls -lZ /nginx
  drwxr-xr-x. 8 nginx nginx unconfined_u:object_r:httpd_sys_content_t:s0 4096 Jun  8 15:29 dative

It also allowed me to serve Dative at https://dative.test.ivdnt.org.

See this tutorial on chcon in SELinux:
https://www.thegeekstuff.com/2017/07/chcon-command-examples/.


IP Address
================================================================================

The public IP address of the INT server is ``132.229.188.226``. The INT
technical support team handles DNS configuration and SSL certificate maintenance
and configuration for the ``ivdnt.org`` domain. They configure the subcomain
``dative.test.ivdnt.org`` to resolve to the public IP and they route traffic to
port 80 on the server. This means that operators of the INT server at our level
can configure Nginx to listen on port 80 and do not need to worry about SSL
certificate configuration.

Note that the above is the public address of the firewall that does the security
for the server.

The private IP address is different. The private IP address is used for VPN
access and low-level management access.

There is also a distinct perimeter IP Address. The perimeter address is
used by the proxy/firewall to forward the filtered traffic that is directed to
the external address ``132.229.188.226``.

For example::

  dative.test.ivdnt.org -> 132.229.188.226:443 -> PERIMETER_IP_ADDRESS:80

Check the IP interfaces on the server::

  $ ip address

Alternatively::

  $ ifconfig


Domain Name Configuration
================================================================================

The subdomain ``dative.test.ivdnt.org`` resolves to the INT server under scheme
``https``.

INT controls DNS configuration for ``dative.test.ivdnt.org`` As indicated
elsewhere in this document, ``dative.test.ivdnt.org`` resolves to public IP
``132.229.188.226:443`` which is routed to the perimeter IP of the INT server,
which in turn routes traffic to the INT server's private IP. The end result is
that our Nginx server receives requests to ``https://dative.test.ivdnt.org`` on
port 80.

As of 2024-06-06, https://dative.test.ivdnt.org/ is successfully routing
requests to Nginx on the INT server. I believe that my alterations to the
firewall made this possible as of 2024-06-05 because before those alterations I
was seeing no evidence of HTTP requests to that URL reaching the machine. See
the `Firewall`_ section below.

(Note that subdomain ``app-cipl.dative.ca`` is also being routed to the INT
server. Joel configured the DNS for this himself, using the DigitalOcean
nameserver UI. The relevant A record in DigitalOcean configures
``app-cipl.dative.ca`` to resolve directly to the private IP of the INT server.
This is not really helpful. I did this out of frustration with the firewall
configuration that was previously blocking requests to the ``ivdnt.org``
subdomain from reaching the INT server.)

Ultimately, we will need the DNS configuration for ``dative.ca`` to be moved to
an INT-controlled nameserver. See ticket
https://github.com/dativebase/dativebaseclj/issues/17.


Firewall
================================================================================

The INT AlmaLinux server uses ``firewall-cmd`` to control access. See:

- https://linuxconfig.org/how-to-open-http-port-80-on-redhat-7-linux-using-firewall-cmd
- https://linuxconfig.org/introduction-to-firewalld-and-firewall-cmd-command-on-linux

I had to run the following to expose port 80 over TCP::

  $ sudo firewall-cmd --zone=public --add-port=80/tcp --permanent
  $ sudo firewall-cmd --reload

It was only after running the above that Nginx on the INT server started to
receive traffic from ``dative.test.ivdnt.org``.
