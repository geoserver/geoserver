.. _app-schema_online_tests:

App-Schema Online Tests
=======================

The offline tests in app-schema-test suite use properties files as data source. In reality, properties files are only used as testing means, whereas in production, users would use databases as data source. Users would often encounter problems/bugs that cannot be recreated using properties files, which raises the need to run with a test database. Moreover, Niels' joining support to increase performance can only be tested online, and we need to ensure that it works with the current features/bug fixes covered in the tests. 

Prerequisites
-------------

This requires installation of Oracle driver in Maven repository::       
                                  
    mvn install:install-file -Dfile=ojdbc7.jar -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.2 -Dpackaging=jar 

You would also need to have test databases for both Oracle and Postgis. Then follow these steps:

* Create oracle.properties and postgis.properties in {user directory}/.geoserver directory.

* Populate each properties file with database details, e.g.::

    password=onlinetestuser

    passwd=onlinetestuser

    user=onlinetestuser

    port=5432

    url=jdbc\:postgresql\://localhost:5432/onlinetest

    host=localhost

    database=onlinetest

    driver=org.postgresql.Driver

    dbtype=postgisng 

Running tests from Maven
------------------------

Without specifying any profile, the default Maven configuration for app-schema-test is to run offline tests only. 

To run online tests, enable the profile::

    -Papp-schema-online-test 

This profile enables the data reference set tests and offline tests to run online. Data reference set tests are online tests based on data and use cases from GeoScience Victoria. Each is explicit for a database type (Oracle and Postgis) and has a copy to run with joining enabled. 

The offline tests are configured to run online with joining through separate modules for each database: app-schema-oracle-test and app-schema-postgis-test. These modules are placeholders for pom.xml files containing database specific parameters. This makes it easy to identify when a test fails with a particular database when running from Maven/buildbot. 

Memory requirements
```````````````````

The online tests require more memory than usual, so specifying the usual -Dtest.maxHeapSize=256m is not enough. Specify --Dtest.maxHeapSize=1024m instead.

When the build is successful, you would see this in the "Reactor Summary"::

    [INFO] Application Schema Integration Online Test with Oracle Database  SUCCESS  [5:52.980s]
    [INFO] Application Schema Integration Online Test with Postgis Database  SUCCESS  [1:42.428s]

Running tests from JUnit
------------------------

There is no need to import the online test modules as they are empty and you cannot run the tests through them in Eclipse.

To run offline tests (in app-schema-test/src/test/java/org/geoserver/test) with a test database, 
enable joining and specify the database. Add these parameters in VM Arguments for postgis::

    -Dapp-schema.joining=true -DtestDatabase=postgis -Xmx256m 

Similarly, to test with oracle::

    -Dapp-schema.joining=true -DtestDatabase=oracle -Xmx256m 

Additionally for Oracle, you also need to add ojdbc14.jar in the test Classpath. 

.. note:: Please note that you should only run the tests in org.geoserver.test package with the above parameters, since the data reference tests in org.geoserver.test.onlineTest package contain non-joining tests which would fail.   

You do not need to specify these VM Arguments for running data reference tests (in app-schema-test/src/test/java/org/geoserver/test/onlineTest). However, you would still need to specify the Oracle JDBC driver in the Classpath for Oracle specific tests. Data reference tests package also requires 768m memory to run from JUnit. 

Adding new tests
----------------

When adding new tests to app-schema-test suite (except for onlineTest package for data reference tests), please note the following:

Test offline only
`````````````````

If your test is a special case and does not need to be tested online, exclude them in both app-schema-oracle-test and app-schema-postgis-test pom.xml and ignore the points beyond this. Otherwise, read on. 

idExpression
````````````

If your test database does not use primary keys, ensure idExpression is specified for the top level element in your mapping file.

Multi-valued properties ordering 
````````````````````````````````

When testing multi-valued properties, the order of the values could vary depending on the data source type. To be safe, compare your values as a list, instead of evaluating individual xpath node against a single value for such properties. E.g.::

        List<String> names = new ArrayList<String>();
        names.add("New Group");
        names.add("-Xy");
        String name = evaluate("//gsml:MappedFeature[@gml:id='" + id
                + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:MappedFeature[@gml:id='" + id
                + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

This is because of the difference in the handling of queries with joining. Joining uses order by when querying tables. When the tests run offline, property data store returns data from properties file unordered.

When joining is enabled:

* If the multi-valued properties are not feature chained, the order is unpredictable.

* If the multi-valued properties are feature chained, they are ordered by the foreign key used in feature chaining.

Column names in upper case
``````````````````````````

Ensure column names in mapping files are in upper case, even if they are in lower case in the properties file. This is to avoid failures with Oracle database, due to OracleDialect not wrapping names with escape characters. To work around this, the script for online tests creates the columns in upper case, therefore should be referred by with upper case. 

Functions in feature chaining
`````````````````````````````

If using feature chaining, avoid using functions in sourceExpression for linking attributes, i.e. attribute used in both OCQL and linkField. This is because functions used in feature chaining are not supported with joining support. 

3D tests
````````
There are a number of tests that try out 3D features in App-schema. To run these as online tests against a postgis or oracle database, a number of prerequisites must be met.

For PostGIS:

    * You must use postgis 2 to support 3D.
    * In your postgis, if it hasn't been done yet, this command must be executed to support srid 4979 (wgs84 with 3d)::

        INSERT into spatial_ref_sys (srid, auth_name, auth_srid, proj4text, srtext) values ( 4979, 'epsg', 4979, '+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs ', 'GEOGCS["WGS 84",DATUM["World Geodetic System 1984",SPHEROID["WGS 84",6378137.0,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0.0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.017453292519943295],AXIS["Geodetic latitude",NORTH],AXIS["Geodetic longitude",EAST],AXIS["Ellipsoidal height",UP],AUTHORITY["EPSG","4979"]]');


For Oracle:

    * You must use Oracle 11g Release 2, preferably the latest version that can be downloaded for best 3D support
    * Oracle does NOT support WKT parsing of 3d geometries, so some extra DBA work is needed to set this up. Otherwise the online tests, which rely on WKT to enter data in the database, will fail.

      You need the following package 'SC4O' (Spatial Companion for Oracle), created Simon Greener: download at http://www.spatialdbadvisor.com/files/SC4O.zip.
      It has an installation script for linux and windows that must be run from the server that runs oracle. The package will provide JTS functionality that can be called from PL/SQL.

      If the online test user is different from the user used for the installation of the package, the online test user must be given permission to use the package.
      You must also execute as an admin user the following command (with 'onlinetestuser' being the online test user)::

		CALL DBMS_JAVA.GRANT_PERMISSION('onlinetestuser','java.lang.RuntimePermission','getClassLoader','');
      
      Afterwards, you have to specify the user where the SC4O package was installed to the online testing system. You do this by specifying the system property -DSC4OUser. If it is the same as the online test user, you can omit this parameter.
      The online test will use the JTS method for wkt parsing (ST_GeomFromEWKT) rather than the regular oracle method SDO_GEOMETRY.      
      For example, I installed the package using the System user. Then I gave onlinetestuser permission to execute it.
      I run the tests with -DSC4OUser=System so it knows to use the System.SC4O.ST_GeomFromEWKT method.

Running MongoDB Online Tests 
----------------------------

MongoDB online tests are activated by the ``app-schema-online-test`` profile and will run if configuration file ``{user directory}/.geoserver/mongodb.properties`` is available. If the configuration file is not available an example file will be created and tests will be skipped. The content of the configuration file should look like this::

	mongo.port=27017
	mongo.host=127.0.0.1

During the tests a new database will be created in MongoDB and when the tests end that database will be removed. 