# Windows binary {#installation_windows_bin}

!!! note

    For installing on Windows with an existing application server such as Tomcat, please see the [Web archive](../../user/installation/war.md) section.


The other way of installing GeoServer on Windows is to use the platform-independent binary. This version is a GeoServer web application bundled inside [Jetty](http://eclipse.org/jetty/), a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

## Installation

1.  Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 11** or **Java 17** environment, as provided by [Adoptium](https://adoptium.net) Windows installers.

    !!! note

        For more information about Java and GeoServer, please see the section on [Java Considerations](../../user/production/java.md).


2.  Navigate to the `GeoServer Download page <download>`{.interpreted-text role="website"}.

3.  Select the version of GeoServer that you wish to download. If you're not sure, select `Stable <release/stable>`{.interpreted-text role="website"} release.

    !!! abstract "Nightly Build"

        These instructions are for GeoServer -SNAPSHOT which is provided as a `Nightly <release/main>`{.interpreted-text role="website"} release. Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases change on an ongoing basis and are not suitable for a production environment.


    !!! abstract "Release"

        These instructions are for GeoServer .


4.  Select :**Platform Independent Binary` on the download page: :download_release:`bin**

5.  Download the archive and unpack to the directory where you would like the program to be located.

    !!! note

        A suggested location would be **`C:\\Program Files\\GeoServer`**.


### Setting environment variables

You will need to set the `JAVA_HOME` environment variable if it is not already set. This is the path to your JRE such that **`%JAVA_HOME%\\bin\\java.exe`** exists.

1.  Navigate to **Control Panel --> System --> Advanced --> Environment Variables**.
2.  Under :**System variables` click :guilabel:`New**.
3.  For :**Variable name` enter ``JAVA_HOME``. For :guilabel:`Variable value** enter the path to your JDK/JRE.
4.  Click OK three times.

!!! note

    You may also want to set the `GEOSERVER_HOME` variable, which is the directory where GeoServer is installed, and the `GEOSERVER_DATA_DIR` variable, which is the location of the GeoServer data directory (which by default is **`%GEOSERVER_HOME\\data_dir`**). The latter is mandatory if you wish to use a data directory other than the default location. The procedure for setting these variables is identical to setting the `JAVA_HOME` variable.


## Running

!!! note

    This can be done either via Windows Explorer or the command line.


1.  Navigate to the **`bin`** directory inside the location where GeoServer is installed.

2.  Run **`startup.bat`**. A command-line window will appear and persist. This window contains diagnostic and troubleshooting information. This window must be left open, otherwise GeoServer will shut down.

3.  Navigate to `http://localhost:8080/geoserver` (or wherever you installed GeoServer) to access the GeoServer [Web administration interface](../../user/webadmin/index.md).

    If you see the GeoServer Welcome page, then GeoServer is successfully installed.

    <figure>
    <img src="images/success.png" alt="images/success.png" />
    <figcaption>GeoServer Welcome Page</figcaption>
    </figure>

## Stopping

To shut down GeoServer, either close the persistent command-line window, or run the **`shutdown.bat`** file inside the **`bin`** directory.

## Uninstallation

1.  Stop GeoServer (if it is running).
2.  Delete the directory where GeoServer is installed.
