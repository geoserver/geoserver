FROM mcr.microsoft.com/windows:20H2

LABEL maintainer="sander.schaminee@geocat.net"

WORKDIR C:/gsbuild

RUN curl --output C:/gsbuild/nsis.zip --url "https://sourceforge.net/projects/nsis/files/NSIS%203/3.06.1/nsis-3.06.1.zip/download"

RUN powershell -Command "expand-archive -Path 'c:\gsbuild\nsis.zip' -DestinationPath 'c:\gsbuild'"

# RUN C:/gsbuild/nsis_setup.exe /S
ENTRYPOINT [ "cmd" ]

# RUN powershell.exe -Command \
#     $ErrorActionPreference = 'Stop'; \
#     $ProgressPreference = 'SilentlyContinue'; \
#     $url = "'https://sourceforge.net/projects/nsis/files/NSIS 3/3.06.1/nsis-3.06.1-setup.exe/download'"; \
#     $outpath = "C:\Windows\Temp\nsis_install.exe"; \
#     # $wc = New-Object System.Net.WebClient; \
#     # $wc.DownloadFile($url, $outpath); \
#     wget $url -OutFile $outpath; \
#     Start-Process -Filepath $outpath -ArgumentList '/quiet InstallAllUsers=1 PrependPath=1' -Wait ; \
#     Remove-Item $outpath -Force
