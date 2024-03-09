# Advanced log configuration

GeoServer uses the Log4J framework for logging, which is configured by selecting a logging profile (in the [global settings](globalsettings.md#config_globalsettings_log_location)).

The GeoServer logging profiles assign logging levels to specific server operations:

-   GeoServer loggers record server function and the activity of individual services.
-   GeoWebCache loggers record the activity of the tile protocol library used by GeoServer.
-   GeoTools loggers record the activity of the data access and rendering library used by GeoServer.
-   The appender `stdout` is setup as a Console appender sending information to standard output, based on **Log to Stdout** [global settings](globalsettings.md#config_globalsettings_log_stdout).
-   The appender `geoserverlogfile` is setup as a FileAppender or RollingFile appender sending information to the **Log location** [global settings](globalsettings.md#config_globalsettings_log_location).
-   Logging levels range from:
    -   Failure (`FATAL`, `ERROR`, `WARN`) levels
    -   Operational (`INFO`, `CONFIG`) levels
    -   Verbose (`DEBUG`, `TRACE`, `FINEST`) levels

In addition to the built-in profiles you may setup a custom logging profile, or override the logging configuration completely (even to use another logging library altogether).

## Built-in logging profiles

GeoServer includes several built-in logging profiles:

-   [DEFAULT_LOGGING](download/DEFAULT_LOGGING.xml)
-   [GEOSERVER_DEVELOPER_LOGGING](download/GEOSERVER_DEVELOPER_LOGGING.xml)
-   [GEOTOOLS_DEVELOPER_LOGGING](download/GEOTOOLS_DEVELOPER_LOGGING.xml)
-   [PRODUCTION_LOGGING](download/PRODUCTION_LOGGING.xml)
-   [QUIET_LOGGING](download/QUIET_LOGGING.xml)
-   [VERBOSE_LOGGING](download/VERBOSE_LOGGING.xml)

The built-in logging profiles are installed into your data directory the first time the application is run. If you have customized (see the next section) these files and wish to restore the original contents:

-   Use the startup parameter `-DUPDATE_BUILT_IN_LOGGING_PROFILES=true`, the built-in logging profiles will be checked and updated if required; or
-   Delete the file and restart GeoServer, the missing file will be restored; or
-   Copy the contents from the download links above

For a description of these logging profiles see [Logging Profile](globalsettings.md#config_globalsettings_log_profile). Additional built-in logging profiles are supplied by installed extensions (example [IMPORTER_LOGGING](download/IMPORTER_LOGGING.xml) profile is built into the importer extension).

## Custom logging profiles

Anyone can write a new logging profile by adding a Log4J configuration file to the list of files already available in the `$GEOSERVER_DATA_DIR/logs` folder.

Profiles in this folder that match **`*_LOGGING.*`** will be listed on the global settings page as available for use. The name of the file, excluding the extension, will be presented as the profile name.

Here is an example, taken from the [DEFAULT_LOGGING.xml](download/DEFAULT_LOGGING.xml) configuration, which enables additional GeoServer log messages to be included in the logs:

~~~xml
{% 
  include "../../../../../src/main/src/main/resources/DEFAULT_LOGGING.xml"
%}
~~~

Any custom configuration can be setup to enable specific packages to emit logs at the desired logging level.

There are however a few rules to follow:

-   Custom levels are available for `CONFIG` and `FINEST` levels.
-   Appenders are used to output logging information, with GeoServer providing external configuration for appenders named `geoserverlogfile` and `stdout`.
    -   Always include a `geoserverlogfile` `FileAppender` or `RollingFile` appender that GeoServer will configure to work against the location configured in the [global settings](globalsettings.md#config_globalsettings_log_location).

        Care is taken to preserve your file extension when updating `<filename>` location, so if you wish to log to **`access_logs.txt`** you may do so, and the **`txt`** extension will be preserved.

    -   When setting `geoserverlogfile` appender as `RollingFile` appender, care is taken to preserve your `<filePattern>` extensions, which must align with the roll over strategies configured.

        As an example `-%i` is used with DefaultRolloverStrategy to produce a maximum of `3` backup files.

        ~~~xml
        {% 
          include "../../../../../src/main/src/main/resources/DEFAULT_LOGGING.xml"
        %}
        ~~~

    -   a `Console` appender writing to the standard output should be called `stdout` and again GeoServer will enable/disable it according to the configuration set in the [global settings](globalsettings.md#config_globalsettings_log_stdout)

        ~~~xml
        {% 
          include "../../../../../src/main/src/main/resources/DEFAULT_LOGGING.xml"
        %}
        ~~~
-   Loggers are used to collect messages from geoserver components, and individual libraries used.
    -   GeoServer Logger names match up with the package names in the project javadocs overview (available for download).

        As an example package `org.geoserver.wms` is listed, allowing level of WMS service logging to be controlled:

        ``` xml
        <Logger name="org.geoserver.wms" level="debug" additivity="false">
            <AppenderRef ref="stdout"/>
            <AppenderRef ref="geoserverlogfile"/>
        </Logger>
        ```

    -   GeoTools Logger names match up with the package names in the [project javadocs overview](https://docs.geotools.org/latest/javadocs/overview-summary.html).

        As an example package `org.geotools.data.shapefile` is listed, allowing level of shapefile logging to be controlled:

        ``` xml
        <Logger name="org.geotools.data.shapefile" level="debug" additivity="false">
            <AppenderRef ref="stdout"/>
            <AppenderRef ref="geoserverlogfile"/>
        </Logger>
        ```

    -   Assign a level to each logger indicating the level of detail you wish to record:

        | Level  | Description                                                                       |
        |--------|-----------------------------------------------------------------------------------|
        | OFF    | Turn off all logging                                                              |
        | FATAL  | A serious problem has occurred, application may be crashing or in need of restart |
        | ERROR  | Problem has occurred, application unable to perform requested operation           |
        | WARN   | Potential problem, application will try and continue                              |
        | INFO   | Normal function indicating what application is doing.                             |
        | CONFIG | Normal application function during application startup and configuration          |
        | DEBUG  | Internal messages intended for debugging                                          |
        | TRACE  | Metod by method tracing of execution                                              |
        | FINEST | Really detailed troubleshooting of an algorithm                                   |
        | ALL    | Turn on all logging                                                               |

        The more verbose logging levels potentially include a stack-trace showing where the message occurred.

    -   Use `additivity="false"` to prevent a message collected from one logger from being passed to the next.

        If you end up with double log messages chances check for this common misconfiguration.

    -   The `Root` logger is last in the list and should collect everything.

### Example of console only logging

Copy built-in logging profile and customize:

1.  Copy an example such as [QUIET_LOGGING.xml](download/QUIET_LOGGING.xml) to **`CONSOLE_LOGGING.xml`**:

2.  Update the initial part of **`CONSOLE_LOGGING.xml`** with the new name:

    ``` xml
    <?xml version="1.0" encoding="UTF-8"?>
    <Configuration name="CONSOLE_LOGGING" status="fatal" dest="out">
    ```

3.  Double check the Console appender configuration:

    ``` xml
    <Appenders>
        <Console name="stdout" target="SYSTEM_OUT">
            <PatternLayout pattern="%date{dd mmm HH:mm:ss} %-6level [%logger{2}] - %msg%n%"/>
        </Console>
    </Appenders>
    ```

4.  Add appenders for geoserver (and any others you wish to track):

    ``` xml
    <Logger name="org.geoserver" level="ERROR" additivity="false">
        <AppenderRef ref="stdout"/>
    </Logger>
    <Logger name="org.vfny.geoserver" level="ERROR" additivity="false">
        <AppenderRef ref="stdout"/>
    </Logger>
    ```

5.  Double check the root logger:

    ``` xml
    <Root level="FATAL">
        <AppenderRef ref="stdout"/>
    </Root>
    ```

6.  This result provides minimal feedback to the console, only reporting when GeoServer encounters an error.

## Overriding the log location setup in the GeoServer configuration

When setting up a cluster of GeoServer machines it is common to share a single data directory among all the cluster nodes.

There is however a gotcha, all nodes would end up writing the logs in the same file, which would cause various kinds of troubles depending on the operating system file locking rules (a single server might be able to write, or all together in an uncontrolled manner resulting in an unreadable log file).

A common choice could be to use the machine name as a distinction, setting values such as `logs/geoserver_node1.log`, `logs/geoserver_node2.log` and so on: in this case all the log files would still be contained in the data directory and properly rotated, but each server would have its own separate log file to write on.

In this case it is convenient to set a separate log location for each GeoServer node:

-   The `GEOSERVER_LOG_LOCATION` parameter can be set as system property, environment variables, or servlet context parameters:

        GEOSERVER_LOG_LOCATION=<the location of the file>

    This setting overrides global setting, and is applied to `geoserverlogfile` appender as a template for filename and filePattern.

-   This same effect may be obtained using Log4J [property substitution](https://logging.apache.org/log4j/2.x/manual/configuration.html#PropertySubstitution), where a wide range of [property lookups](https://logging.apache.org/log4j/2.x/manual/lookups.html) are available.

    ``` xml
    <RollingFile name="geoserverlogfile">
        <filename>logs/geoserver-${hostName}.log</filename>
        <filePattern>logs/geoserver-${hostName}-%d{yyyy-MM-dd-HH}-%i.zip</filePattern>
        <PatternLayout pattern="%date{dd mmm HH:mm:ss} %-6level [%logger{2}] - %msg%n"/>
        <Policies>
          <OnStartupTriggeringPolicy />
          <SizeBasedTriggeringPolicy size="20 MB" />
          <TimeBasedTriggeringPolicy />
        </Policies>
        <DefaultRolloverStrategy max="9" fileIndex="min"/>
    </RollingFile>
    ```

    Where `${hostName}` is the current system host name or ip address.

## Forcing GeoServer to relinquish Log4J control

GeoServer internally overrides the Log4J configuration by using the current logging configuration as a template and applying the log location and standard output settings configured by the administrator.

If you wish GeoServer not to override the normal Log4J behavior you can set the following parameter among the JVM system variables, environment variables, or servlet context parameters:

    RELINQUISH_LOG4J_CONTROL=true

This can be combined with `log4j2.configurationFile` system property to [configure Log4J externally](https://logging.apache.org/log4j/2.x/manual/configuration.html#AutomaticConfiguration) :

    -DRELINQUISH_LOG4J_CONTROL=true -Dlog4j2.configurationFile=logging_configuration.xml

## Forcing GeoServer to use an alternate logging redirection

GeoServer uses the GeoTools logging framework, which in turn is based on Java Logging, but allowing to redirect all messages to an alternate framework of the user's choice.

By default GeoServer setups a Log4J redirection, but it is possible to configure GeoServer to use plain Java Logging, or Commons Logging instead (support for other loggers is also possible by using some extra programming).

If you wish to force GeoServer to use a different logging mechanism set the following parameters among the JVM system variables, environment variables, or servlet context parameters:

    GT2_LOGGING_REDIRECTION=[CommonsLogging,JavaLogging,Log4J,Log4J2,LogBack]
    RELINQUISH_LOG4J_CONTROL=true

As noted in the example you'll also have to demand that GeoServer does not exert control over the Log4J configuration.

### Force java logging example

As an example to configure java logging:

    -DRELINQUISH_LOG4J_CONTROL=true -DGT2_LOGGING_REDIRECTION=JavaLogging -Djava.util.logging.config.file=logging.properties

With java util logging configuration provided by **`logging.properties`**:

``` properties
handlers=java.util.logging.ConsoleHandler

java.util.logging.ConsoleHandler.level = ALL
java.util.logging.ConsoleHandler.formatter = org.geotools.util.logging.MonolineFormatter
org.geotools.util.logging.MonolineFormatter.source = class:short

.level= ALL
org.geoserver.level = CONFIG
org.vfny.geoserver.level = WARN

org.geotools.level = WARN
org.geotools.factory.level = WARN
```
