# GeoServer installer for Windows

This folder contains the required files to create a Windows setup (.exe) for GeoServer.
The installer is built using the [NSIS](https://nsis.sourceforge.io/) script `geoserver_winsetup.nsi` in the `build` directory.

Besides the NSIS compiler (`makensis.exe`), you will need the following things to build an installer:
- `AccessControl` and `ShellLink` plugins for NSIS. These DLLs are included in the `nsis_plugins` folder, but can also be obtained through the [NSIS plugins page](https://nsis.sourceforge.io/Category:Plugins).
- `signtool.exe`: required to digitally sign the installer. This app is included in the [Windows SDK](https://developer.microsoft.com/en-US/windows/downloads/windows-sdk/). If you wish to build an installer yourself, you need to have a digital signing certificate (e.g. `pfx` file). If you don't wish to sign the installer, you will have to manually remove the related code from the `geoserver_winsetup.nsi` script. **The script has been created to build official installers only.**
- In order to install and run GeoServer as a Windows service, a Java wrapper application is required. Here we have used the [Java Service Launcher](https://roeschter.de/) (JSL), included in the `build/wrapper` folder. For more information about the wrapper, please have a look at the `README.md` file in that folder.
- [Pandoc](https://pandoc.org/) is used to convert the `LICENSE.md` in the GeoServer binary into Rich Text Format (`.rtf`). This is required because NSIS does not support Markdown.


## Deployment

The `Dockerfile` included in this directory is used to create a Windows Docker image that can also build the installer. Note that although NSIS can run on Linux, the installer has to be signed for the Windows platform, which is why a Windows image is required instead.

In order to build the Windows installer as part of the CI/CD GeoServer pipeline, this image needs to be built on and deployed to the Azure Container Registry (ACR).
Using Docker and the Azure CLI, you can do:

```
$ az acr build --platform windows --registry <registry_name> --image <image_name:tag> .
```

An Azure DevOps pipeline is used to start the container and build the Windows installer from the Jenkins build artifacts after each successful GeoServer release and nightly build. If the Windows installer build was successful, it is pushed to SourceForge.
