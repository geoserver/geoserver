---
render_macros: true
---

# Installing the GeoServer CSS extension {: #css_install }

The CSS extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Download the `css`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer.

If installation was successful, you will see a new CSS entry in the [Styles](../webadmin/index.md) editor.

![](images/css_style_format.png)
*CSS format in the new style page*

After installation, you may wish to read the tutorial: [Styling data with CSS](tutorial.md).
