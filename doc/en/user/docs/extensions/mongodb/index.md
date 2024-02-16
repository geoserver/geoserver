---
render_macros: true
---

# MongoDB Data Store {: #mongodb }

This module provides support for MongoDB data store. This extension is build on top of [GeoTools MongoDB plugin](https://docs.geotools.org/latest/userguide/library/data/mongodb.html).

## Installation

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: `mongodb`{.interpreted-text role="download_extension"}

    The download link will be in the **Extensions** section under **Vector Formats**.

    !!! warning

        Make sure to match the version of the extension (for example {{ release }} above) to the version of the GeoServer instance!

2.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.

3.  Restart GeoServer

## Usage

If the extension was successfully installed a new type of data store named `MongoDB` should be available:

![](images/mongodb_store_1.png)
*MongoDB data store.*

Configuring a new MongoDB data store requires providing:

1.  The URL of a MongoDB database.
2.  The absolute path to a data directory where GeoServer will store the schema produced for the published collections.

![](images/mongodb_store_2.png)
*Configuring a MongoDB data store.*

For more details about the usage of this data store please check the [GeoTools MongoDB plugin documentation](https://docs.geotools.org/latest/userguide/library/data/mongodb.html).
