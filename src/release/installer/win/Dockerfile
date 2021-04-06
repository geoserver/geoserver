FROM suchja/wine:latest
RUN dpkg --add-architecture i386

# Install requirements (NSIS 3.0.6 and Mono)
RUN apt-get update -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y nsis nsis-doc nsis-pluginapi mono-devel --assume-yes wine32 && \
    rm -rf /var/lib/apt/lists/*

ENV DISPLAY :0
ENV WINEDEBUG=fixme-all

# Add AccessControl and ShellLink NSIS plugins
ADD nsis_plugins /usr/share/nsis/Plugins

# # Create temp directory for plugins
# RUN mkdir /tmp/nsis_plugins

# # Download and extract AccessControl plugin (amd64)
# RUN wget https://nsis.sourceforge.io/mediawiki/images/4/4a/AccessControl.zip -P /tmp/nsis_plugins && \
#     unzip /tmp/nsis_plugins/AccessControl.zip -d /tmp/nsis_plugins && \
#     mv /tmp/nsis_plugins/Plugins/amd64-unicode/AccessControl.dll /usr/share/nsis/Plugins/amd64-unicode/AccessControl.dll && \
#     rm -r /tmp/nsis_plugins/*

# # Download and extract ShellLink plugin (32-bits only)
# RUN wget https://nsis.sourceforge.io/mediawiki/images/6/6c/Shelllink.zip -P /tmp/nsis_plugins && \
#     unzip /tmp/nsis_plugins/Shelllink.zip -d /tmp/nsis_plugins && \
#     mv /tmp/nsis_plugins/Unicode/Plugins/ShellLink.dll /usr/share/nsis/Plugins/x86-unicode/ShellLink.dll && \
#     rm -r /tmp/*

# Copy source files for NSIS build script
ADD build /installer
ADD source /installer/source

# Add certificate files for signing
ADD OSGeo_DigiCert_Authenticode.pvk /installer
ADD OSGeo_DigiCert_Authenticode.spc /installer

# Setup a Wine prefix
ENV WINEPREFIX=/tmp/.wine
ENV WINEARCH=win32
# RUN winecfg

WORKDIR /installer

# ENTRYPOINT [ "makensis", "-V4", "geoserver_winsetup.nsi" ]
ENTRYPOINT [ "/bin/bash" ]