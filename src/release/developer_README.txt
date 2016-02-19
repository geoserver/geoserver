For a comprehensive guide for developers, go the main documentation site: 
http://docs.geoserver.org/latest/en/developer/

---------------------------

1) Install Java Development Kit (JDK) 8 or greater.

* http://www.oracle.com/technetwork/java/javase/downloads/index.html
* http://openjdk.java.net

Create an environment variable called JAVA_HOME and point it to your Java JDK directory.
Then modify the PATH variable and add: ;%JAVA_HOME%/bin
Apply the changes.

2) Download and install Git

* http://git-scm.com/

3) Clone the GeoServer repository

Using Git Clone, get the source code:

git clone git://github.com/geoserver/geoserver.git geoserver

4) Download and install Maven

* http://maven.apache.org/

If you are using Linux, ensure maven is included on your path:

export M2_HOME=/usr/java/maven-3.0.5
export PATH=$PATH:$M2_HOME/bin


5) Build Source Code

Go to the command line and navigate to the root of the source tree that you just downloaded.

cd geoserver/src

Execute the command:

mvn clean install

If it fails, just try again. It trys to download jars and some might not be available at that time. So just keep trying.

If it succeeds, run the next command:

mvn eclipse:eclipse

6) Set up Eclipse

Got to: Windows -> Preferences
In the wondow that pops up click on Java -> Build Path -> Classpath Variables
On the Classpath Variables panel, select New
Define a new variables called M2_REPO and set it to your local maven repository. (for windows it would be C:/Documents and Settings/username/.m2/repository)

7) Get the Code into Eclipse

Import existing projects into the workspace, use the root of your geoserver source tree.
Select all of the modules. Hit Finish.

8) Run

From the Package Explorer select the web-app module.

Navigate to the org.geoserver.web package

Right-click the Start class and navigate to Run as, Java Application.

After running the first time you can return to the Run Configurations dialog to fine tune your launch environment (including setting a GEOSERVER_DATA_DIRECTORY).
