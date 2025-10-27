.. _community_libjpeg-turbo:

libjpeg-turbo Map Encoder Extension
==========================================
This plugin brings in the ability to encode JPEG images as WMS output using the libjpeg-turbo library. Citing its website the `libjpeg-turbo library <http://libjpeg-turbo.virtualgl.org//>`_ is a derivative of libjpeg that uses SIMD instructions (MMX, SSE2, NEON) to accelerate baseline JPEG compression and decompression on x86, x86-64, and ARM systems. On such systems, libjpeg-turbo is generally 2-4x as fast as the unmodified version of libjpeg, all else being equal. I guess it is pretty clear why we wrote this plugin! Note that the underlying imageio-ext-turbojpeg uses TurboJpeg which is a higher level set of API (providing more user-friendly methods like "Compress") built on top of libjpeg-turbo.

.. warning:: The speedup may vary depending on the target infrastructure.

The module, once installed, replaces the standard JPEG encoder for GeoServer and allows us to use the libjpeg-turbo library to encode JPEG response for GetMap requests.

.. note:: When using OpenJDK provided with your Linux distribution it may already be configured with libjpeg-turbo library.

.. note:: This module depends on a successful installation of the libjpeg-turbo native libraries described in the next section.

Installing the libjpeg-turbo native library
-------------------------------------------

Installing the libjpeg-turbo native library is a precondition to have the relative GeoServer Map Encoder properly installed; once the GeoServer extension has been installed as we explain in the following section, the needed JARs with the Java bridge to the library are in the classpath, therefore all we need to do is to install the native library itself to start encoding JPEG at turbo speed.

#. Linux users may also check if it is available from their package manager:

   .. code-block:: bash
      
      apt get libjpeg-dev
      apt get libjpeg-java

To perform the installation of the libjpeg-turbo binaries (or native library) you have to perform the following steps:

#. Go to the download site `here <https://sourceforge.net/projects/libjpeg-turbo/files/>`__ and download the latest available stable release (1.2.90 at the time of writing).

#. Select the package that matches the target platform in terms of Operating System (e.g. Linux rather than Windows) and Architecture (32 vs 64 bits).

#. Perform the installation using the target platform conventions. As an instance for Windows you should be using an installer that installs all the needed libs in a location at user's choice. On Ubuntu Linux systems you can use the *deb* files instead.

#. Once the native libraries are installed, you have to make sure the GeoServer can load them. This should happen automatically after Step 2 on Linux, while on Windows you should make sure that the location where you placed the DLLs is part of the PATH environment variable for the Java Process for the GeoServer.

.. warning:: When installing on Windows, always make sure that the location where you placed the DLLs is part of the ``PATH`` environment variable for the Java Process for the GeoServer. This usually means that you have to add such location to the PATH environmental variable for the user that is used to run GeoServer or the system wide variables.

.. warning:: When installing on Linux, make sure that the location where you placed the DLLs is part of the ``LD_LIBRARY_PATH`` environment variable for the Java Process for the GeoServer. This usually happens automatically for the various Linux packages, but in some cases you might be forced to do that manually

.. note:: It does not hurt to add also the location where the native libraries are installed to the Java startup options ``-Djava.library.path=<absolute_and_valid_path>``.

Installing the GeoServer libjpeg-turbo extension
------------------------------------------------

.. warning:: Before moving on make sure you installed the libjpeg-turbo binaries as per the section above.

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Output Formats** extensions download **JPEG Turbo**.

   * |release| example: :download_extension:`libjpeg-turbo`
   * |version| example: :nightly_extension:`libjpeg-turbo`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory of the GeoServer installation.

Checking if the extension is enabled
------------------------------------

Once the extension is installed, the following lines should appear in the GeoServer log::

  10-mar-2013 19.16.28 it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities load
  INFO: TurboJPEG library loaded (turbojpeg)

or::

   10 mar 19:17:12 WARN [turbojpeg.TurboJPEGMapResponse] - The turbo jpeg encoder is available for usage

You can also check in the :menuselection:`Server Status` page. From the :guilabel:`Modules` tab:

* Locate the :guilabel:`GeoServer libjpeg-turbo Module` module. The enabled status indicates if the extension is available
* Click on the :guilabel:`GeoServer libjpeg-turbo Module` link to check module status. The :guilabel:`Module Info` dialog indicates the JNI LibJPEGTurbo Wrapper Version used.

Disabling the extension
------------------------------------
When running GeoServer the turb encoder can be disabled by using the Java switch for the JVM process::

  -Ddisable.turbojpeg=true

In this case a message like the following should be found in the log::

  WARN [map.turbojpeg] - The turbo jpeg encoder has been explicitly disabled


.. note:: We will soon add a section in the GUI to check the status of the extension and to allow users to enable/disable it at runtime.
