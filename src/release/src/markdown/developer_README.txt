Welcome to GeoServer Development
================================

This README helps you getting started with GeoServer development. It will guide you through the process of checking out the source code, compiling it, and running.

GeoServer Development requires Java 8, Maven, and git. Due to subtle changes in Java class libraries we require development on Java 8 at this time (although the result is tested on Java LTS releases).

Further reading:
  https://docs.geoserver.org/latest/en/developer/index.html


Linux
-----

1. Install prerequisites

    Obtain OpenJDK 8, Maven and git from your Linux distribution.

macOS
-----

1. Install Java Runtime Environment

    Download and install Java 8, as provided by Adoptium macOS installers:
    https://adoptium.net

    Update your shell environment with:
   
    ```bash
    export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home
    ```
  
    The system ``/usr/bin/java`` makes use of ``JAVA_HOME`` setting above.

2. Download and install git:
   https://git-scm.com/download/mac

3. Download and install Maven:
   https://maven.apache.org/download.html

Windows
-------

1. Install Java Runtime Environment

    Download and install Java 8 runtime environment, as provided by Adoptium windows installers:
    https://adoptium.net

    Update Windows *Environment Variables* is handled by the installer:

    * ``JAVA_HOME`` points to your Java JDK directory.
    * PATH variable includes: ``;%JAVA_HOME%/bin``

2. Download and install git:
   https://git-scm.com/download/windows

3. Download and install Maven:
   https://maven.apache.org/download.html

OS independent tasks
--------------------

1. Get the source code

    Go to the command line and run:
   
    ```bash
    git clone https://github.com/geoserver/geoserver.git
    ```
   
2. Build the source code
   
    Navigate to the folder you just checked out. Now run:
   
    ```bash
    cd geoserver
    cd src
    mvn clean install
    ```
   
3. Running locally:

    ```bash
    cd web/app
    mvn jetty:run
    ```

4. For complete build instructions, including how to build extensions or run from an IDE, see the user guide:
   https://docs.geoserver.org/latest/en/developer/
   
Troubleshooting
---------------

Common build failures:

1. Unavailable dependencies - Maven tries to download dependencies which might not be available on the server side yet.

    Solution: Try again in a few minutes.

2. Failing tests - Maven runs existing tests automatically. If some of them fail, the build fails.

    Mitigation: You can ask maven not to run the tests. This is discouraged.
    Bug the developers instead or fix the test and send a patch, thanks!
  
    If you really just want to disable the test, run Maven like so:
  
    ```bash
    mvn -DskipTests=true install
    ```