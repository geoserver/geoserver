# GeoServer Source Code

This README helps you getting started with GeoServer development. It will guide you through the process of checking out the source code, compiling it, and running.

Further readings:

* https://docs.geoserver.org/latest/en/developer/index.html

## Development Environment

GeoServer Development requires Java 17, Maven, and git.

### SDKMan

[SDKMan](https://sdkman.io) helps manage and change between different development environments (available on Linux, macOS, and Windows WSL):

```bash
# list to determine latest Temurin JDK 17
sdk list java | grep "17.*-tem"

# Installing latest Temurin JDK 17 shown above
sdk install java 17.0.16-tem
sdk install maven
```

### Linux

Obtain OpenJDK 17, Maven and git from your Linux distribution:

```bash
sudo apt install openjdk-17-jdk 
sudo apt install git
sudo apt install maven
```

### macOS

1. Install Java Runtime Environment

   Download and install Java 17 runtime environment, as provided by adoptium macOS installers:
   https://adoptium.net/temurin/releases?version=17

   Update your shell environment with:
   
  ```bash
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptiumjdk-17.jdk/Contents/Home
  ```
  
  The system ``/usr/bin/java`` makes use of JAVA_HOME setting above.

2. Git is incuded with  XCode Command Line Tools.
   
   ```bash
   xcode-select –-install
   ```

   You may also download and install git:

   https://git-scm.com/download/mac

3. Download and install Maven:

   https://maven.apache.org/download.html

### Windows

1. Install Java Runtime Environment

   Download and install Java 17 runtime environment, as provided by Adoptium windows installers:
   https://adoptium.net/temurin/releases?version=17

   Update Windows *Environment Variables*:

   * Create an environment variable JAVA_HOME and point it to your Java JDK directory.
   * Modify the PATH variable and add: ;%JAVA_HOME%/bin

2. Download and install git:

   https://git-scm.com/downloads/win

3. Download and install Maven:

   https://maven.apache.org/download.html

## Building Locally

1. Get the source code

   Go to the command line and run:
   
   ```bash
   git clone https://github.com/geoserver/geoserver.git
   ```

2. Build the source code
   
   Go to the command line and navigate to the folder you just checked out. Now run:
   
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

Troubleshooting
---------------

The build process may fail because of several reasons:

### Unavailable dependencies

Maven tries to download dependencies which might not be available on the server side yet.

* Try again in a few minutes.

  GeoServer primarily uses repo.osgeo.org to cache dependencies in one location to avoid this previously common problem.
  
  
  * https://repo.osgeo.org/#browse/browse:release
  * https://repo.osgeo.org/#browse/browse:snapshot
  
* If maven has cached that the dependency is not available, you can use `-U` to force it to update dependencies
  
  ```bash
  mvn clean install -U
  ```

### Failing tests


Maven runs existing tests automatically. If some of them fail, the build fails.

* Try again: Some tests, like app-schema are not stable, you can try to ``-rf <module>` to resume the build from the point of failure:
  
  ```bash
  mvn clean install -rf :gs-app-schema
  ```
  
* Look at while the test is failing, and send a pull-request, thanks!
  
  Running test in a wide range of environments helps make GeoServer better. Please look at while
  the test is failing for you. 

* Discouraged: You can tell Maven not to run the tests.
  
  ```
  mvn -DskipTests=true install
  ```

