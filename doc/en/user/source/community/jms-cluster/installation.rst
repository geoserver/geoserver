.. module:: jms.installation

.. _jms.installation:

Installation of the JMS Cluster modules
=======================================

To install the JMS Cluster modules you simply have to:

* Download the ``geoserver-jms-cluster-<version>.zip`` file from the nightly builds, community section
* Stop GeoServer
* Unpack the zip file in ``webapps/geoserver/WEB-lib``
* Start again GeoServer

: .. warning::
  
  In GeoServer versions 2.10.4, 2.11.1 and 2.12-beta default topic name was renamed from ``VirtualTopic.>``, which has a special meaning in ActiveMQ, to ``VirtualTopic.geoserver``. When upgrading to one of this versions or above the virtual topic name will be automatically updated. Note that only GeoServer instances that use the same topic name will be synchronized.

As a recommendation, for the first tests try to run the cluster module on a cluster
of GeoServer all having their own data directory.

If you want to use the clustering extension while sharing the same data dir that's also possible,
but you'll have to remember to use the ``CLUSTER_CONFIG_DIR`` system variable, and set it
to a different folder for each instance, e.g.:

* set ``-DGEOSERVER_CONFIG_DIR=/path/to/cluster/config/dir/1`` on the first node, 
* set ``-DGEOSERVER_CONFIG_DIR=/path/to/cluster/config/dir/2`` on the second node
* and so on

The directories do not need to exist, GeoServer will create and populate them on
startup automatically.