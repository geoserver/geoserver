# GeoServer Source Code

This README helps you getting started with GeoServer development. It will guide you through the process of checking out the source code, compiling it, and running.

GeoServer Development requires Java 11, Maven, and git.


Further readings:
  https://docs.geoserver.org/latest/en/developer/index.html

## Linux

1. Install prerequisites

   Obtain OpenJDK 11, Maven and git from your Linux distribution.

## macOS

1. Install Java Runtime Environment

   Download and install Java 11 runtime environment, as provided by adoptium macOS installers:
   https://adoptium.net/temurin/archive/?version=11

   Update your shell environment with:

       export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptiumjdk-11.jdk/Contents/Home
  
   The system /usr/bin/java makes use of JAVA_HOME setting above.

2. Download and install git:

   https://git-scm.com/download/mac

3. Download and install Maven:

   https://maven.apache.org/download.html

## Windows

1. Install Java Runtime Environment

   Download and install Java 11 runtime environment, as provided by Adoptium windows installers:
   https://adoptium.net

   Update Windows *Environment Variables*:

   * Create an environment variable JAVA_HOME and point it to your Java JDK directory.
   * Modify the PATH variable and add: ;%JAVA_HOME%/bin

2. Download and install git:

   https://git-scm.com/download/windows

3. Download and install Maven:

   https://maven.apache.org/download.html

OS independent tasks
--------------------

1. Get the source code

   Go to the command line and run:
   
       git clone https://github.com/geoserver/geoserver.git

2. Build the source code
   
   Go to the command line and navigate to the folder you just checked out. Now run:
   
       cd geoserver
       cd src
       mvn clean install

3. Running locally:

       cd web/app
       mvn jetty:run

Troubleshooting
---------------

The build process may fail because of several reasons:

* Unavailable dependencies - Maven tries to download dependencies which might not be available on the server side yet.

  Solution: Try again in a few minutes.


* Failing tests - Maven runs existing tests automatically. If some of them fail, the build fails.

  Solution: You can tell Maven not to run the tests. This is discouraged.

  Bug the developers instead or fix the test and send a patch, thanks!
  
  If you really just want to disable the test, run maven like so:
  
    mvn -DskipTests=true install
