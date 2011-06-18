CITE Testing Instructions
=========================

Building the Engine
-------------------

1. Checkout Engine Sources

Create a directory called 'engine', and check the out the teamengine sources
into it.

  'mkdir engine'
  'svn co -r 433 http://teamengine.svn.sourceforge.net/svnroot/teamengine/branches/team2 engine'
  
*Note*: Revision 443 is the last verified version of the engine.

2. Checkout Test Sources

Create a directory called 'tests', and check out the cite test sources into
it.

*Note*: You need an account to access the test sources. If you have an OGC
        portal account that will work. If not simply ask on the developer
        list and someone will send you a copy of the tests.

  'mkdir tests'
  'svn co -r 2740 https://svn.opengeospatial.org:8443/ogc-projects/cite/scripts/wfs/1.0.0/trunk tests/wfs-1.0.0'
  'svn co -r 2740 https://svn.opengeospatial.org:8443/ogc-projects/cite/scripts/wfs/1.1.0/trunk tests/wfs-1.1.0'
  'svn co -r 2740 https://svn.opengeospatial.org:8443/ogc-projects/cite/scripts/wms/1.1.1/trunk tests/wms-1.1.1'
  'svn co -r 2740 https://svn.opengeospatial.org:8443/ogc-projects/cite/scripts/wcs/1.0.0/trunk tests/wcs-1.0.0'
  'svn co -r 2740 https://svn.opengeospatial.org:8443/ogc-projects/cite/scripts/wcs/1.1.1/trunk tests/wcs-1.1.1'
  'svn co https://svn.opengeospatial.org:8443/ogc-projects/cite/scripts/wms/1.3.0/trunk tests/wms-1.3.0'

*Note* : Revision 2740 is the last verified version of the tests.

3. Checkout the CITE1 Component

  'svn co https://svn.opengeospatial.org:8443/ogc-projects/cite/components/cite1/trunk cite1'

4. Patch the Test Engine Sources

Patch the engine sources with the 'engine.patch' file.

   'patch -p0 < engine.patch'

5. Patch the Test Sources

Patch the test sources with the 'tests.patch' file.

   'patch -p0 < tests.patch'

6. Build the Test Engine

The test engine is built with the following command: 

  'mvn clean install'

Running the Engine with Jetty
-----------------------------

Change to the 'engine' directory and run the 'mvn jetty:run-exploded' command:

  'cd engine'
  'mvn jetty:run-exploded'

Navigate to http://localhost:9090/teamengine in a web browser.

Running the Engine from the Command Line
----------------------------------------

Running a test suite is done with the command:

  'run.sh <testsuite>'

Where testsuite is one of the following:

  'wfs-1.0.0'
  'wfs-1.1.0'
  'wms-1.1.1'
  'wcs-1.0.0'
  'wcs-1.1.1'

Examples:

  'run.sh wfs-1.0.0'

E. View the Test Logs

Viewing the entire log of a test run is done with the command:

  'log.sh <profile>'

Viewing the log of a single test is done with the command:

  'log.sh <profile> <testid>'

Where testid is the identifer for the test. For example:

  'log.sh wfs-1.0.0 wfs-1.0.0/w24aac25b3b9d185b1_1'

F. Note for Windows Users

At this time, run.sh and log.sh have yet to ported to batch files. Windows
users must use the batch files included with the test engine. 

The equivalent to 'run.sh <profile>':

  'engine/bin/test.sh 
      -logdir=target/logs
      -source=tests/<service>/<version>/ets/ctl/[main.xml|<service>.xml] 
      -session=<profile>' 

Where service and version are the service and version being tested
respectively. Examples:

  'engine/bin/test.sh
      -logdir=target/logs
      -source=tests/wfs/1.0.0/ets/ctl/wfs.xml
      -session=wfs-1.0.0'

  'engine/bin/test.sh
      -logdir=target/logs
      -source=tests/wfs/1.1.0/ets/ctl/main.xml
      -session=wfs-1.1.0'

The equivalent to 'log.sh <profile> [<test>]':

  'engine/bin/viewlog.sh -logdir=target/logs -session=<profile> [<test>]'

Example:

  'engine/bin/viewlog.sh -logdir=target/logs -session=wfs-1.0.0 wfs-1.0.0/w24aac25b3b9d185b1_1' 


