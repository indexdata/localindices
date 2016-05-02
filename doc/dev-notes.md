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

Note to self: sometimes something else, such as Squid, is running on
the woefully oversubscribed port 8080, and so Tomcat can't start and
these URLs won't work. To discover what has the port, use `lsof -i
:8080`.

The `localindices` module
-------------------------

There is extensive documentation in
[localindices/doc/manual/](manual/manual.html), but this is all to do
with how to operate the Harvester admin console -- no developer
documentation.

Three directories from the source tree are omitted from the Debian
packages:

* `lib` -- yet more copies of popular Java libraries including Jetty
  and log4j.
* `solr` -- Solr configuration files.
* `webapps` -- contains a WAR of Solr.

I don't understand why these all exist in `localindices` when the
equivalents are all provided by the `lui-solr` packages -- in fact,
that is the whole purpose of those packages. My best guess is that we
use the `localindices` versions of these files when doing development,
but leave them behind and use those from the `lui-solr4` packages in
deployment. ### It would be great to confirm this.


Development system on Linux
---------------------------

#### General setup and LUI-Solr

Here's what I did on a relatively clean Debian Jessie install (with
OpenJDK and a GUI installed, at least). The (outdated) Solr
documentation on deploying with Tomcat is at
<http://wiki.apache.org/solr/SolrTomcat>.
The wiki is a good resource that contains useful things not contained
in the reference documentation (or at least not spelled out as
explicitly), but suffers from typically wonky wiki organization. The
Solr reference guide for 4.9.1 is available as a PDF (still no luck
finding a nicely indexed HTML version!) at
<https://archive.apache.org/dist/lucene/solr/ref-guide/apache-solr-ref-guide-4.9.pdf>

    $ sudo apt-get install git tomcat8 tomcat8-admin

Add the following lines into /etc/tomcat8/tomcat-users.xml to give access to the Tomcat admin tool:

    <role rolename="manager-gui"/>
    <user username="admin" password="tc3636" roles="manager-gui"/> <!-- or whatever -->

And then:

    $ sudo /usr/sbin/service tomcat8 restart

You should now be able to look at your Tomcat config from a browser
pointing to <http://localhost:8080/manager/html>

Now we're ready to start working with the git repo:

    $ git clone ssh://git.indexdata.com:222/home/git/pub/lui-solr
    $ LUI=`pwd`/lui-solr

To run Solr out of the checked out lui-solr repo (this is what I would
probably want to do, so I can just commit changes to the repo), do the
following. (Many of these steps are to get files and directories into
the places where the lui-solr context-fragment expects them to be):

    $ sudo mkdir -p /usr/share/masterkey/lui/solr4/war
    $ sudo ln -s $LUI/dist/solr-4.9.1.war /usr/share/masterkey/lui/solr4/war/solr.war
    $ sudo ln -s $LUI/lib/* /usr/share/tomcat8/lib
    $ sudo mkdir -p /usr/share/masterkey/lui/solr4/master/collection1
    $ sudo ln -s $LUI/conf /usr/share/masterkey/lui/solr4/master/collection1/conf
    $ sudo mkdir -p /var/lib/masterkey/lui/solr4/master
    $ sudo chown tomcat8:tomcat8 /var/lib/masterkey/lui/solr4/master
    $ cd $LUI/conf
    $ ln -s solrconfig-master.xml solrconfig.xml # this is not ideal, because it creates a file in the repo that you have to ignore
    $ sudo ln -s $LUI/etc/solr4-tomcat-context-master.xml /etc/tomcat8/Catalina/localhost/solr4.xml
    $ sudo /usr/sbin/service/tomcat8 restart

You should now be able to get to the Solr admin screen at
<http://localhost:8080/solr4/>.

Much of this pain might be avoidable with a move to Solr 5+, not sure
what that entails. Here is the project's explanation of why they are
ditching JSP container deployment:
<https://wiki.apache.org/solr/WhyNoWar>.
That is to say, they are ditching other peoples' JSP containers :-).

#### Harvester

Now we can add the harvester and its admin console. As with LUI, we
will do this by creating symbolic links that make the development
environment resemble the production environment.

    $ git clone ssh://git.indexdata.com:222/home/git/private/localindices
    $ cd $LOCALINDICES
    $ LOCALINDICES=`pwd`
    $ sudo apt-get install maven # this installs approximately 1,000,000 packages
    $ mvn install > install.log # generates lots of output, you may want to look for warnings/errors
    $ sudo apt-get install mysql-server # Debian
    $ sudo apt-get install mysql-server-5.6 # Ubuntu 15.10
    $ mysql -u root -p
        mysql> create database localindices;
        mysql> grant all privileges on localindices.* to 'localidxadm'@'localhost' identified by 'localidxadmpass';
        mysql> quit
    $ mysql -u localidxadm -plocalidxadmpass localindices < sql/localindices.sql
    $ sudo mkdir -p /var/log/masterkey/harvester
    $ sudo chown tomcat8:tomcat8 /var/log/masterkey/harvester
    $ sudo mkdir -p /etc/masterkey/harvester
    $ sudo ln -s $LOCALINDICES/harvester/target/harvester/WEB-INF/harvester.properties /etc/masterkey/harvester/harvester.properties

    ### Why are the war files created by Maven not used in deployment?

    $ sudo ln -s $LOCALINDICES/harvester/target/harvester /usr/share/masterkey/harvester
    $ sudo ln -s $LOCALINDICES/etc/harvester-context.xml /etc/tomcat8/Catalina/localhost/harvester.xml

##### Harvester is failing to deploy on Tomcat 8:

>[EL Info]: connection: 2016-04-29 23:35:19.852--ServerSession(2136012227)--file:/home/wayne/localindices/harvester/target/harvester/WEB-INF/lib/masterkey-dal-2.8.0.jar_localindicesPU login successful
>[EL Warning]: 2016-04-29 23:35:19.962--UnitOfWork(275800371)--Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.5.0.v20130507-3faac2b): org.eclipse.persistence.exceptions.DatabaseException
>Internal Exception: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: Table 'localindices.SETTING' doesn't exist
>Error Code: 1146
>Call: SELECT COUNT(ID) FROM SETTING WHERE NAME LIKE CONCAT(?, ?)
>	bind => [2 parameters bound]
>Query: ReportQuery(referenceClass=Setting sql="SELECT COUNT(ID) FROM SETTING WHERE NAME LIKE CONCAT(?, ?)")
>Apr 29, 2016 11:35:19 PM org.apache.catalina.core.StandardContext startInternal
>SEVERE: Error listenerStart
>Apr 29, 2016 11:35:19 PM org.apache.catalina.core.StandardContext startInternal
>SEVERE: Context [/harvester] startup failed due to previous errors

#### Harvester console

    $ sudo ln -s $LOCALINDICES/harvester-admin/target/harvester-admin /usr/share/masterkey/harvester-admin
    $ sudo ln -s $LOCALINDICES/etc/harvester-admin-context.xml /etc/tomcat8/Catalina/localhost/harvester-admin.xml

Development system on a Mac
---------------------------

Much of this is the same as on Linux. First, though, you will need to
install and start Tomcat:

    $ brew install tomcat
    $ cd /usr/local/opt/tomcat/libexec/bin//usr/local/opt/tomcat/libexec/bin/
    $ ./startup.sh 

You can't just `brew install solr` because it gives you Solr 5, and
our software needs Solr 4. Since the present `lui-solr` package uses
Solr 4.9.1, that's the best one to use, so:

    $ cd
    $ wget https://archive.apache.org/dist/lucene/solr/4.9.1/solr-4.9.1.tgz
    $ tar xzf solr-4.9.1.tgz

Now you can proceed essentially as outlined above, with the paths
adjusted as necessary.

