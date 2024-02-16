---
render_macros: true
---

# Installing the GeoServer MBStyle extension {: #mbstyle_install }

The MBStyle extension is listed on the GeoServer download page.

To install MBStyle extension:

1.  Download the `mbstyle`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. This extension includes two jars.

3.  Restart GeoServer.

4.  To confirm successful installation, check for a new `MBStyle` format option in the [Styles](../webadmin/index.md) editor.
