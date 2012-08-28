.. _production_linuxscript:

Linux init scripts
==================

You will have to adjust the scripts to your environment. Download a script, rename it to :file:`geoserver` and move it to :file:`/etc/init.d`. Use :command:`chmod` to make the script executable and test with :command:`/etc/init.d/geoserver`.

To set different values for environment variables, create a file :file:`/etc/default/geoserver` and specify your  environment.

Example settings in :file:`/etc/default/geoserver` for your environment::


 USER=geoserver
 GEOSERVER_DATA_DIR=/home/$USER/data_dir
 GEOSERVER_HOME=/home/$USER/geoserver
 JAVA_HOME=/usr/lib/jvm/java-6-sun
 JAVA_OPTS="-Xms128m -Xmx512m"



Debian/Ubuntu
-------------

:download:`Download the init script <scripts/geoserver_deb>`


Suse
----

:download:`Download the init script <scripts/geoserver_suse>`


Starting GeoServer in Tomcat
----------------------------

:download:`Download the init script <scripts/geoserver_tomcat>`

