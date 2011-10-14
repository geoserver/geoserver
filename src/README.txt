For a comprehensive guide for developers, go the main documentation site: 
http://geoserver.org/display/GEOSDOC/Developers+Guide

---------------------------

1) Install JAVA SDK
Download and install the Java SDK, Sun Java version 6.0 (1.6.0_XX)

Create an environment variable called JAVA_HOME and point it to your Java SDK directory.
Then modify the PATH variable and add: ;%JAVA_HOME%/bin
Apply the changes.


2) Download Subversion
http://subversion.tigris.org/getting.html
Install subversion.


3) Checkout the Source Code
Using SVN Checkout, get the source code:
svn checkout https://svn.codehaus.org/geoserver/trunk


4) Download and install Maven
As of 2009-04-28 GeoServer does not build with Maven 2.1.0. We therefore recommend 2.0.9. 
Windows: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.0.9.exe
Linux: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.0.9.zip


If you are using Linux, execute the following commands:
export M2_HOME=/usr/java/maven-2.0.9
export PATH=$PATH:$M2_HOME/bin


5) Build Source Code
Go to the command line and navigate to the root of the source tree that you just downloaded.
Execute the command:
mvn install

If it fails, just try again. It trys to download jars and some might not be available at that time. So just keep trying.

If it succeeds, run the next command:
mvn eclipse:eclipse


6) Set up Eclipse
http://www.eclipse.org/downloads/
Pick "Eclipse IDE for Java Developers" unless you plan on integrating GeoServer into J2EE applications


Install Eclipse.

Start up Eclipse.
Got to: Windows -> Preferences
In the wondow that pops up click on Java -> Build Path -> Classpath Variables
On the Classpath Variables panel, select New
Define a new variables called M2_REPO and set it to your local maven repository. (for windows it would be C:/Documents and Settings/username/.m2/repository)

7) Get the Code into Eclipse
Import existing projects into the workspace, use the root of your geoserver source tree.
Select all of the modules. Hit Finish.
