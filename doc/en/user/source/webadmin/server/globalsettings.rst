.. _globalsettings:

Global Settings
================
The Global Setting page configures messaging, logging, character and proxy settings for the entire server.  

.. figure:: ../images/server_globalsettings.png
   :align: left
   
   *Global Settings Page*
   
Global Setting Fields
---------------------
**Verbose Messages:**  When enabled, Verbose Messages tells GeoServer to return XML with newlines and indents.  Because such XML responses contain a larger amount of data, and in turn requires a larger amount bandwidth we recommended this option only for testing purposes.  

**Verbose Exception Reporting:**
Instead of the one line error message, enabled Verbose Exception Reporting returns service exceptions with full Java stack traces.  Verbose exception reporting writes to the GeoServer log file and offers one of the most useful configuration options for debugging. 

**Number of Decimals:**
Refers to the number of decimal places returned in a GetFeature response.  Also useful in optimizing bandwidth.

*Character Set:**
Specifies the global character encoding that will be used in XML responses. We recommend the default UTF-8 for most users but support all character sets listed on the `IANACharset Registry <http://www.iana.org/assignments/character-sets>`_, and have an available Java implementation. 

**Proxy Base URL:**
GeoServer can have the capabilities documents properly report a proxy.  The Proxy Base URL field is the base URL seen beyond a reverse proxy.

**Logging Profile:**
Corresponds to a log4j configuration file in GeoServer's data directory. (Apache `log4j <http://logging.apache.org/log4j/1.2/index.html>`_ is a Java-based logging utility.)  By default, there are five logging profiles set in GeoServer's configurations file; customized profiles can be added by editing the log4j file. 

There are six logging levels used by log.  They range from the least serious TRACE, through DEBUG, INFO, WARN, ERROR and finally the most serious, FATAL.  The GeoServer logging profiles combine logging levels with specific server operations.  The five pre-built logging profiles available on the global settings page are:
 
#. *Default Logging* provides a good mix of detail without being VERBOSE.  Default logging enables INFO on all GeoTools and GeoServer levels, except certain (chatty) GeoTools packages which require WARN. 
#. *Verbose Logging* provides much more detail by enables DEBUG level logging on GeoTools, GeoServer, and VFNY.
#. *Production Logging* is the most minimal logging profile, with only WARN enabled on all GeoTools and GeoServer levels.  With such production level logging only problems are written to the log files.
#. *GeoTools Developer Logging* is a verbose logging profile that includes DEBUG information only on GeoTools.  This developer profile is recommended for active debugging of GeoTools.
#. *GeoServer Developer Logging* is a verbose logging profile that includes DEBUG information on GeoServer and VFNY.  This developer profile is recommended for active debugging of GeoServer.

**Log to StdOut:**
In general, StdOut (Standard output) refers to where a program writes its output data. In GeoServer, the Log to StdOut checkbox enables logging to the text terminal which initiated the program, most often the console. If you are running GeoServer in a large J2EE container, you might not want your container-wide logs filled with GeoServer information. Un-checking this option will suppress most GeoServer logging, with only fatal exceptions still outputted to the console log.

**Log Location**
Sets the written output location for the logs. A log location may be a directory or a file, and can be specified as an absolute path (e.g., :file:`C:\GeoServer\GeoServer.log`) or a relative one (e.g., :file:`GeoServer.log`).  Relative paths are relative to the GeoServer data directory. 
     
     
     
     
     
     
     
     
     
     
     