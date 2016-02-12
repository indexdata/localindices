Running the Index Data harvester
================================

Overview
--------

The complete harvester setup consists of two git modules:

* lui-solr -- configuration for Solr to hold harvested records.
* localindices -- the harvester software, including its control panel.

(The lui-solr module is in /pub/ but localindices is in
/private/. That doesn't make a great deal of sense, but it doesn't
matter much.)

There is also a lui-solr3 module, which can be ignored. It seems to
have been a misconceived attempt to fork the source so it can work
with Solr 3.

*What I want to know:*
How to get a lui-solr flavoured Solr running, how to change the schema
for an existing Solr database, how to plumb the harvester into it,
what you need installed before you start, all that kind of
thing. Basically, everything I need to know to run it all locally and
start making changes.


lui-solr
--------

Tne lui-solr package contains its own WAR files for Solr, so there is
on need to install Solr separately. (I don't understand why we're
doing this. Why not just depend on a Solr package?)

The localindices code can run against several different backends. But
one of them (plain files) is mostly a proof-of-concept, not intended
for use in real life. Another (Zebra) is deprecated. So for almost all
purposes, Solr (as configured by lui-solr) will be used.

Compilation is managed by Maven, so should be possible without using
an IDE. Deployment is by copying the WAR file into Tomcat's live area:
we may add Maven rules to run under Jetty.

It seems as though the expected way to use lui-solr is by building a
Debian package and installing it. But the NEWS file is out of date
with respect to the version number in IDMETA, and there is no Debian
changelog, so it's hard to know the status of this code.

The lui-solr model makes several fields special. These include, but
may not be limited to:
* database -- sub-database identifier
* database_name -- corresponding sub-database name
* id -- unique key, used to know when updates are provided

The RPM specification file for lui-solr seems to be outdated: for
example, it refers to etc/solr-tomcat-context.xml which doesn't exist
in the current module, but solr4-tomcat-context-master.xml and
solr4-tomcat-context-slave.xml both do. There are other such
inconsistencies. It seems this was last touched for release 0.30 in
January 2013, and should be considered completely
outdated. Presumably the Debian package is more up to date.

lui-solr has both conf and conf3 directories. The latter seems to be a
set of obsolete config files which were used for running against Solr
3.x, which we no longer do. (It doesn't get a mention in debian/rules)

Zookeeper is somehow involved with starting the LUI Solr instance: see
lui-solr/etc/init.d/indexdata-solr-zookeeper. But John says this is
just Dennis's experiments, not used in production.


localindices
------------

(To be written)


