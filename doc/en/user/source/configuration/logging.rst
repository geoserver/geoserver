.. _logging:

Advanced log configuration
==========================

GeoServer logging subsystem is based on Java logging, which is in turn by default redirected to Log4J
and controlled by the current logging configuration set in the :ref:`config_globalsettings`.

The standard configuration can be overridden in a number of ways to create custom logging profiles
or to force GeoServer to use another logging library altogheter.

Custom logging profiles
-----------------------

Anyone can write a new logging profile by adding a Log4J configuration file to the list of files already available in the ``$GEOSERVER_DATA_DIR/logs`` folder.
The name of the file will become the configuration name displayed in the admin console and the contents will drive the specific behavior of the logger.

Here is an example, taken from the ``GEOTOOLS_DEVELOPER_LOGGING`` configuration, which enables the geotools log messages to appear in the logs::

    log4j.rootLogger=WARN, geoserverlogfile, stdout
    
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%d{dd MMM HH:mm:ss} %p [%c] - %m%n
    
    log4j.category.log4j=FATAL
    
    log4j.appender.geoserverlogfile=org.apache.log4j.RollingFileAppender
    # Keep three backup files.
    log4j.appender.geoserverlogfile.MaxBackupIndex=3
    # Pattern to output: date priority [category] - message
    log4j.appender.geoserverlogfile.layout=org.apache.log4j.PatternLayout
    log4j.appender.geoserverlogfile.layout.ConversionPattern=%d %p [%c] - %m%n
    
    
    log4j.category.org.geotools=TRACE
    # Some more geotools loggers you may be interest in tweaking
    log4j.category.org.geotools.factory=TRACE
    log4j.category.org.geotools.renderer=DEBUG
    log4j.category.org.geotools.data=TRACE
    log4j.category.org.geotools.feature=TRACE
    log4j.category.org.geotools.filter=TRACE
    log4j.category.org.geotools.factory=TRACE
    
    log4j.category.org.geoserver=INFO
    log4j.category.org.vfny.geoserver=INFO
    
    log4j.category.org.springframework=WARN

Any custom configuration can be setup to enable specific packages to emit logs at the desired logging level.
There are however a few rules to follow:

* the configuration should always include a ``geoserverlogfile`` appender that GeoServer will configure to work against the location configured in the :ref:`config_globalsettings`
* a logger writing to the standard output should be called ``stdout`` and again GeoServer will enable/disable it according to the configuration set in the :ref:`config_globalsettings`
* it is advisable, but not require, to setup log rolling for the ``geoserverlogfile`` appender

Overriding the log location setup in the GeoServer configuration
----------------------------------------------------------------

When setting up a cluster of GeoServer machines it is common to share a single data directory among all the cluster nodes.
There is however a gotcha, all nodes would end up writing the logs in the same file, which would cause various kinds of troubles depending on the operating system file locking rules (a single server might be able to write, or all togheter in an uncontrolled manner resulting in an unreadable log file).

In this case it is convenient to set a separate log location for each GeoServer node by setting  the following parameter among the JVM system variables, enviroment variables, or servlet context parameters::

  GEOSERVER_LOG_LOCATION=<the location of the file>
  
A common choice could be to use the machine name as a distinction, setting values such as  ``logs/geoserver_node1.log``, ``logs/geoserver_node2.log`` and so on: in this case all the log files would still be contained in the data directory and properly rotated, but each server would have its own separate log file to write on.

Forcing GeoServer to relinquish Log4J control
---------------------------------------------

GeoServer internally overrides the Log4J configuration by using the current logging configuration as a template and appling the log location and standard output settings configured by the administrator.

If you wish GeoServer not to override the normal Log4J behavior you can set the following parameter among the JVM system variables, enviroment variables, or servlet context parameters::

  RELINQUISH_LOG4J_CONTROL=true
  
Forcing GeoServer to use an alternate logging redirection
---------------------------------------------------------

GeoServer uses the GeoTools logging framework, which in turn is based on Java Logging, but allowing to redirect all message to an alternate framework of users choice.

By default GeoServer setups a Log4J redirection, but it is possible to configure GeoServer to use plain Java Logging or Commons Logging instead (support for other loggers is also possible by using some extra programming).

If you wish to force GeoServer to use a different logging mechanism set the following parameters among the JVM system variables, enviroment variables, or servlet context parameters::

  GT2_LOGGING_REDIRECTION=[JavaLogging,CommonsLogging,Log4J]
  RELINQUISH_LOG4J_CONTROL=true
  
As noted in the example you'll also have to demand that GeoServer does not exert control over the Log4J configuration