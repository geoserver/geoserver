---
render_macros: true
---

# Installing Catalog Services for Web (CSW) {: #csw_installing }

The CSW extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Download the `csw`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer or the servlet container, as appropriate to your configuration.

4.  Verify that the module was installed correctly by navigating to the Welcome page of the [Web administration interface](../../webadmin/index.md) and seeing the **CSW** entry is listed in the **Service Capabilities** list, and the CSW modules are listed in the [Web administration interface](../../webadmin/index.md) Module list.

![](images/install.png)
*CSW Installation Verification*
