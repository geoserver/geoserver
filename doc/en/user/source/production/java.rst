.. _production_java:

Java Considerations
===================

Use a supported Java version 
----------------------------

GeoServer's speed depends a lot on the chosen Java Runtime Environment (JRE). The latest versions of GeoServer are tested with both OpenJDK and Oracle. Implementations other than those tested may work correctly, but are generally not recommended.

GeoServer is built and tested with Java 8 (Recommended), `supported up to 2026 <https://adoptopenjdk.net/support.html#roadmap>`_, and Java 11, `supported up to 2024 <https://adoptopenjdk.net/support.html#roadmap>`_. We recommend these long-term-support releases for production. Java has adopted development cycle with a release every six months containing experimental features. These releases are not tested with GeoServer and are not recommended for production.

For best performance we recommend the use *OpenJDK 8* (also known as JRE 1.8).

.. list-table:: Title
   :widths: 20, 10, 10, 10, 50
   :header-rows: 1

   * - Java Version
     - OpenJDK
     - Oracle
     - Sun
     - GeoServer Compatibility
   * - **Java 11**
     - tested
     - tested
     -
     - GeoServer 2.15.x and above
   * - **Java 8**
     - tested
     - tested
     -
     - GeoServer 2.9.x and above
   * - Java 7 
     - tested
     - tested
     -
     - GeoServer 2.6.x to 2.8.x
   * - Java 6 
     - 
     - tested
     -
     - GeoServer 2.3.x to 2.5.x
   * - Java 5 
     - 
     - 
     - tested
     - GeoServer 2.2.x and earlier

As of GeoServer 2.0, a Java Runtime Environment (JRE) is sufficient to run GeoServer.  GeoServer no longer requires installation of a complete Java Development Kit (JDK) with compiler.

Running on Java 8 (Recommended)
-------------------------------

GeoServer is built and tested with Java 8, long-term-support with industry `support up to 2026 <https://adoptopenjdk.net/support.html#roadmap>`_.

Recommended:

* We recommend installation of the Marlin Renderer described next section
* Unlimited Strength Jurisdiction Policy Files are available for separate intallation

Available, but not recommended:

* Java 8 is the last release to offer a native extension mechanism. GeoServer uses our own JAI-EXT image processing operations. In rare circumstances you may wish to disable JAI-EXT, and install Native JAI and ImageIO extensions.

.. _java_marlin:

Marlin renderer (Recommended)
`````````````````````````````

Further speed improvements can be released using `Marlin renderer <https://github.com/bourgesl/marlin-renderer>`__ alternate renderer for Java 8.

Before Java 9, OpenJDK the Pisces renderer, and Oracle used the Ductus renderer, to rasterize vector data respectively.  In Java 9 onward they use Marlin renderer which has better overall performance in most situations than either Pisces or Ductus.

In order to enable Marlin on Java 8, see :ref:`production_container.marlin` on the next page.

.. _java_policyfiles:

Installing Unlimited Strength Jurisdiction Policy Files
```````````````````````````````````````````````````````
These policy files are needed for unlimited cryptography. As an example, Java does not support AES
with a key length of 256 bit.


#. Installing the policy files removes these restrictions.

   * OpenJDK

     Since Open JDK is Open Source, the policy files are already installed.   

   * Oracle Java

     The policy files are available at `Java 8 JCE policy jars <http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html>`__.

     The download contains two files, **local_policy.jar** and  **US_export_policy.jar**. The default ersions of these two files are stored in :file:`JRE_HOME/lib/security`. Replace these two files with the versions from the download. 

   * IBM Java

     The policy files are available at `IBM JCE policy jars <https://www14.software.ibm.com/webapp/iwm/web/preLogin.do?source=jcesdk>`__. 

     An IBM ID is needed to log in. The installation is identical to Oracle.

#. Test if unlimited key length is available

   Start or restart GeoServer and login as administrator. The annotated warning should have disappeared.

   .. figure:: ../security/webadmin/images/unlimitedkey.png

#. Additionally, the GeoServer log file should contain the following line::

      "Strong cryptography is available"

.. note::

   The installing these policy files needs to be done for each update of the Java runtime. 

Native JAI and ImageIO extensions (not recommended)
```````````````````````````````````````````````````

The `Java Advanced Imaging API <http://www.oracle.com/technetwork/java/javase/tech/jai-142803.html>`_ (JAI) is an image processing library built by Oracle:

* GeoServer uses JAI-EXT, a set of replacement operations with bug fixes and NODATA support, for all image processing operations.

* In case there is no interest in NODATA support, one can disable JAI-EXT and install the native JAI extensions to improve raster processing performance.

.. warning:: Users should take care that *JAI* native libraries remove support for NODATA pixels, provided instead by the pure Java JAI-EXT libraries.

Before installing native JAI, JAI-EXT can be disabled adding the following system variable to the JVM running GeoServer::

	-Dorg.geotools.coverage.jaiext.enabled=false

Native JAI and ImageIO extensions are available for:

+----------+-----------+-----------+
| System   | 32-bit    | 64-bit    |
+==========+===========+===========+
| Windows  | available |           |
+----------+-----------+-----------+
| Linux    | available | available |
+----------+-----------+-----------+
| Solaris  | available | available |
+----------+-----------+-----------+
| Max OSX  |           |           |  
+----------+-----------+-----------+

.. warning:: A system installations of JAI and ImageIO may conflict with the pure java copy of JAI and ImageIO included in your GeoServer ``WEB-INF/lib`` folder - producing "class cast exceptions" preventing your application server from starting GeoServer.
    
    * When installed as a "java extension" JAI and JAI ImageIO are unpacked into your JRE as both native code (in ``bin``) and jars (in ``ext/libs``). If you encounter this problem after installation of native the JAI and ImageIO extensions remove the pure java implementation from your GeoServer instances ``WEB-INF/lib`` folder::
        
        rm jai_core-*jar jai_imageio-*.jar jai_codec-*.jar

    * On OSX jars may be installed in ``~/Library/Java/Extensions``, we advise removing these jars if present as they are no longer maintained by Apple.
   
.. note:: Native ImageIO encoding may not always be the best choice, we recommend the built-in :ref:`PNGJ based encoder <JAI>` and :ref:`community_libjpeg-turbo` for png8 and jpeg encoding performance.
   
Installing native JAI on Windows
````````````````````````````````

#. Go to the `JAI download page <http://download.java.net/media/jai/builds/release/1_1_3/>`_ and download the Windows installer for version 1.1.3. At the time of writing only the 32 bit version of the installer is available, so if you are using a JDK, you will want to download `jai-1_1_3-lib-windows-i586-jdk.exe <http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib-windows-i586-jdk.exe>`_, and if you are using a JRE, you will want to download `jai-1_1_3-lib-windows-i586-jre.exe <http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib-windows-i586-jre.exe>`_.
#. Run the installer and point it to the JDK/JRE install that GeoServer will use to run.
#. Go to the `JAI Image I/O download page <http://download.java.net/media/jai-imageio/builds/release/1.1/>`_ and download the Windows installer for version 1.1. At the time of writing only the 32 bit version of the installer is available, so if you are using a JDK, you will want to download `jai_imageio-1_1-lib-windows-i586-jdk.exe <http://download.java.net/media/jai-imageio/builds/release/1.1/jai_imageio-1_1-lib-windows-i586-jdk.exe>`_, and if you are using a JRE, you will want to download `jai_imageio-1_1-lib-windows-i586-jre.exe <http://download.java.net/media/jai-imageio/builds/release/1.1/jai_imageio-1_1-lib-windows-i586-jre.exe>`_
#. Run the installer and point it to the JDK/JRE install that GeoServer will use to run.
#. Once the installation is complete, you may optionally remove the original JAI files from the GeoServer ``WEB-INF/lib`` folder::

   * jai_core-x.y.z.jar
   * jai_imageio-x.y.jar 
   * jai_codec-x.y.z.jar
   

   where ``x``, ``y``, and ``z`` refer to specific version numbers.
   
.. note:: These installers are limited to allow adding native extensions to just one version of the JDK/JRE on your system.  If native extensions are needed on multiple versions, manually unpacking the extensions will be necessary.  See the section on :ref:`native_JAI_manual_install`.

.. note:: These installers are also only able to apply the extensions to the currently used JDK/JRE.  If native extensions are needed on a different JDK/JRE than that which is currently used, it will be necessary to uninstall the current one first, then run the setup program against the remaining JDK/JRE.

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

#. Once the installation is complete, you may optionally remove the original JAI files from the GeoServer ``WEB-INF/lib`` folder::

   * jai_core-x.y.z.jar
   * jai_imageio-x.y.jar 
   * jai_codec-x.y.z.jar
   

   where ``x``, ``y``, and ``z`` refer to specific version numbers.


.. _native_JAI_manual_install:

Installing native JAI manually
``````````````````````````````

You can install the native JAI manually if you encounter problems using the above installers, or if you wish to install the native JAI for more than one JDK/JRE.

Please refer to the `GeoTools page on JAI installation <http://docs.geotools.org/latest/userguide/build/install/jdk.html#java-extensions-optional>`_ for details.

Running on Java 11
------------------

GeoServer is tested with Java 11 (LTS), with industry `support up to 2024 <https://adoptopenjdk.net/support.html#roadmap>`_. 

GeoServer 2.15 onward will run under Java 11 with no additional configuration on **Tomcat 9** or newer and **Jetty 9.4.12** or newer. Running GeoServer using Java 11 on other Application Servers may require some additional configuration as not all Application Servers support Java 11 yet.

* Java 11 already includes Marlin Renderer, although you may wish to consider installing a newer version
* Java 11 includes the Unlimited Strength Jurisdiction Policy Files, no need for a separate installation
* Java 11 does not support Native JAI and ImageIO extensions

