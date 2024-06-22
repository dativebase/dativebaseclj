================================================================================
  DativeBase Runbook
================================================================================

Created: 2024-06-05.
Last updated: 2024-06-21.

This document describes how to maintain the production DativeBase deployment on
the INT server with IP 132.229.188.226.

By "DativeBase", I mean (as of 2024) the Dative frontend UI app, the OLD REST
API service, the Dative and OLD web sites, all supporting services (Nginx,
MySQL, and docker), external domain name servers (DNSs), and logging
functionality. This term here also covers other apps and services (i.e.,
rewrites) that may yet arise in the DativeBase ecosystem.

This document should be the point of entry for all those who need to answer
questions like "How is Dative served?" or "Why isn't Dative working?"


Table of Contents
================================================================================

- `Production Deployment Summary`_
- `Architectural Overview of Production DativeBase`_
- `Guided Walkthrough`_
- `How to Create a New OLD`_
- `Configuration of MySQL`_
- `Installation of OLD HTTP API`_
- `Installation of Dative SPA`_
- `Installation of DativeBase`_
- `IP Address`_
- `Domain Name Configuration`_
- `Firewall`_
- `Mounted Drives`_
- `Serving the Dative Website`_
- `Serving the OLD Website`_
- `Plan to Complete the Migration of the OLDs from DO to INT`_


Production Deployment Summary
================================================================================

DativeBase is served on the "INT server". INT is the Instituut voor de
Nederlandse Taal. INT and CIPL are institutional sponsors that are supporting
the deployment and maintenance of this DativeBase instance. CIPL is the Comité
International Permanent des Linguistes.

- IP Address: 132.229.188.226
- URL: https://dative.test.ivdnt.org

  - 2024-06-07: This is serving the Dative SPA at path /.
  - 2024-06-08: This is serving the OLDs under path /olds/.
  - 2024-06-08: This is serving the Dative website under path /dative-website.
  - 2024-06-09: This is serving the OLD website under path /old-website.
  - 2024-06-15: All OLD data has been transferred to INT, but DO is still live
    so this data is stale.

Machine Specs:

- CPUs:            4
- RAM:             8G
- disk 1 root OS:  16G at /
- disk 2 data:     160G at /vol1
- Backups:         weekly full backup

Software installed:

- Linux OS:        AlmaLinux 8.9 (SELinux)
- Nginx:           1.14
- Docker:          23
- Mysql (MariaDB): 10.11
- PostgreSQL:      16.3

Processes, sites & apps:

- Dative app:      CoffeeScript (JavaScript) SPA
- OLD service:     Pyramid Python RESTful web service (multiple instances in containers)
- DativeBaseCLJ:   WIP monorepo containing a Clojure/ClojureScript rewrite of Dative and OLD
- Dative website:  Static HTML
- OLD website:     Static HTML

Processes & apps that still need to be deployed on the INT server:

- Dative app v2:   ClojureScript SPA
- RabbitMQ:        Message broker

URLs of Services that are still running on Joel's DigitalOcean server:

- Dative app:      https://app.dative.ca
- Dative app v2:   https://v2.dative.ca
- Dative web site: https://www.dative.ca
- OLD service:     https://app.onlinelinguisticdatabase.org
- OLD web site:    https://www.onlinelinguisticdatabase.org


Architectural Overview of Production DativeBase
================================================================================

DativeBase has a frontend, Dative, and a backend, the OLD, i.e., the Online
Linguistic Database.

Dative is a SPA, a single-page application. This means that it runs in the
browser and makes extensive use of JavaScript. As software, Dative is no longer
feasibly alterable: it is written in the now-arcane CoffeeScript language and
suffers from bit rot and JavaScript dependency hell breakdown. It is, however,
relatively easy to deploy Dative: simply serve its static and pre-compiled
assets (HTML, JavaScript, images, etc.) from a web server (Nginx).

The OLD is an HTTP API following REST-like practices. It exposes resources such
as "forms" and "tags" via a consistent URL pattern and usage of the HTTP methods
GET, POST, PUT, and DELETE for read, create, update, and destroy, respectively.
It is written in Python, in the Pyramid framework. It writes data to MySQL and
to the file system. It expects that foma (an FST toolkit) and MITLM (a language
model toolkit) are installed on the host. It is therefore easier to deploy on
docker on pre-built containers that have these difficult-to-install dependencies
pre-installed.

As the above implies, we need Nginx installed, running and properly configured
in order to serve Dative and the OLD. We also need a MySQL server instance
running, DNS configured correctly, SSL certs, and Docker. Most of this was
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

  $ mysql -u admin -p
  > CREATE DATABASE <<OLD_NAME>> DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;

Initialize the OLD, i.e., get the OLD to create the needed empty tables and
directory structure for the new OLD::

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

Check if the OLD is running::

  $ sudo docker ps

If running, the above should indicate two running OLD containers: ``old`` and
``old2``. To ensure that the OLD service is running, run the deploy script::

  $ ./deployold.sh

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
but I ran into obstacles there. I suspect docker and podman have conflicting
dependencies and one can only easily install one or the other. I therefore
decided to move forward with using Docker with sudo.

Pull the OLD image from dockerhub. You probably should use the sugested 4d90
tage suggested below::

  $ sudo docker pull jrwdunham/old-pyramid:<DOCKERTAG>
  $ sudo docker pull jrwdunham/old-pyramid:4d9089186d5f0bcff3e5a57ba0c28980c50718a7

The OLD data live under ``/vol1/dative/oldsdata/``::

  $ mkdir /vol1/dative/oldsdata

(Note: I had previously configured this to be ``/home/joel/oldsdata/`` but
switched to the ``/vol1/`` sub-path because that is where the disk space is.)

We are running the OLD in a container and connecting to a MySQL server instance
that is running on the host. I followed the advice at
https://stackoverflow.com/questions/24319662/from-inside-of-a-docker-container-how-do-i-connect-to-the-localhost-of-the-mach.

**WARNING: Don't use the following to re-deploy the OLD.** Instead use ``sudo
./deployold.sh``. See below.

The following runs the OLD container on the host network so that we can access
MySQL running on the host using host:port 127.0.0.1:3306. It also allows us to
access the OLD instance directories at ``/vol1/dative/oldsdata``::

  $ sudo docker run \
      -d \
      -v "/vol1/dative/oldsdata:/usr/src/old/store" \
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

The INT Nginx configuration for both Dative and the OLD as of 2024-06-15 is
``/etc/nginx/sites-available/dative.test.ivdnt.org``. (For a local copy of its
contents, see ``dativebaseclj/docs/etc/nginx/dative.test.ivdnt.org``.) We also
have a similar Nginx config file ready to go for ``app.dative.ca``; see
``/etc/nginx/sites-available/app.dative.ca``.

To enable the available Nginx config files::

  sudo ln -s /etc/nginx/sites-available/dative.test.ivdnt.org /etc/nginx/sites-enabled/
  sudo ln -s /etc/nginx/sites-available/app.dative.ca /etc/nginx/sites-enabled/

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

Alternatively, to tail all OLD logs (from the joel user's home directory)::

  $ sudo make log-old

Alternatively, to tail all logs (Nginx, Dative, OLDs)::

  $ sudo make log


Installation of Dative SPA
================================================================================

The Dative UI is just static HTML and JavaScript. The static content being
served is located at ``/nginx/dative/``.

This was simply copied from ``home/joel/apps/dative/releases/dist/`` after
cloning the source::

  $ pwd
  /home/joel/apps
  $ git clone https://github.com/dativebase/dative.git
  $ cd dative/releases
  $ tar -xvf release-315b7d9a8e2106612639caf13189eb2de8586278.tar.gz
  $ cp -r dist /nginx/dative

The Nginx configuration for Dative is at
``/etc/nginx/sites-available/dative.test.ivdnt.org``.

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


Installation of DativeBase
================================================================================

Date: 2024-06-21.

Clone the DativeBase source::

  $ pwd
  /home/joel/apps
  $ git clone https://github.com/dativebase/dativebaseclj.git

Install rlwrap::

  $ dnf install rlwrap

Java::

  $ java -version
  openjdk version "1.8.0_402"
  OpenJDK Runtime Environment (build 1.8.0_402-b06)
  OpenJDK 64-Bit Server VM (build 25.402-b06, mixed mode)

Install Clojure::

  $ curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
  $ chmod +x linux-install.sh
  $ sudo ./linux-install.sh

Install the Swagger UI resources::

  $ make install-swagger-ui

I followed the instructions at
https://jumpcloud.com/blog/how-to-install-postgresql-16-rhel-9 in order to
install and configure PostgreSQL 16.

Update packages::

  $ sudo dnf update -y

Install PostgreSQL::

  $ sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm
  $ sudo dnf -qy module disable postgresql
  $ sudo dnf install postgresql16-server -y

Initialize Postgres::

  $ sudo /usr/pgsql-16/bin/postgresql-16-setup initdb

Start and enable the PostgreSQL service to load at boot::

  $ sudo systemctl enable postgresql-16
  $ sudo systemctl start postgresql-16

View status of PostgreSQL::

  $ sudo systemctl status postgresql-16

To restart the database server::

  $ sudo systemctl restart postgresql-16

To reload the database server without stopping the service::

  $ sudo systemctl reload postgresql-16

Set a password::

  $ sudo passwd postgres

View the version::

  $ psql -V
  psql (PostgreSQL) 16.3

Open the PG console::

  $ sudo -u postgres psql
  postgres=#

Set a password for the postgres user::

  postgres=# \password postgres

Access the PostgreSQL access policy configuration file::

  $ sudo vim /var/lib/pgsql/16/data/pg_hba.conf

and set line::

  local  all  all  peer

to::

  local  all  all  md5

The reload PG::

  $ sudo systemctl reload postgresql-16

Now PG prompts for the postgres password.

Show PG users and DBs::

  postgres=# \du
                               List of roles
   Role name |                         Attributes
  -----------+------------------------------------------------------------
   postgres  | Superuser, Create role, Create DB, Replication, Bypass RLS

  postgres=# \l

Create a new DB named dative::

  postgres=# create database dativebase;

Run DativeBase::

  $ pwd
  /home/joel/apps/dativebaseclj
  $ make run

To specify a different config path and run DativeBase::

  $ clj -X:run :config-path '"/home/joel/apps/dativebaseclj/dev-config-SECRET.edn"'


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


Dative.ca Domain Configuration
--------------------------------------------------------------------------------

The following table shows the DNS A-type records currently (2024-06-15)
configured for dative.ca::

    Type Hostname                      Value
    A    app-cipl.dative.ca            INT PRIVATE IP
    A    world-round-up-2024.dative.ca 157.245.232.138 (DO droplet IP)
    A    newapp.dative.ca              144.126.212.39 (DO droplet IP)
    A    v2.dative.ca                  144.126.212.39 (DO droplet IP)
    A    dev.dative.ca                 144.126.212.39 (DO droplet IP)
    A    app.dative.ca                 144.126.212.39 (DO droplet IP)
    A    www.dative.ca                 144.126.212.39 (DO droplet IP)

Desired DNS configuration for dative.ca::

    Type Hostname                      Value
    A    app.dative.ca                 INT PUBLIC IP (Dative app)
    A    www.dative.ca                 INT PUBLIC IP (Dative website)
    A    www.old.dative.ca             INT PUBLIC IP (OLD website)
    A    v2.dative.ca                  INT PUBLIC IP (New DativeBase app)
    A    dev.dative.ca                 INT PUBLIC IP (Staging/Dev env)


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

Display the default zone::

  $ sudo firewall-cmd --get-default-zone
  public

Display the current firewall settings::

  $ sudo firewall-cmd --list-all


Mounted Drives
================================================================================

View mounted drives::

  $ df -aTh
  Filesystem                 Type        Size  Used Avail Use% Mounted on
  /dev/mapper/almalinux-root xfs          14G  7.7G  5.8G  58% /
  /dev/sdb                   ext4        157G   28K  149G   1% /vol1


Serving the Dative Website
================================================================================

The GitHub URL for the source of the Dative website is
https://github.com/dativebase/dative-website.

Clone it::

  $ pwd
  /home/joel/apps
  $ git clone https://github.com/dativebase/dative-website
  $ sudo cp -r dative-website /nginx/dative-website
  $ sudo chown -R nginx:nginx /nginx/dative-website
  $ sudo chmod -R 755 /nginx/dative-website
  $ sudo chcon -R -t httpd_sys_content_t /nginx/dative-website

I had to fix the Dative website source. I had to make some paths relative in the
index.html file

Nginx server location block::

  location ~ ^/dative-website.*$ {
    rewrite ^/dative-website$ /dative-website/ permanent;
    rewrite ^/dative-website/$ /dative-website/index.html;
    rewrite ^/dative-website/(.*)$ /$1 break;
    root /nginx/dative-website;
    index index.html;
    try_files $uri $uri/ =404;
  }

The Dative website is now being served at::

  https://dative.test.ivdnt.org/dative-website/


Serving the OLD Website
================================================================================

The GitHub URL for the source of the OLD website is
https://github.com/dativebase/old-website.

Clone it::

  $ pwd
  /home/joel/apps
  $ git clonoe https://github.com/dativebase/old-website.git
  $ cp -r old-website /nginx/old-website
  $ sudo chown -R nginx:nginx /nginx/old-website
  $ sudo chmod -R 755 /nginx/old-website
  $ sudo chcon -R -t httpd_sys_content_t /nginx/old-website

Nginx server location block::

  # OLD website is served at /old-website(/)
  location ~ ^/old-website.*$ {
    rewrite ^/old-website$ /old-website/ permanent;
    rewrite ^/old-website/$ /old-website/index.html;
    rewrite ^/old-website/(.*)$ /$1 break;
    root /nginx/old-website;
    index index.html;
    try_files $uri $uri/ =404;
  }

The OLD website is now being served at::

  https://dative.test.ivdnt.org/old-website/


Plan to Complete the Migration of the OLDs from DO to INT
================================================================================

The following steps constitute the current plan to complete the migration of
Dative from Digital Ocean to the INT server.

- DONE. Document the migration status and completion plan in the runbook.
- DONE. Draft an email to send to users of Dative.
- DONE. Ensure we have Nginx config ready for dative.ca on INT.
- DONE. Share the migration completion plan with technical stakeholders for feeback.
- DONE. Set a date and time for the migration. Suggestion: June 28, 2024.
- DONE. Get feedback on the email and plan from internal stakeholders.
- DONE. Confirm that the migration transfers the data correctly.
- DONE. Ensure we have the commands ready to shut down DO Dative and OLDs.
- TODO. Send the migration notification email to the Dative users.
- TODO. Wait to see if any users want to opt out.
- TODO. Shut down DO Dative & its OLDs.
- TODO. Run the final data migration from DO to OLD.
- TODO. With the help of INT staff, configure dative.ca subdomains to resolve to INT
  server.
  - app.dative.ca => INT server 132.229.188.226 (Dative app & OLD API)
  - www.dative.ca => INT server 132.229.188.226 (Dative website)
  - www.old.dative.ca => INT server 132.229.188.226 (OLD website)
  - onlinelinguisticdatabase.org is no longer needed.
- TODO. Confirm that production INT Dative is operating correctly.
- TODO. Email users to notify that Dative has been restored.
- TODO. Shut down the Digital Ocean server.
- TODO. Shut down the onlinelinguisticdatabase.org domain.


How to Shut Down DO Dative During the Migration
--------------------------------------------------------------------------------

First, update the Dative Nginx config to serve static HTML indicating the
temporary downtime::

  $ sudo vim /etc/nginx/sites-available/dative.ca

Both ``location /`` blocks (for app.dative.ca and v2.dative.ca) should look as
follows::

  # Uncomment the following in order to shut down access to v2.dative:
  default_type text/html;
  return 200 "<!DOCTYPE html><h2>Dative is Temporarily Down</h2><p>Check back soon.</p>\n";
  # try_files $uri $uri/ =404;

Remove the symlink for app.onlinelinguisticdatabase.org::

  $ sudo rm /etc/nginx/sites-enabled/app.onlinelinguisticdatabase.org

To restore the symlinks for app.onlinelinguisticdatabase.org::

  $ sudo ln -s /etc/nginx/sites-available/app.onlinelinguisticdatabase.org /etc/nginx/sites-enabled/

Reload Nginx::

  $ sudo systemctl reload nginx

When the OLD is running, navigating to
``https://app.onlinelinguisticdatabase.org/blaold/forms`` displays::

  {"error":"Authentication is required to access this resource."}

When access to the OLD is disabled through Nginx, navigating to the above
displays an SSL warning in the browser.

When the Dative apps have been shut down, the following two URLs:

- https://app.dative.ca
- https://v2.dative.ca

Should display::

  Dative is Temporarily Down
  Check back soon.

For good measure, also shut down the OLD API as follows::

  $ docker stop old
  $ docker stop old2

To restore the OLDs::

  $ ./reloadolds.sh


Migration Notification Email to Dative Users
--------------------------------------------------------------------------------

Draft of Email to Dative Users (2024-06-16)::

  Dear user of Dative and the Online Linguistic Database (OLD),

  My name is Joel Dunham. I am the original creator of Dative and the OLD, a suite
  of Internet tools for collaborative linguistic data management. You are
  receiving this email because you have one or more accounts on Dative and may
  have used it to store or process your data.

  This letter is to inform you that CIPL, in conjunction with INT, have kindly
  offered to support the continued deployment of Dative on the web. CIPL is the
  Comité International Permanent des Linguistes and INT is the Instituut voor de
  Nederlandse Taal.

  At present, the Dative data (the "OLDs") are being served on a commercial
  hosting platform, the cost of which has been covered primarily by grants
  received by professor Alan Bale of Concordia University and, to a lesser extent,
  by my own corporation, Lambda Bar Software Ltd.

  We are happy to announce, that as of June 28, 2024 both the Dative app and all
  of the OLDs will be hosted on a server run by INT's information technology
  department. With the support of INT and CIPL we expect to be able to better
  respond to issue requests and new OLD creation requests. We also hope to be able
  to add new, long-awaited features to the Dative/OLD system.

  On the date of the migration (June 28), we anticipate a short period (4-8
  hours) of downtime, during which Dative, which is served at
  https://app.dative.ca, and the OLDs, which are served under
  https://app.onlinelinguisticdatabase.org/, will be unavailable. Once the
  migration is complete, Dative will again be available at https://app.dative.ca
  and the OLDs will now be served at sub-paths under https://app.dative.ca/olds/.

  What is required of you? If you do not take issue with your data being
  transferred to the INT-managed server and if you never use the OLD API (or do
  not know what that means), then there is nothing you need to do.

  If you do not want your data to be transferred, please respond to this email
  indicating that fact, well in advance of the migration date of June 28, 2024.

  If you use the OLD API to access your data, e.g., from a Python script, then
  you will need to replace any usage of https://app.onlinelinguisticdatabase.org/
  in your script with the equivalent path under https://app.dative.ca/olds/.
  For example, if you currently use URL
  https://app.onlinelinguisticdatabase.org/myold, then you would need to switch
  to using https://app.dative.ca/olds/myold. If you are a non-technical user of
  Dative, then this paragraph does not apply to you.

  Thank you for taking the time to read this email and for your support of Dative
  and the OLD. With kind regards,

  Joel


How to Migrate the OLD Data from DO to INT
--------------------------------------------------------------------------------

This section describes how to migrate the OLD data from the Digital Ocean server
(DO) to the INT server. Note that this is a cumulative process, at least for the
filesystem data, which is the bulk of it. This means that future migrations take
far less time than the original one.

Running the following commands will dump the MySQL databases on DO, transfer all
the data from DO to local and then to INT, and then ingest the dumped MySQL
databases into the INT RDBMS::

  dodative:$ ./dump-old-dbs.sh
  local:$ ./sync-do-old-to-local.sh
  int:mysql> source /home/joel/load-do-mysql-dumps.sql

For more details on the above, see below. See also GitHub ticket
https://github.com/dativebase/dativebaseclj/issues/22.

Make space for the replicated OLD data on the large mounted disk of the INT
server::

  $ mkdir olds-data-synced-from-do
  $ sudo mkdir /vol1/dative
  $ sudo chown joel:joel /vol1/dative
  $ cd /vol1/dative
  $ mkdir olds-data-synced-from-do

Make space for the dumped OLD data on the DO server::

  $ pwd
  /home/jrwdunham
  $ mkdir mysql-dumps-for-sync-to-int

Dump the MySQL database of an OLD on the DO server::

  $ mysqldump -u admin -p'<<REDACTED>>' okaold > /home/jrwdunham/mysql-dumps-for-sync-to-int/okaold.sql

Hash on DO::

  $ md5sum /home/jrwdunham/mysql-dumps-for-sync-to-int/okaold.sql
  45d85d4fe4c0113a6f7b0eb13eacf36e

Try to SSH to the DO machine from the INT machine::

  $ ssh -vvv -i /home/joel/.ssh/id_rsa jrwdunham@144.126.212.39

Despite numerous attempts, I was unable to SSH into the DO server from the INT
one. I tried the following::

  $ ssh -vvv -i /home/joel/.ssh/id_rsa jrwdunham@144.126.212.39
  $ sudo chown -R joel:joel .ssh
  $ ls -alZ /home/joel/.ssh
  $ restorecon -R -v /home/joel/.ssh

The ufw firewall on the Ubuntu DO machine does not appear to be blocking
inbound SSH. The firewall-cmd on the Alma Linux INT machine does not appear to
be blocking outbound (client) SSH either. I was unable to find evidence of SSH
connection attempts on the DO server, which suggests that the issue is on the
INT side. I decided to work around this by using my local machine as
intermediary.

Rsync the DO files to local::

  $ mkdir /Users/joeldunham/Development/do-to-int-migration-2024-06
  $ rsync -vzz --progress \
      dodative:/home/jrwdunham/mysql-dumps-for-sync-to-int/okaold.sql \
      /Users/joeldunham/Development/do-to-int-migration-2024-06/okaold.sql

Hash on local::

  $ openssl md5 /Users/joeldunham/Development/do-to-int-migration-2024-06/okaold.sql
  45d85d4fe4c0113a6f7b0eb13eacf36e

Rsync the local files to INT::

  $ mkdir /Users/joeldunham/Development/do-to-int-migration-2024-06
  $ rsync -vzz --progress \
      /Users/joeldunham/Development/do-to-int-migration-2024-06/okaold.sql \
      cipl:/vol1/dative/olds-data-synced-from-do/okaold.sql

Hash on INT::

  $ md5sum /vol1/dative/olds-data-synced-from-do/okaold.sql
  45d85d4fe4c0113a6f7b0eb13eacf36e

Load the database dump into the INT MySQL server::

  $ mysql -u admin -p
  > CREATE DATABASE okaold DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;
  > USE okaold;
  > SET NAMES utf8;
  > SOURCE /vol1/dative/olds-data-synced-from-do/okaold.sql
  > select count(id) from form;
  +-----------+
  | count(id) |
  +-----------+
  |      1796 |
  +-----------+

At present, there are 125 OLD-specific directories containing 43G (44,707,772
bytes) of data on DO::

  $ pwd
  /home/jrwdunham/oldsdata
  $ ls -l | wc
  125
  $ du -h .
  43G	.
  $ du .
  44707772

At present, there is 181G of free space on my external mounted volume Charlsea::

  $ pwd
  /Volumes/Charlsea
  $ df -h .
  Filesystem     Size   Used  Avail Capacity iused      ifree %iused  Mounted on
  /dev/disk2s3  2.7Ti  2.6Ti  181Gi    94% 4429892 4290537387    0%   /Volumes/Charlsea

Create a directory to hold the DO data on the Charlsea volume::

  $ mkdir /Volumes/Charlsea/do-backups-2024-06

Rsync the OLD filesystem data from DO to local::

  $ rsync -avzz --progress \
      dodative:/home/jrwdunham/oldsdata/ \
      /Volumes/Charlsea/do-backups-2024-06/oldsdata

Count the OLDs on DO by their directories::

  $ ls -l /home/jrwdunham/oldsdata/

There are 124 OLD-specific top-level directories on the DO server. See
``operator.dativebase.migrate-do-to-int-2024``.

Make a place for the OLD file data in INT::

  $ pwd
  /vol1/dative
  $ mkdir oldsdata

Rsync the local files to INT::

  $ rsync -avzz --progress \
      /Volumes/Charlsea/do-backups-2024-06/oldsdata/ \
      cipl:/vol1/dative/oldsdata

Counting the directories on INT (from aceold to zgaold) indicates that there are
124 OLDs. See ``operator.dativebase.migrate-do-to-int-2024/filesystem-olds``.

Count the OLD MySQL databases on the DO server::

  $ mysql -u admin -p
  mysql> SHOW DATABASES;

There are 124 OLDs on DO. The filesystem and MySQL data are consistent. For the
MySQL-sourced database names, see
``operator.dativebase.migrate-do-to-int-2024/mysql-olds``.

There is a dump script on the DO server. Running this script should dump all
data on DO needed in order to perform a manual synchronization of data to
another system. (Note: this should be used in coordination with a prior MySQL
shutdown in order to ensure an identical (consistent) replication.) To generate
this script, see ``operator.dativebase.migrate-do-to-int-2024/dump-do``. To run
the script::

  $ ./dump-old-dbs.sh

Once the above completes, the MySQL data are written to::

  /home/jrwdunham/mysql-dumps-for-sync-to-int/

The filesystem data require no dump step.

Rsync the DO DB dumps from DO to local::

  $ rsync -avzz --progress \
      dodative:/home/jrwdunham/mysql-dumps-for-sync-to-int/ \
      /Users/joeldunham/Development/do-to-int-migration-2024-06

Pull the DO ``servers.json`` file to local::

  $ rsync -vzz --progress \
      dodative:/home/jrwdunham/apps/dative/releases/dist/servers.json \
      /Users/joeldunham/Development/do-to-int-migration-2024-06-servers.json

To run the full sync from DO to local, including the DBs and the filesystem
data::

  $ ./sync-do-old-to-local.sh

Rsync the local files to INT::

  $ rsync -avzz --progress \
      /Volumes/Charlsea/do-backups-2024-06/oldsdata/ \
      cipl:/vol1/dative/oldsdata

Rsync the local MySQL dump files to INT::

  $ rsync -avzz --progress \
      /Users/joeldunham/Development/do-to-int-migration-2024-06/ \
      cipl:/vol1/dative/olds-data-synced-from-do

To run the full sync from DO to local and then to INT, including the DBs and the
filesystem data::

  $ ./sync-do-old-to-local-to-int.sh

Summary of replication commands::

  dodative:$ ./dump-old-dbs.sh
  local:$ ./sync-do-old-to-local.sh
  int:mysql> source /home/joel/load-do-mysql-dumps.sql

Note that both ``./sync-do-old-to-local.sh`` and
``./sync-do-old-to-local-to-int.sh`` will prompt for the INT SSH key passphrase.

Note also that the last MySQL source command is a complete refresh, meaning it
redefines all OLD DBs in INT. This can take a while, ~10 minutes.

The following steps need only be, and have already been, performed once. Of
course, if we add a new OLD to DO and alter the servers.json file, then these
will need to be run again. Use the REPL to create an INT-specific
``servers.json`` file locally, using the DO analog::

  => (println (olds->servers-json-str mysql-olds))

and then copy the output of the above to the clipboard and paste it into
``/nginx/dative/servers.json`` on the INT machine.
