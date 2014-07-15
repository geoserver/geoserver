.. _installation_linux_debian:

Debian
======

.. warning:: Under construction

Setup a local geoserver instance with tomcat7 in Debian wheezy/sid.

Preparation
-----------

#. Follow instructions from :doc:`../war` until step 3 and return here again.
#. Root permissions are needed, to make some changes to the linux system. Gain superuser rights by executing on a terminal: ``su``
#. If not already available on your debian system, install the tomcat7 servlet container with your favourite package administration tool (synaptic, apt-get or aptitude). This tutorial uses aptitude: ``aptitude install tomcat7``

Installation
------------

#. Copy as user root the geoserver web application archive into tomcat7's webapp directory: ``cp geoserver.war /var/lib/tomcat7/webapps``
#. Tomcat should recognize the WAR archive and immediately start to deploy the web application. This process takes some time and depends on your hardware used. Congratulations, your `local geoserver`_ is now up and running.

.. _local geoserver: http://localhost:8080/geoserver

