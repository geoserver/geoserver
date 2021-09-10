# Note: image must be built on Azure (use Azure build context)!
FROM mcr.microsoft.com/windows/servercore:10.0.17763.1158-amd64

LABEL maintainer="sander.schaminee@geocat.net"

# Install Chocolately, NSIS and NSIS plugins
RUN @powershell.exe -NoProfile -InputFormat None -ExecutionPolicy Bypass -Command "[System.Net.ServicePointManager]::SecurityProtocol = 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" && SET "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
RUN choco install -y nsis
COPY ["nsis_plugins/x86-unicode/AccessControl.dll", "C:/Program Files (x86)/NSIS/Plugins/x86-unicode/AccessControl.dll"]
COPY ["nsis_plugins/x86-unicode/ShellLink.dll", "C:/Program Files (x86)/NSIS/Plugins/x86-unicode/ShellLink.dll"]

# Copy data and run script
COPY build C:/gsbuild
WORKDIR C:/gsbuild

# TODO: install OSGeo certificate, push build artifacts
ENTRYPOINT [ "C:/Program Files (x86)/NSIS/makensis.exe", "-V4", "geoserver_winsetup.nsi" ]
# ENTRYPOINT @powershell -NoProfile -ExecutionPolicy unrestricted -Command "C:/Program Files (x86)/NSIS/makensis.exe -V4 geoserver_winsetup.nsi"
# ENTRYPOINT [ "cmd" ]
