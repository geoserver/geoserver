---
render_macros: true
---

# Data directory default location

If GeoServer is running in **standalone** mode (via an installer or a binary) the data directory is located at `<installation root>/data_dir`.

| Standalone platform | Default/typical location                                     |
|---------------------|--------------------------------------------------------------|
| Windows (except XP) | C:\\Program Files\\GeoServer {{ release }}\\data_dir         |
| Windows XP          | C:\\Program Files\\GeoServer {{ release }}\\data_dir         |
| Mac OS X            | /Applications/GeoServer.app/Contents/Resources/Java/data_dir |
| Linux (Tomcat)      | /var/lib/tomcat9/webapps/geoserver/data                      |

If GeoServer is running as a **web archive** inside of a custom-deployed application server, the data directory is by default located at `<web application root>/data`.

## Creating a new data directory

The easiest way to create a new data directory is to copy an existing one.

Once the data directory has been located, copy it to a new location. To point a GeoServer instance at the new data directory proceed to the next section [Setting the data directory location](setting.md).
