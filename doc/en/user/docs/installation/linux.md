---
render_macros: true
---

# Linux binary

!!! note

    For installing on Linux with an existing application server such as Tomcat, please see the [Web archive](war.md) section.


The platform-independent binary is a GeoServer web application bundled inside [Jetty](http://eclipse.org/jetty/), a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

## Installation

1.  Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 11** or **Java 17** environment, available from [OpenJDK](https://openjdk.java.net), [Adoptium](https://adoptium.net), or provided by your OS distribution.

    !!! note

        For more information about Java and GeoServer compatibility, please see the section on [Java Considerations](../production/java.md).


2.  Navigate to the [GeoServer Download page](https://geoserver.org/download).

3.  Select the version of GeoServer that you wish to download. If you're not sure, select [Stable](https://geoserver.org/release/stable) release.

    !!! abstract "Nightly Build"

        These instructions are for GeoServer {{ version }}-SNAPSHOT which is provided as a [Nightly](https://geoserver.org/release/main) release. Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases change on an ongoing basis and are not suitable for a production environment.


    !!! abstract "Release"

        These instructions are for GeoServer {{ release }}.


4.  Select **Platform Independent Binary** on the download page: [geoserver-{{ release }}-bin.zip](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/geoserver-{{ release }}-bin.zip)

5.  Download the **`zip`** archive and unpack to the directory where you would like the program to be located.

    !!! note

        A suggested location would be **`/usr/share/geoserver`**.


6.  Add an environment variable to save the location of GeoServer by typing the following command:

    ``` bash
    echo "export GEOSERVER_HOME=/usr/share/geoserver" >> ~/.profile
    . ~/.profile
    ```

7.  Make yourself the owner of the `geoserver` folder. Type the following command in the terminal window, replacing `USER_NAME` with your own username :

    ``` bash
    sudo chown -R USER_NAME /usr/share/geoserver/
    ```

8.  Start GeoServer by changing into the directory `geoserver/bin` and executing the `startup.sh` script:

    ``` bash
    cd geoserver/bin
    sh startup.sh
    ```

9.  In a web browser, navigate to `http://localhost:8080/geoserver`.

    If you see the GeoServer Welcome page, then GeoServer is successfully installed.

    ![](images/success.png)
    *GeoServer Welcome Page*

10. To shut down GeoServer, either close the persistent command-line window, or run the **`shutdown.sh`** file inside the **`bin`** directory.

## Uninstallation

1.  Stop GeoServer (if it is running).
2.  Delete the directory where GeoServer is installed.
