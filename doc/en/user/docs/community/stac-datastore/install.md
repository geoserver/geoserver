---
render_macros: true
---

# Installing the STAC data store

The STAC store community module is listed among the other community modules on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Download the STAC store nightly GeoServer community module from {{ download_community('stac-datastore','snapshot') }}.

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer.

If installation was successful, you will see a new STAC datastore entry in the "new Data Source" menu.

![](images/store-selection.png)
*STAC datastore entry*
