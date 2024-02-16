---
render_macros: true
---

# Installing the Smart Data Loader extension {: #smart_data_loader_install }

The Smart Data Loader extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Download the Smart Data Loader nightly GeoServer community module from `smart-data-loader`{.interpreted-text role="download_community"}.

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Make sure you have downloaded and installed the `app-schema`{.interpreted-text role="download_extension"} extension.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

4.  Restart GeoServer.

If installation was successful, you will see a new Smart Data Loader entry in the "new Data Source" menu.

![](images/store-selection.png)
*Smart Data Loader entry*
