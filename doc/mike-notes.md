Running the Index Data harvester
================================

*What I want to know:*
How to get a `lui-solr` flavoured Solr running, how to change the schema
for an existing Solr database, how to plumb the harvester into it,
what you need installed before you start, all that kind of
thing. Basically, everything I need to know to run it all locally and
start making changes.


Overview
--------

The complete harvester setup consists of two git modules:

* `lui-solr` -- configuration for Solr to hold harvested records.
* `localindices` -- the harvester software, including its control panel.

(### The `lui-solr` module is in `/pub/` but `localindices` is in
`/private/`. That doesn't make a great deal of sense, but it doesn't
matter much.)

There is also a `lui-solr3` module, which can be ignored. It seems to
have been a misconceived attempt to fork the source so it can work
with the long-superseded Solr 3.


The `lui-solr` module
---------------------

The `lui-solr` model makes several fields special. These include, but
may not be limited to:

* database -- sub-database identifier
* database_name -- corresponding sub-database name
* id -- unique key, used to know when updates are provided
* usage tag -- seems to be a human-readable designation
* mngmt tag -- seems to be a human-readable designation

(### Although, oddly, the word `database` occurs nowhere in
`schema.xml` -- so I don't know where this is defined. It's in
`schema-minimal.xml` but I assume that is not used, as it lacks many
important fields.  Also, the primary-key quality of the `id` field is
inherited from the example `schema.xml` included with Solr
distributions.)

The `localindices` code can run against several different backends. But
one of them (plain files) is mostly a proof-of-concept, not intended
for use in real life. Another (Zebra) is deprecated. So for almost all
purposes, Solr (as configured by `lui-solr`) will be used.

The `lui-solr` package contains its own WAR files for Solr, so there is
no need to install Solr separately. (### I don't understand why we're
doing this. Why not just depend on a Solr package?)

Compilation is managed by Maven, so should be possible without using
an IDE. Deployment is by copying the WAR file into Tomcat's live area:
we may add Maven rules to run under Jetty.

It seems as though the expected way to use `lui-solr` is by building a
Debian package and installing it. ### But the NEWS file is out of date
with respect to the version number in IDMETA, and there is no Debian
changelog, so it's hard to know the status of this code. The Debian
control file seems a bit confused over whether the packages are named
`masterkey-lui-solr` or `masterkey-lui-solr4`. We currently build ten
packages, which seems a bit extreme; but that is mostly down to the
master/slave distinction, which I think relates to the Zookeper stuff
that we're not actually doing.

The RPM specification file for `lui-solr` seems to be outdated: for
example, it refers to `etc/solr-tomcat-context.xml` which doesn't exist
in the current module, but `solr4-tomcat-context-master.xml` and
`solr4-tomcat-context-slave.xml` both do. There are other such
inconsistencies. It seems this was last touched for release 0.30 in
January 2013, and should be considered completely
outdated. Presumably the Debian package is more up to date.

Configuration files are found in:

* `conf` -- configuration for Solr itself. The main file seems to be
  `conf/schema.xml`, though it's not clear what all the files are for
  -- especially `schema-minimal.xml` and `schema.xml.org`.
* `etc` -- configuration for wiring Solr into Tomcat, I think. Again,
  there seem to be multiple versions of the key file, for reasons that
  are not clear. May be related to Zookeeper.

(### There are both conf and conf3 directories. The latter seems to be a
set of obsolete config files which were used for running Solr 3.x,
which we no longer do. It doesn't get a mention in `debian/rules`. I
suppose this should have been deleted when the `lui-solr3` package was
created.)

`lib` contains a bunch of third-party JAR files, I assume to be used
by Solr. ### As usual, I am baffled by the inclusion of these. We don't
distribute our own libc.so with Metaproxy.

`scripts` contains five shell-scripts whose purposes are unclear.

Zookeeper is somehow involved with starting the LUI Solr instance: see
`lui-solr/etc/init.d/indexdata-solr-zookeeper`. But John says this is
just Dennis's experiments, not used in production.

### Debian packages

We build ten (ten!) Debian packages from `lui-solr`, in a dependency
DAG as follows. (Each package name begins `masterkey-lui-solr4-` but
I have removed these for simplicity. (I have also ignored the
`masterkey-lui-solr4-zookeper` package, which is not used.)

                  common
              _____/||\_________________
             /      ||                  \
            /       | \____________      \
           /        |              \      \
        master    common-tomcat6    |    slave
         / |        / \             |      |  \
        /  |       /   \   common-tomcat8  |   \
       /   |      /     \    /\            |    \
      /    | ____/       \__/__\______     |     \
     /     |/              /    \     \    |      \
    (   master-tomcat6    /      \  slave-tomcat6  )
     \                   /        \               /
      \______  _________/          \             /
             \/                     \           /
           master-tomcat8           slave-tomcat8

Given the questionable need to support both v6 and v8 of Tomcat (and
the need for both master and slave Solr congfigs) this DAG does
actually make perfect sense. But the specifics of what files each
package installs where seem to be a bit off: for example,

`masterkey-lui-solr4-common-tomcat8` installs
`/usr/share/tomcat8/lib/log4j.properties`, which I assume specifies
the default logging for _all_ Tomcat8 applications. It also installs
`log4j-1.2.16.jar` in the same area, which seems a bit presumptious.

### Using lui-solr


Once the `masterkey-lui-solr4-master-tomcat8` and its dependencies are
installed, you can exercise the running Solr with searches like

* <http://localhost:8080/solr4/query?q=water>
* <http://localhost:8080/solr4/query?q=water%20AND%20dinosaur>
* <http://localhost:8080/solr4/query?q=water%20AND%20dinosaur&debug=query>

(But of course the database will be empty initially, so all the
queries will return no hits.)


The `localindices` module
-------------------------

There is extensive documentation in
[localindices/doc/manual/](manual/manual.html), but this is all to do
with how to operate the Harvester admin console -- no developer
documentation.

Three directories from the source tree are omitted from the Debian
packages, presumably because they are used in development but not in
deployment:

* `lib -- yet more copies of popular Java libraries including Jetty
  and log4j.
* `solr` -- Solr configuration files.
* `webapps` -- contains a WAR of Solr.

I don't understand why these all exist in `localindices` when the
equivalents are all provided by the `lui-solr` packages -- in fact,
that is the whole purpose of those packages.

