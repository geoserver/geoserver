---
render_macros: true
---

# Installing the GeoServer YSLD extension {: #ysld_install }

The YSLD extension is listed on the GeoServer download page.

To install:

1.  Download the `ysld`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer.

4.  To confirm successful installation, check for a new YSLD entry in the [Styles](../webadmin/index.md) editor.
