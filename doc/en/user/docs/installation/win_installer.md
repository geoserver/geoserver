# Windows installer {#installation_windows_installer}

The Windows installer provides an easy way to set up GeoServer on your system, as it requires no configuration files to be edited or command line settings.

1.  Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 11** or **Java 17** environment, as provided by [Adoptium](https://adoptium.net) Windows installers.

    !!! note

        For more information about Java and GeoServer, please see the section on [Java Considerations](../../user/production/java.md).


2.  Navigate to the `GeoServer Download page <download>`{.interpreted-text role="website"}.

3.  Select the version of GeoServer that you wish to download. If you're not sure, select `Stable <release/stable>`{.interpreted-text role="website"} release.

    !!! abstract "Nightly Build"

        This documentation covers GeoServer -SNAPSHOT which is under development and is available as a `Nightly <release/main>`{.interpreted-text role="website"} release.
    
        Nightly releases are used to test out try out new features and test community modules and do not provide a windows installer. When GeoServer .0 is released a windows installer will be provided.


    !!! abstract "Release"

        These instructions are for GeoServer .


4.  Click the link for the :**Windows Installer**.

    <figure>
    <img src="images/win_download.png" alt="images/win_download.png" />
    <figcaption>Downloading the Windows installer</figcaption>
    </figure>

5.  After downloading, double-click the file to launch.

6.  At the Welcome screen, click :**Next**.

    <figure>
    <img src="images/win_welcome.png" alt="images/win_welcome.png" />
    <figcaption>Welcome screen</figcaption>
    </figure>

7.  Read the [License](../../user/introduction/license.md) and click :**I Agree**.

    <figure>
    <img src="images/win_license.png" alt="images/win_license.png" />
    <figcaption>GeoServer license</figcaption>
    </figure>

8.  Select the directory of the installation, then click :**Next**.

    <figure>
    <img src="images/win_installdir.png" alt="images/win_installdir.png" />
    <figcaption>GeoServer install directory</figcaption>
    </figure>

9.  Select the Start Menu directory name and location, then click :**Next**.

    <figure>
    <img src="images/win_startmenu.png" alt="images/win_startmenu.png" />
    <figcaption>Start menu location</figcaption>
    </figure>

10. Enter the path to a **valid Java Runtime Environment (JRE)**. GeoServer requires a valid JRE in order to run, so this step is required. The installer will inspect your system and attempt to automatically populate this box with a JRE if it is found, but otherwise you will have to enter this path manually. When finished, click :**Next**.

    !!! note

        A typical path on Windows would be **`C:\\Program Files\\Java\\jre8`**.


    !!! note

        Don't include the **`\\bin`** in the JRE path. So if **`java.exe`** is located at **`C:\\Program Files (x86)\\Java\\jre8\\bin\\java.exe`**, set the path to be **`C:\\Program Files (x86)\\Java\\jre8`**.


    !!! note

        For more information about Java and GeoServer, please see the section on [Java Considerations](../../user/production/java.md).


    <figure>
    <img src="images/win_jre.png" alt="images/win_jre.png" />
    <figcaption>Selecting a valid JRE</figcaption>
    </figure>

11. Enter the path to your GeoServer data directory or select the default. If this is your first time using GeoServer, select the :**Default data directory`. When finished, click :guilabel:`Next**.

    <figure>
    <img src="images/win_datadir.png" alt="images/win_datadir.png" />
    <figcaption>Setting a GeoServer data directory</figcaption>
    </figure>

12. Enter the username and password for administration of GeoServer. GeoServer's [Web administration interface](../../user/webadmin/index.md) requires authentication for management, and what is entered here will become those administrator credentials. The defaults are :**admin / geoserver`. It is recommended to change these from the defaults. When finished, click :guilabel:`Next**.

    <figure>
    <img src="images/win_creds.png" alt="images/win_creds.png" />
    <figcaption>Setting the username and password for GeoServer administration</figcaption>
    </figure>

13. Enter the port that GeoServer will respond on. This affects the location of the GeoServer [Web administration interface](../../user/webadmin/index.md), as well as the endpoints of the GeoServer services such as [Web Map Service (WMS)](../../user/services/wms/index.md) and [Web Feature Service (WFS)](../../user/services/wfs/index.md). The default port is :**8080`, though any valid and unused port will work. When finished, click :guilabel:`Next**.

    <figure>
    <img src="images/win_port.png" alt="images/win_port.png" />
    <figcaption>Setting the GeoServer port</figcaption>
    </figure>

14. Select whether GeoServer should be run manually or installed as a service. When run manually, GeoServer is run like a standard application under the current user. When installed as a service, GeoServer is integrated into Windows Services, and thus is easier to administer. If running on a server, or to manage GeoServer as a service, select :**Install as a service`. Otherwise, select :guilabel:`Run manually`. When finished, click :guilabel:`Next**.

    <figure>
    <img src="images/win_service.png" alt="images/win_service.png" />
    <figcaption>Installing GeoServer as a service</figcaption>
    </figure>

15. Review your selections and click the :**Back` button if any changes need to be made. Otherwise, click :guilabel:`Install**.

    <figure>
    <img src="images/win_review.png" alt="images/win_review.png" />
    <figcaption>Verifying settings</figcaption>
    </figure>

16. GeoServer will install on your system.

    <figure>
    <img src="images/win_install_process.png" alt="images/win_install_process.png" />
    <figcaption>Install progress</figcaption>
    </figure>

17. When finished, click :**Finish** to close the installer.

    <figure>
    <img src="images/win_completing.png" alt="images/win_completing.png" />
    <figcaption>Completing</figcaption>
    </figure>

18. If you installed GeoServer as a service, it is already running. Otherwise, you can start GeoServer by going to the Start Menu, and clicking :**Start GeoServer** in the GeoServer folder.

19. Navigate to `http://localhost:8080/geoserver` (or wherever you installed GeoServer) to access the GeoServer [Web administration interface](../../user/webadmin/index.md).

    If you see the GeoServer Welcome page, then GeoServer is successfully installed.

    <figure>
    <img src="images/success.png" alt="images/success.png" />
    <figcaption>GeoServer Welcome Page</figcaption>
    </figure>

## Uninstallation

GeoServer can be uninstalled in two ways: by running the **`uninstall.exe`** file in the directory where GeoServer was installed, or by standard Windows program removal.
