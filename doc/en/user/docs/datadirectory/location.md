---
render_macros: true
---

---
render_macros: true
---

# Data directory location

The current data directory location is always shown on the [Status](../configuration/status.md) page.

![](configuration/img/server_status.png)
*Status Page (default tab)*

## Default data directory location

By default GeoServer includes an example data directory allowing you to try out the application quickly:

- Platform Independent Binary: The data directory is located at **`<installation root>/data_dir`**.

| Platform | Default location                               |
|----------|------------------------------------------------|
| Linux    | **`/usr/share/geoserver/data_dir`**          |
| Windows  | **`C:\Program Files\GeoServer\data_dir`** |

<!-- mkdocs-translate: removed 2 spaces indentation -->

> The windows **`Program Files`** location above is not ideal due to restrictions placed on this location.

- Web archive: If GeoServer is running as a **web archive** inside of your application server, the data directory is by default located at **`<web application root>/data`**`.

| Platform | Default location |
|----|----|
| Linux (Tomcat) | **`/var/lib/tomcat9/webapps/geoserver/data`** |
| Windows (Tomcat) | **`C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\geoserver\data`** |

<!-- mkdocs-translate: removed 2 spaces indentation -->

- Windows Installer: The windows installer unpacks the data directory to **`%PROGRAMDATA%\GeoServer`**:

| Platform            | Default location                 |
|---------------------|----------------------------------|
| Windows (Installer) | **`%ProgramData%\GeoServer`** |

<!-- mkdocs-translate: removed 2 spaces indentation -->

- Docker: The Docker image maintains a data directory in **``**/opt/geoserver_data``.

  This location should be mapped to an absolute path in your host as described in [Using your own Data Directory](../installation/docker.md#installation_docker_data).

## External data directory location

To make [upgrading](../installation/upgrade.md) easier, **Web Archive** users should switch to an *external* data directory (outside the application).

| Platform | Example location                                            |
|----------|-------------------------------------------------------------|
| Linux    | [|data_directory_linux|](##SUBST##|data_directory_linux|) |
| Windows  | [|data_directory_win|](##SUBST##|data_directory_win|)     |
| MacOS    | [|data_directory_mac|](##SUBST##|data_directory_mac|)     |

## Creating a new data directory

To create a new data directory:

- The easiest way to create a new data directory is to copy an existing default data directory above.

  Once the data directory has been located, copy it to a new location. To point a GeoServer instance at the new data directory proceed to the next section [Setting the data directory location](setting.md).

- You may download the sample data directory.

  Navigate to the [GeoServer Download page](https://geoserver.org/download), select a version of GeoServer, and download the provided data directory zip.

  !!! abstract "Nightly Build"


  For GeoServer {{ version }} Nightly: [data](https://geoserver.org/release/stable/data)

  !!! abstract "Release"


  For GeoServer {{ release }}: [data](https://geoserver.org/release/stable/data)

- You may also use a new empty folder as the data directory location.

  GeoServer will create configuration files and folders as needed.
