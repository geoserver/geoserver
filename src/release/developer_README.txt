For a comprehensive guide for developers, go the main documentation site: 
http://docs.geoserver.org/latest/en/developer/

---------------------------

1) Install JAVA SDK
Download and install the Java SDK.

Create an environment variable called JAVA_HOME and point it to your Java SDK directory.
Then modify the PATH variable and add: ;%JAVA_HOME%/bin
Apply the changes.


2) Download Subversion
Windows: http://subversion.tigris.org/files/documents/15/29065/svn-1.3.0-setup.exe
Linux: http://subversion.tigris.org/project_packages.html

Install subversion.


3) Checkout the Source Code
Using SVN Checkout, get the source code:
svn checkout https://svn.codehaus.org/geoserver/trunk


4) Download and install Maven
Windows: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.0.4.exe
Linux: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.0.4.zip


If you are using Linux, execute the following commands:
export M2_HOME=/usr/java/maven-2.0.4
export PATH=$PATH:$M2_HOME/bin


5) Build Source Code
Go to the command line and navigate to the root of the source tree that you just downloaded.
Execute the command:
mvn install

If it fails, just try again. It trys to download jars and some might not be available at that time. So just keep trying.

If it succeeds, run the next command:
mvn eclipse:eclipse


6) Set up Eclipse
Windows: http://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops/R-3.1.2-200601181600/eclipse-SDK-3.1.2-win32.zip
Linux GTK: http://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops/R-3.1.2-200601181600/eclipse-SDK-3.1.2-linux-gtk.tar.gz
Linux Motif: http://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops/R-3.1.2-200601181600/eclipse-SDK-3.1.2-linux-motif.tar.gz

Install Eclipse.

Start up Eclipse.
Got to: Windows -> Preferences
In the wondow that pops up click on Java -> Build Path -> Classpath Variables
On the Classpath Variables panel, select New
Define a new variables called M2_REPO and set it to your local maven repository. (for windows it would be C:/Documents and Settings/username/.m2/repository)

7) Get the Code into Eclipse
Import existing projects into the workspace, use the root of your geoserver source tree.
Select all of the modules. Hit Finish.

