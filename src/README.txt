This README helps you getting started with GeoServer development.
It will guide you trough the process of checking out the source code, 
compiling it and optionally import it into eclipse.

== Linux ==

1) Install prerequisites

GeoServer needs a Java SDK, maven (version 3 is recommended) and git.
While compiling GeoServer with OpenJDK 6 and 7 works now, Sun's JDK 6 is still the recommendation.
Under Debian/Ubuntu simply run:

sudo apt-get install sun-java6-jdk maven git
or
sudo aptitude install sun-java6-jdk maven git

== Windows ==

1) Install Java SDK
Download and install the Java SDK, Sun Java version 6.0 (1.6.0_XX)
http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html#javasejdk

Create an environment variable called JAVA_HOME and point it to your Java SDK directory.
Then modify the PATH variable and add: ;%JAVA_HOME%/bin
Apply the changes.

2) Download and install git
(the github windows installer will take care of keeping the command line tools up to date for you):
http://windows.github.com

3) Download and install Maven 3
http://maven.apache.org/download.html

== OS independent tasks ==

1) Get the source code
Go to the command line and run:
git clone https://github.com/geoserver/geoserver.git

2) Build the source code
Go to the command line and navigate to the folder you just checked out. Now run:
mvn clean install

The build process may fail because of several reasons:

* Unavailable dependencies - Maven tries to download dependencies which might not be available on the server side yet.
Solution: Try again in some minutes.

* Failing tests - Maven runs existing tests automatically. If some of them fail, the build fails.
Solution: You can tell maven not to run the tests. This is discouraged.
Bug the developers instead or fix the test and send a patch, thanks!
If you really just want to disable the test, run maven like so:
$ mvn -DskipTests=true install

== Optional Tasks ==

1) Set up Eclipse
Go to http://www.eclipse.org/downloads/ and download "Eclipse IDE for Java Developers" 
unless you plan on integrating GeoServer into J2EE applications.

=== Windows ===
You need to let eclipse know where your maven repository is located.

Start Eclipse and go to: Windows -> Preferences
In the window that pops up click on Java -> Build Path -> Classpath Variables
On the Classpath Variables panel, select New
Now define a new variables called M2_REPO and set it to your local maven repository.
E.g."C:/Documents and Settings/username/.m2/repository"

=== Linux ===
You need to let eclipse know where your maven repository is located.
Do so by running the following command:
mvn -Declipse.workspace=<path-to-your-eclipse-workspace> eclipse:add-maven-repo


2) Import GeoServer as an eclipse project
Let maven create an eclipse project for you:
mvn eclipse:eclipse

After that, run eclipse and "Import existing projects into the workspace", use the root of your geoserver source tree.
Select all of the modules. Hit Finish.


== Further readings ==

Developer manual: http://docs.geoserver.org/stable/en/developer/index.html
