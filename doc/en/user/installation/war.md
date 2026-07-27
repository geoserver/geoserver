---
render_macros: true
---


# Web archive

GeoServer is packaged as a standalone Web Archive (**`geoserver.war`**) file for use with existing application servers such as [Apache Tomcat](https://tomcat.apache.org/) and [Jetty](https://jetty.org/).

| JavaEE          | JakartaEE       | Tomcat        | Jetty      | GeoServer     |
| --------------- | --------------- | --------------| ---------- | ------------- |
|                 | Servlet API 6.1 | Tomcat 11.0.x | Jetty 12.1 | GeoServer 3   |
|                 | Servlet API 6.0 | Tomcat 10.1.x | Jetty 12.0 | not supported |
|                 | Servlet API 5.0 | Tomcat 10.0.x | Jetty 11.0 | not supported |
| Servlet API 4   |                 | Tomcat 9.x    |            | GeoServer 2   |
| Servlet API 3.1 |                 |               | Jetty 9.4  | GeoServer 2   |

GeoServer is tested using Tomcat 11.0.x, and this is the recommended application server. Other application servers have been known to work, but are not tested regularly by community members.

!!! note
    GeoServer 3 is compatible with Tomcat 11.0.x which provides Jakarta EE Servlet API 6.1.x and annotation processing.

!!! note
    GeoServer 2 is compatible with Tomcat 9.x which provides the required Java Enterprise Edition Servlet API 4 and annotation processing.

## Installation

1.  Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 17** or **Java 21** environment.

    **Linux**

    --8<--
    doc/en/user/installation/jdk-linux-guidance.txt
    --8<--

    **Windows**

    --8<--
    doc/en/user/installation/jdk-windows-guidance.txt
    --8<--

    **MacOS**

    --8<--
    doc/en/user/installation/jdk-macos-guidance.txt
    --8<--

    !!! note
        For more information about Java and GeoServer compatibility, please see the section on [Java Considerations](../production/java.md).

2.  Navigate to the [GeoServer Download page](https://geoserver.org/download).

3.  Select the version of GeoServer that you wish to download.

    - If you're not sure, select [Stable](https://geoserver.org/release/stable) release.

      Examples provided for GeoServer {{ release }}.

    - Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases change on an ongoing basis and are not suitable for a production environment.

      Examples are provided for GeoServer {{ version }}, which is provided as a [Nightly](https://geoserver.org/release/main) release.

4.  Select **Web Archive** on the download page:

    - [geoserver-{{ release }}-war.zip]({{ download_release }}war.zip)
    - [geoserver-{{ snapshot }}-war.zip]({{ nightly_release }}war.zip)

5.  Download and unpack the archive.

6.  Deploy the web archive as you would normally. Often, all that is necessary is to copy the **`geoserver.war`** file to the application server's **`webapps`** directory, and the application will be deployed by the application server.

    !!! note
        A restart of your application server may be necessary.

7.  In a web browser, navigate to <http://localhost:8080/geoserver>.

    When you see the GeoServer Welcome page, then GeoServer is successfully installed.

    ![](images/welcome_page.png)

    *GeoServer Welcome Page*

11. To shut down GeoServer, either close the persistent command-line window, or run the **`shutdown.sh`** file inside the **`bin`** directory.


### Additional Tomcat tutorials

Several additional tomcat tutorials are available:

 *  [Tomcat Hardening](../tutorials/tomcat-hardening/index.md) - Recommended
 *  [Tomcat JNDI](../tutorials/tomcat-jndi/tomcat-jndi.md)
   
