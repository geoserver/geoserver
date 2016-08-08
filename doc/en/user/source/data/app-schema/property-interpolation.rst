.. _app-schema.property-interpolation:

Property Interpolation
======================

Interpolation in this context means the substitution of variables into strings. GeoServer app-schema supports the interpolation of properties (the Java equivalent of environment variables) into app-schema mapping files. This can be used, for example, to simplify the management of database connection parameters that would otherwise be hardcoded in a particular mapping file. This enables data directories to be given to third parties without inapplicable authentication or system configuration information. Externalising these parameters make management easier.

Defining properties
-------------------

* If the system property ``app-schema.properties`` is not set, properties are loaded from ``WEB-INF/classes/app-schema.properties`` (or another resource ``/app-schema.properties`` on the classpath).
* If the system property ``app-schema.properties`` is set, properties are loaded from the file named as the value of the property. This is principally intended for debugging, and is designed to be used in an Eclipse launch configuration.

    * For example, if the JVM is started with ``-Dapp-schema.properties=/path/to/some/local.properties``, properties are loaded from ``/path/to/some/local.properties``.

* System properties override properties defined in a configuration file, so if you define ``-Dsome.property`` at the java command line, it will override a value specified in the ``app-schema.properties`` file. This is intended for debugging, so you can set a property file in an Eclipse launch configuration, but override some of the properties contained in the file by setting them explicitly as system properties.
* All system properties are available for interpolation in mapping files.

Predefined properties
---------------------

If not set elsewhere, the following properties are set for each mapping file:

* ``config.file`` is set to the name of the mapping file
* ``config.parent`` is set to the name of the directory containing the mapping file

Using properties
----------------

* Using ``${some.property}`` anywhere in the mapping file will cause it to be replaced by the value of the property ``some.property``.
* It is an error for a property that has not been set to be used for interpolation.
* Interpolation is performed repeatedly, so values can contain new interpolations. Use this behaviour with caution because it may cause an infinite loop.
* Interpolation is performed before XML parsing, so can be used to include arbitrary chunks of XML.

Example of property interpolation
---------------------------------

This example defines an Oracle data store, where the connection parameter are interpolated from properties::

    <sourceDataStores>
        <DataStore>
            <id>datastore</id>
            <parameters>
                <Parameter>
                    <name>dbtype</name>
                    <value>Oracle</value>
                </Parameter>
                <Parameter>
                    <name>host</name>
                    <value>${example.host}</value>
                </Parameter>
                <Parameter>
                    <name>port</name>
                    <value>1521</value>
                </Parameter>
                <Parameter>
                    <name>database</name>
                    <value>${example.database}</value>
                </Parameter>
                <Parameter>
                    <name>user</name>
                    <value>${example.user}</value>
                </Parameter>
                <Parameter>
                    <name>passwd</name>
                    <value>${example.passwd}</value>
                </Parameter>
            </parameters>
        </DataStore>
    </sourceDataStores>

Example property file
---------------------

This sample property file gives the property values that are interpolated into the mapping file fragment above. These properties can be installed in ``WEB-INF/classes/app-schema.properties`` in your GeoServer installation::

    example.host = database.example.com
    example.database = example
    example.user = dbuser
    example.passwd = s3cr3t


