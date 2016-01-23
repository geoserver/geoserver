.. _production_java:

Java Considerations
===================

Use Supported JRE
-----------------

GeoServer's speed depends a lot on the chosen Java Runtime Environment (JRE).  For best performance, use *Oracle JRE 8* (also known as JRE 1.8). JREs other than those tested may work correctly, but are generally not recommended.  As an example users of OpenJDK 1.6 report GeoServer 2.5 to be working with reduced 2D rendering performance.

Tested:

* Java 8 - GeoServer 2.9.x and above (OpenJDK and Oracle JRE tested)
* Java 7 - GeoServer 2.6.x to GeoServer 2.8.x (OpenJDK and Oracle JRE tested)
* Java 6 - GeoServer 2.3.x to GeoServer 2.5.x (Oracle JRE tested)
* Java 5 - GeoServer 2.2.x and earlier (Sun JRE tested)

Unsupported:

* Java 9
   
As of GeoServer 2.0, a Java Runtime Environment (JRE) is sufficient to run GeoServer.  GeoServer no longer requires a Java Development Kit (JDK).

Install native JAI and JAI Image I/O extensions
-----------------------------------------------

The `Java Advanced Imaging API <http://java.sun.com/javase/technologies/desktop/media/>`_ (JAI) is an advanced image manipulation library built by Oracle.  GeoServer requires JAI to work with coverages and leverages it for WMS output generation. By default, GeoServer ships with the pure Java version of JAI, but **for best performance, install the native JAI version in your JDK/JRE**.

In particular, installing the native JAI is important for all raster processing, which is used heavily in both WMS and WCS to rescale, cut and reproject rasters. Installing the native JAI is also important for all raster reading and writing, which affects both WMS and WCS.  Finally, native JAI is very useful even if there is no raster data involved, as WMS output encoding requires writing PNG/GIF/JPEG images, which are themselves rasters.

Native extensions are available for Windows, Linux and Solaris (32 and 64 bit systems).  They are, however, not available for OS X.

.. note:: These installers are limited to allow adding native extensions to just one version of the JDK/JRE on your system.  If native extensions are needed on multiple versions, manually unpacking the extensions will be necessary.  See the section on :ref:`native_JAI_manual_install`.

.. note:: These installers are also only able to apply the extensions to the currently used JDK/JRE.  If native extensions are needed on a different JDK/JRE than that which is currently used, it will be necessary to uninstall the current one first, then run the setup program against the remaining JDK/JRE.

Installing native JAI on Windows
````````````````````````````````

#. Go to the `JAI download page <http://download.java.net/media/jai/builds/release/1_1_3/>`_ and download the Windows installer for version 1.1.3. At the time of writing only the 32 bit version of the installer is available, so if you are using a JDK, you will want to download `jai-1_1_3-lib-windows-i586-jdk.exe <http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib-windows-i586-jdk.exe>`_, and if you are using a JRE, you will want to download `jai-1_1_3-lib-windows-i586-jre.exe <http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib-windows-i586-jre.exe>`_.
#. Run the installer and point it to the JDK/JRE install that GeoServer will use to run.
#. Go to the `JAI Image I/O download page <http://download.java.net/media/jai-imageio/builds/release/1.1/>`_ and download the Windows installer for version 1.1. At the time of writing only the 32 bit version of the installer is available, so if you are using a JDK, you will want to download `jai_imageio-1_1-lib-windows-i586-jdk.exe <http://download.java.net/media/jai-imageio/builds/release/1.1/jai_imageio-1_1-lib-windows-i586-jdk.exe>`_, and if you are using a JRE, you will want to download `jai_imageio-1_1-lib-windows-i586-jre.exe <http://download.java.net/media/jai-imageio/builds/release/1.1/jai_imageio-1_1-lib-windows-i586-jre.exe>`_
#. Run the installer and point it to the JDK/JRE install that GeoServer will use to run.

Installing native JAI on Linux
``````````````````````````````

#. Go to the `OpenGeo JAI download page <http://data.opengeo.org/suite/jai/>`_ and download the Linux installer for version 1.1.3, choosing the appropriate architecture:

   * `i586` for the 32 bit systems
   * `amd64` for the 64 bit ones (even if using Intel processors)

#. Copy the file into the directory containing the JDK/JRE and then run it.  For example, on an Ubuntu 32 bit system::
  
    $ sudo cp jai-1_1_3-lib-linux-i586-jdk.bin /usr/lib/jvm/java-6-sun
    $ cd /usr/lib/jvm/java-6-sun
    $ sudo sh jai-1_1_3-lib-linux-i586-jdk.bin
    # accept license 
    $ sudo rm jai-1_1_3-lib-linux-i586-jdk.bin
  
#. Go to the `OpenGeo JAI Image I/O Download page <http://data.opengeo.org/suite/jai/>`_ and download the Linux installer for version 1.1, choosing the appropriate architecture:

   * `i586` for the 32 bit systems
   * `amd64` for the 64 bit ones (even if using Intel processors)

#. Copy the file into the directory containing the JDK/JRE and then run it.  If you encounter difficulties, you may need to export the environment variable ``_POSIX2_VERSION=199209``. For example, on a Ubuntu 32 bit Linux system::
  
    $ sudo cp jai_imageio-1_1-lib-linux-i586-jdk.bin /usr/lib/jvm/java-6-sun
    $ cd /usr/lib/jvm/java-6-sun
    $ sudo su
    $ export _POSIX2_VERSION=199209
    $ sh jai_imageio-1_1-lib-linux-i586-jdk.bin
    # accept license
    $ rm ./jai_imageio-1_1-lib-linux-i586-jdk.bin
    $ exit

.. _native_JAI_manual_install:

Installing native JAI manually
``````````````````````````````

You can install the native JAI manually if you encounter problems using the above installers, or if you wish to install the native JAI for more than one JDK/JRE.

Please refer to the `GeoTools page on JAI installation <http://docs.geotools.org/latest/userguide/build/install/jdk.html#java-extensions-optional>`_ for details.

 
GeoServer cleanup
`````````````````

Once the installation is complete, you may optionally remove the original JAI files from the GeoServer instance::

   jai_core-x.y.z.jar
   jai_imageio-x.y.jar 
   jai_codec-x.y.z.jar
   
where ``x``, ``y``, and ``z`` refer to specific version numbers.

.. _java_policyfiles:

Installing Unlimited Strength Jurisdiction Policy Files
-------------------------------------------------------
These policy files are needed for unlimited cryptography. As an example, Java does not support AES
with a key length of 256 bit. Installing the policy files removes these restrictions.

Open JDK
````````

Since Open JDK is Open Source, the policy files are already installed.   

Oracle Java
```````````

The policy files are available at   

* `Java 6 JCE policy jars <http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html>`_
* `Java 7 JCE policy jars <http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html>`_
* `Java 8 JCE policy jars <http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html>`_ 

The download contains two files, **local_policy.jar** and  **US_export_policy.jar**. The default
versions of these two files are stored in JRE_HOME/lib/security. Replace these two files with the
versions from the download. 


Test if unlimited key length is available
"""""""""""""""""""""""""""""""""""""""""

Start or restart GeoServer and login as administrator. The annotated warning should have disappeared.

.. figure:: ../security/webadmin/images/unlimitedkey.png

Additionally, the GeoServer log file should contain the following line::

   "Strong cryptography is available"

.. note::

   The replacement has to be done for each update of the Java runtime. 

IBM Java
````````

The policy files are available at

* `IBM JCE policy jars <https://www14.software.ibm.com/webapp/iwm/web/preLogin.do?source=jcesdk>`_ 

An IBM ID is needed to log in. The installation is identical to Oracle.

 
