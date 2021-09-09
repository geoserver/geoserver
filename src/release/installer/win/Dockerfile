FROM mcr.microsoft.com/windows:20H2

LABEL maintainer="sander.schaminee@geocat.net"

# Install Chocolately, NSIS and NSIS plugins
ENV nsis_dir="C:/Program Files (x86)/NSIS"
RUN @"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -InputFormat None -ExecutionPolicy Bypass -Command "[System.Net.ServicePointManager]::SecurityProtocol = 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" && SET "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
RUN choco install nsis -y
COPY ["nsis_plugins/x86-unicode/AccessControl.dll", "${nsis_dir}/Plugins/x86-unicode/AccessControl.dll"]
COPY ["nsis_plugins/x86-unicode/ShellLink.dll", "${nsis_dir}/Plugins/x86-unicode/ShellLink.dll"]

# Copy data and run script
COPY build C:/gsbuild
WORKDIR C:/gsbuild

# TODO: install OSGeo certificate, push build artifacts
# ENTRYPOINT [ "${nsis_dir}/makensis.exe", "-V4", "geoserver_winsetup.nsi" ]
ENTRYPOINT [ "cmd" ]
