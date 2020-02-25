Build Windows installer
-----------------------

At the time the GeoServer project does not have financial resources and man power to stand up a Windows build server (if you can help with this, please contact the developer list).
However you can create your own installer (using a Windows machine).

.. note:: This step requires a Windows machine.

#. Download and install `NSIS <http://nsis.sourceforge.net/>`_.

#. Install the `NSIS Access Control plugin <http://nsis.sourceforge.net/AccessControl_plug-in>`_.  The simplest way to do this is to download the zip, extract the .DLL files (:file:`AccessControl.dll`) and copy it to the NSIS plugins directory (usually :file:`C:\\Program Files\\NSIS\\Plugins`).

#. Unzip the binary GeoServer package::

        unzip geoserver-[VERSION]-bin.zip

#. Copy the files from :file:`src/release/installer/win` to the root of the unpacked archive (the same directory level as the :file:`start.jar`)::

      GeoServerEXE.nsi
      gs.ico
      header.bmp
      side_left.bmp
      splash.bmp
      wrapper.conf
      wrapper.dll
      wrapper.exe
      wrapper.jar
      wrapper-server-license.txt

   .. figure:: win-installer1.png
      :align: center

#. Right-click on the installer script :file:`GeoServerEXE.nsi` and select :command:`Compile Script`.  

   .. figure:: win-installer2.png
      :align: center

After successfully compiling the script, an installer named :file:`geoserver-[VERSION].exe` will be located in the root of the unpacked archive.

.. figure:: win-installer3.png
   :align: center
