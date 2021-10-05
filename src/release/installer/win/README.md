# GeoServer installer for Windows

This folder contains the required files to create a Windows setup (.exe) for GeoServer.
The installer is built using the [NSIS](https://nsis.sourceforge.io/) script `geoserver_winsetup.nsi` in the `build` directory.

Besides the NSIS compiler (`makensis.exe`), you will need the following things to build an installer:
- `AccessControl` and `ShellLink` plugins for NSIS. These DLLs are included in the `nsis_plugins` folder.
- `signtool.exe`: required to digitally sign the installer. This app is included in the Windows SDK. If you wish to build an installer yourself, you need to have a digital signing certificate (e.g. `pfx` file). If you don't wish to sign the installer, you will have to manually remove the related code from the `geoserver_winsetup.nsi` script.
- In order to install and run GeoServer as a Windows service, a Java wrapper application is required. Here we have used the [Java Service Launcher](https://roeschter.de/) (JSL), included in the `build/wrapper` folder. For more information about the wrapper, please have a look at the `README.md` file in that folder.

The `Dockerfile` included in this directory is used to create a Windows Docker image that can also build the installer.
An Azure container using this image has been deployed to the Azure Container Registry (ACR) and is currently being used to build and deploy the Windows installer to SourceForge as part of the GeoServer CI/CD pipeline.
