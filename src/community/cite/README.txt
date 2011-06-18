CITE Testing Instructions
=========================

Building the Engine
-------------------

 To build the test ending execute the command `mvn clean install` 

Running a Test Suite
--------------------

 The `run.sh` script is used to execute a test suite:

    ./run.sh <service>-<version>

 For example:

    ./run.sh wfs-1.1.0
    ./run.sh wms-1.1.1
  
Running Headless
----------------

 During normal execution of a test suite the user will be prompted with a form 
 to fill out all the test parameters such as the location of the server under
 test. However the engine can be run in a headless mode as well.

 Executing `run.sh` with the flag `-h` engages headless mode. When running 
 headless will use existing xml files that contain all the test suite 
 parameters. The name of the xml file is <service>-<version>.xml.

 For example, running:

   ./run.sh wms-1.1.1 -h 

 Will use the file `wms-1.1.1.xml`, located in the current directory, for the 
 test suite parameters. These xml files may be edited to modify the test suite
 setup.

Viewing Test Results
--------------------

 The `log.sh` file can be used to view the log for a test run:

    ./log.sh <service>-<version> [testid]

 When the `testid` parameter is ommitted the entire test log is output. When it
 is specified only the log for that specifc test is output. Example:

    ./log.sh wfs-1.0.0 wfs-1.0.0/w24aac25b3b9d185b1_1 

Running TEAMEngine Webapp
-------------------------

 While the `log.sh` can output raw information about a test run using it for 
 test debugging is tedious. The TEAMEngine web application provides a nicer
 interface. 

 To start the TEAMEngine webapp:

   cd engine
   mvn jetty:run-exploded

 The above command will start a jetty container on port 7070. The webapp is 
 then available at http://localhost:7070/teamengine . The port can be changed
 by modifying the `engine/pom.xml` file.

 To log into the teamengine use the username "geoserver" and the password 
 "geoserver". This default password can be changed by modifying the 
 `engine/realm.properties` file.

Cleaning Up
-----------

 The user data for each test run is stored under the `users/geoserver` 
 directory. This directory contains directories named `<service>-<version>` that
 correspond to the various service test suites. To remove data for a test run
 simply remove the corresponding directory.

 *Note*: For a single service and version under test only data for one test run
 is maintained. Which means if you execute a test suite twice consecutively the 
 second run will use the configuration and results of the first test run. 
 Therefore it is important to clean out the test run directories in order to 
 ensure a "clean" run.
