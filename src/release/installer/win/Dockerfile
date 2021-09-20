# Note: image must be built on Azure (use Azure build context)!
FROM mcr.microsoft.com/windows/servercore:ltsc2019-amd64

LABEL maintainer="sander.schaminee@geocat.net"

# Install Chocolately, NSIS and NSIS plugins
RUN @powershell.exe -NoProfile -InputFormat None -ExecutionPolicy Bypass -Command "[System.Net.ServicePointManager]::SecurityProtocol = 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))" && SET "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
RUN choco install -y nsis 

# Install Windows SDK. It provides signtool.exe needed to sign the installer
RUN choco install -y windows-sdk-10.1

# Add NSIS and Windows SDK to the PATH
RUN setx /M PATH "%PATH%;C:/Program Files (x86)/Windows Kits/10/App Certification Kit;C:/Program Files (x86)/NSIS"

# Copy NSIS plugins in place
COPY ["nsis_plugins/x86-unicode/AccessControl.dll", "C:/Program Files (x86)/NSIS/Plugins/x86-unicode/AccessControl.dll"]
COPY ["nsis_plugins/x86-unicode/ShellLink.dll", "C:/Program Files (x86)/NSIS/Plugins/x86-unicode/ShellLink.dll"]

# Copy data, create output folder and set working directory to NSIS script folder
COPY build C:/geoserver/gsbuild
RUN mkdir C:\\geoserver\\target
WORKDIR C:/geoserver/gsbuild
