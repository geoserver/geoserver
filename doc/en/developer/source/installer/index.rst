Build Windows installer
-----------------------

At the time the GeoServer project does not have financial resources and man power to stand up a Windows build server (if you can help with this, please contact the developer list).
However you can create your own installer (using a Windows machine).

.. note:: This step requires a Windows machine.

.. note:: A community provided Powershell script that automates the following steps is available `here <https://github.com/geoserver/geoserver/edit/main/src/release/installer/win/win-installer-builder.ps1>`_.

#. Download and install `NSIS <http://nsis.sourceforge.net/>`_.

#. Install the `NSIS Access Control plugin <http://nsis.sourceforge.net/AccessControl_plug-in>`_.  The simplest way to do this is to download the zip, extract the .DLL files (:file:`AccessControl.dll`) and copy it to the NSIS plugins directory (usually :file:`C:\\Program Files\\NSIS\\Plugins\\x86-ansi`).

#. Download and unzip the binary GeoServer package::

        unzip geoserver-[VERSION]-bin.zip

#. Download and unzip the source GeoServer package::

        unzip geoserver-[VERSION].zip

#. Copy the files :file:`LICENSE.txt`, :file:`src/release/GPL.txt` and the following files from :file:`src/release/installer/win` from the Geoserver source GeoServer package to the root of the unpacked archive (the same directory level as the :file:`start.jar`)::

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

   .. figure:: img/win-installer1.png
      :align: center

#. Right-click on the installer script :file:`GeoServerEXE.nsi` and select :command:`Compile Script`.  

   .. figure:: img/win-installer2.png
      :align: center

#. After successfully compiling the script, an installer named :file:`geoserver-[VERSION].exe` will be located in the root of the unpacked archive.

   .. figure:: img/win-installer3.png
      :align: center
