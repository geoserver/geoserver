Running The GeoServer Binary Distribution
=========================================

The binary distribution of GeoServer comes with a built-in Jetty servlet
container so that it can be run directly without the need for additional
software.  For information on other distributions on GeoServer, please
see the website:

https://geoserver.org

Reference:

* https://docs.geoserver.org/latest/en/user/installation

In order to install and run this application, follow these steps:

1. Download and install Java:

    * Download Java 11 or Java 17 from:

        https://adoptium.net
      
        or:
      
        http://openjdk.java.net/

    * Install according to the instructions included with the release.

    * Set an environment variable ``JAVA_HOME`` to the pathname of the directory
      into which you installed Java.

2. Download and install the GeoServer binary 

    * Download a binary distribution of GeoServer from:

        https://geoserver.org/download/
     
        or:
     
        https://sourceforge.net/projects/geoserver

    * The file should be of the form:

        geoserver-a.b.c-bin.zip

        Where a.b.c is the version number you are downloading.

    * Unpack the binary distribution into a convenient location so that the
      distribution resides in its own directory (conventionally named
      ``geoserver``).  For the purposes of the remainder of this document,
      the symbolic name ``GEOSERVER_HOME`` is used to refer to the directory
      where GeoServer resides.

    There is more help available at the GeoServer home page: https://geoserver.org

3. Start Up GeoServer

    There are two techniques by which GeoServer can be started:

    Use an environment variable:
   
    * Set an environment variable ``GEOSERVER_HOME`` to the path of the directory
      into which you have installed GeoServer.
     
    * Windows terminal command:
     
        ```bash
        %GEOSERVER_HOME%\bin\startup
        ```
   
    * Linux shell command:
   
        ```bash
        $GEOSERVER_HOME/bin/startup.sh
        ```

    Modify your current working directory:
   
    * Windows terminal command: 
     
        ```bash
        cd %GEOSERVER_HOME%\bin
        startup
        ```    
     
    * Unix shell command:
      
        ```bash
        cd $GEOSERVER_HOME/bin
        ./startup.sh
        ```

    After startup, the default web administration tool included with GeoServer 
    will be available by browsing:
   
    http://localhost:8080/geoserver

4. Shut Down GeoServer

    There are two techniques by which GeoServer can be stopped:

    Use an environment variable:
    
    * Set an environment variable ``GEOSERVER_HOME`` to the path of the directory
      into which you have installed GeoServer.
      
    * Windows terminal command:
      
        ```bash
        %GEOSERVER_HOME%\bin\shutdown
        ```

    * Linux shell command:
    
        ```bash
        $GEOSERVER_HOME/bin/shutdown.sh
        ```

    Modify your current working directory:
    
    * Windows terminal command:
      
        ```bash
        cd %GEOSERVER_HOME%\bin
        shutdown
        ```

    * Linux shell command:
    
        ```bash
        cd $GEOSERVER_HOME/bin
        ./shutdown.sh
        ```

5. Troubleshooting

    There are two common problems encountered when attempting to use the binary
    distribution of GeoServer:

    * Another web server (or other process) is already using port 8080.  This
      is the default HTTP port that GeoServer attempts to bind to at startup.
      
        To change this port, open the file: ``$GEOSERVER_HOME/start.ini``

        Search for ``jetty.port`` or ``8080``. Change this to a port that isn't
        in use, but is greater than ``1024`` (such as ``8090``).  Save this file and
        restart GeoServer.
      
        Access GeoServer on the new port: ``http://localhost:####`` (where #### is the new port).

    * The "localhost" address can't be found.  This could happen if you're
      behind a proxy.  If so, make sure the proxy configuration for your
      browser knows to not travel through the proxy to access the "localhost"
      address.  Please see your browser's documentation for how to check this.


6. Further information

    For more information about configuring and running GeoServer, please see the 
    GeoServer documentation:

        https://docs.geoserver.org
