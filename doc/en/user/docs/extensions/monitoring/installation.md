---
render_macros: true
---

# Installing the Monitor Extension {: #monitor_installation }

!!! note

    If performing an upgrade of the monitor extension please see [Upgrading](../../community/monitor-hibernate/upgrade.md).

The monitor extension is not part of the GeoServer core and must be installed as a plug-in. To install:

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: `monitor`{.interpreted-text role="download_extension"}

    The download link will be in the **Extensions** section under **Other**.

    !!! warning

        Make sure to match the version of the extension (for example {{ release }} above) to the version of the GeoServer instance!

2.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.

3.  Restart GeoServer

## Verifying the Installation

There are two ways to verify that the monitoring extension has been properly installed.

1.  Start GeoServer and open the [Web administration interface](../../webadmin/index.md). Log in using the administration account. If successfully installed, there will be a **Monitor** section on the left column of the home page.

> ![](images/monitorwebadmin.png)
> *Monitoring section in the web admin interface*

1.  Start GeoServer and navigate to the current [GeoServer data directory](../../datadirectory/index.md). If successfully installed, a new directory named `monitoring` will be created in the data directory.
