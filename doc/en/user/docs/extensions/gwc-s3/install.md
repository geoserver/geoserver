---
render_macros: true
---

# Installing the GWC S3 extension {: #gwc_s3_install }

The GWC S3 extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: `gwc-s3`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename (for example {{ release }} above) corresponds to the version of GeoServer you are running.

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer.

To verify the installation was successful, to "Tile Caching", "Blobstores" and create a new blobstore, the S3 option show be available:

![](img/newBlobstore.png)
*The S3 option showing while creating a new blobstore*
