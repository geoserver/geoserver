---
render_macros: true
---


# libjpeg-turbo Map Encoder Extension

This plugin brings in the ability to encode JPEG images as WMS output using the libjpeg-turbo library. Citing its website the [libjpeg-turbo library](http://libjpeg-turbo.virtualgl.org//) is a derivative of libjpeg that uses SIMD instructions (MMX, SSE2, NEON) to accelerate baseline JPEG compression and decompression on x86, x86-64, and ARM systems. On such systems, libjpeg-turbo is generally 2-4x as fast as the unmodified version of libjpeg, all else being equal. I guess it is pretty clear why we wrote this plugin! Note that the underlying imageio-ext-turbojpeg uses TurboJpeg which is a higher level set of API (providing more user-friendly methods like "Compress") built on top of libjpeg-turbo.

!!! warning
    The speedup may vary depending on the target infrastructure.

The module, once installed, replaces the standard JPEG encoder for GeoServer and allows us to use the libjpeg-turbo library to encode JPEG response for GetMap requests.

!!! note
    When using OpenJDK provided with your Linux distribution it may already be configured with libjpeg-turbo library.

!!! note
    This module depends on a successful installation of the libjpeg-turbo native libraries described in the next section.

## Installing the GeoServer libjpeg-turbo extension

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Output Formats** extensions download **JPEG Turbo**.

    - {{ release }} example: [libjpeg-turbo](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-libjpeg-turbo-plugin.zip)
    - {{ snapshot }} example: [libjpeg-turbo](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ snapshot }}-libjpeg-turbo-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory of the GeoServer installation.

## Installing the libjpeg-turbo native library

Installing the libjpeg-turbo native library is a prerequisite for properly enabling the corresponding GeoServer Map Encoder. Once the GeoServer extension is installed, the required JARs providing the Java bindings to the native library are already available on the classpath.

The turbojpeg-X.X.X.jar bundle (at the time of writing, turbojpeg-8.5.0.jar), included in the libjpeg-turbo-plugin.zip, also contains the necessary native libraries. Therefore, the only remaining step is to install the native library itself to enable high-performance JPEG encoding.

1.  Linux users may also check if it is available from their package manager:

    ``` bash
    apt get libjpeg-dev
    apt get libjpeg-java
    ```

To perform the installation of the libjpeg-turbo binaries (or native library) you have to perform the following steps:

1.  Extract the turbojpeg-X.X.X.jar file (located in the WEB-INF/lib directory of your GeoServer installation) into a temporary folder.

2.  Navigate to the META-INF/lib directory and select the package that matches your target platform, considering both the operating system (e.g., Linux or Windows) and the architecture (32-bit or 64-bit).
3.  Copy the required native library (e.g., turbojpeg.dll for Windows or turbojpeg.so for Linux) to a location included in the Java process’s native library path for GeoServer. See the **Notes** below for further details.

!!! note
    When installing on Windows, always make sure that the location where you placed the DLLs is part of the `PATH` environment variable for the Java Process for the GeoServer. This usually means that you have to add such location to the PATH environmental variable for the user that is used to run GeoServer or the system wide variables.

!!! note
    When installing on Linux, make sure that the location where you placed the DLLs is part of the `LD_LIBRARY_PATH` environment variable for the Java Process for the GeoServer. This usually happens automatically for the various Linux packages, but in some cases you might be forced to do that manually

!!! note
    It does not hurt to add also the location where the native libraries are installed to the Java startup options `-Djava.library.path=<absolute_and_valid_path>`.


## Checking if the extension is enabled

Once the extension is installed, the following lines should appear in the GeoServer log:

```
10-mar-2013 19.16.28 it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities load
INFO: TurboJPEG library loaded (turbojpeg)
```

or:

    10 mar 19:17:12 WARN [turbojpeg.TurboJPEGMapResponse] - The turbo jpeg encoder is available for usage

You can also check in the **Server Status** page. From the **Modules** tab:

- Locate the **GeoServer libjpeg-turbo Module** module. The enabled status indicates if the extension is available
- Click on the **GeoServer libjpeg-turbo Module** link to check module status. The **Module Info** dialog indicates the JNI LibJPEGTurbo Wrapper Version used.

## Disabling the extension

When running GeoServer the turb encoder can be disabled by using the Java switch for the JVM process:

    -Ddisable.turbojpeg=true

In this case a message like the following should be found in the log:

    WARN [map.turbojpeg] - The turbo jpeg encoder has been explicitly disabled

!!! note
    We will soon add a section in the GUI to check the status of the extension and to allow users to enable/disable it at runtime.
