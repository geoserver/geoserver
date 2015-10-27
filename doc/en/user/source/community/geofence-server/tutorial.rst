Using the Internal GeoFence server (Tutorial)
=============================================

Introduction
------------

This tutorial shows how to use the integrated internal GeoFence Server plug-in in GeoServer. GeoFence allows far more advanced security configurations than the default GeoServer :ref:`security` substem, such as rules that combine data and service restrictions. This module allows the use of GeoFence in GeoServer without the need to set-up a separate server application.


Getting Started
---------------

Install the plugin-in, see :ref:`geofence_server_install`. If your most commonly used user/group service is not called "default", create a new text file called '/geofence/geofence-server.properties' in the geoserver data directory and add the following line::

        defaultUserGroupServiceName=<name_of_preferred_usergroupservice>

The user/group service must allow the creation of new users for the tutorial.

Restart GeoServer.

There should now be a :guilabel:`GeoFence Server` link on the left side of the screen after logging in. Click on it. 
This is the configuration page of your internal GeoFence.

.. figure:: images/tutorial_rulespage1.png
   :align: center

Creating new Rules with the GUI
-------------------------------

Click on the "Add new rule" link. Change only "Access" to "DENY".

.. figure:: images/tutorial_rulepage1.png
   :align: center

Click on "Save".

.. figure:: images/tutorial_rulespage2.png
   :align: center

We have now expressed that the first rule (with lowest priority) disallows everyone from everything. The following more specific rules we make will provide the exceptions to that general rule. It is also possible to do it the other way (allow everyone to anything as most general rule and specify exceptions to that.) 

As a next step, we will grant the administrator access to everything. Click on "Add new rule" again. Change "Role" to "ADMIN" and click "Save".

.. figure:: images/tutorial_rulepage2.png
   :align: center

.. figure:: images/tutorial_rulespage3.png
   :align: center

You now have a working, basic security configuration.


Creating rules with the REST API
--------------------------------

Open a new tab with your browser and go to the following URL: http://localhost:8080/geoserver/geofence/rest/rules. You should get an XML representation of your rules::

  <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
  <Rules count="2">
  	<Rule id="2">
  		<access>ALLOW</access>
  		<priority>0</priority>
  		<roleName>ADMIN</roleName>
	</Rule>
  	<Rule id="1">
  		<access>DENY</access>
  		<priority>1</priority>
  	</Rule>
  </Rules>

Let us first create a new user. Do this by sending a POST request to the following URL http://localhost:8080/geoserver/geofence/rest/usergroup/users with the following content::

  <user>
        <userName>michaeljfox</userName>
        <password>back2$future</password>
        <enabled>true</enabled>
  </user>

You should receive a "201 Created" HTTP Response.

Now we will create an access rule for this user. Do this by sending a POST request to the following URL: http://localhost:8080/geoserver/geofence/rest/rules with the following content::

  <Rule>
        <userName>michaeljfox</userName>
        <workspace>topp</workspace>
        <layer>states</layer>
        <service>WMS</service>
        <request>GetMap</request>
        <access>ALLOW</access>
  </Rule>

Again, you should receive a "201 Created" HTTP Response. When browsing to the URL http://localhost:8080/geoserver/geofence/rest/rules we should now see the following information::

  <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
  <Rules count="2">
  	<Rule id="3">
		<access>ALLOW</access>
		<layer>states</layer
		<priority>0</priority>
		<request>GETMAP</request>
		<service>WMS</service>
		<userName>michaeljfox</userName>
		<workspace>topp</workspace>
	</Rule>
  	<Rule id="2">
  		<access>ALLOW</access>
  		<priority>0</priority>
  		<roleName>ADMIN</roleName>
	</Rule>
  	<Rule id="1">
  		<access>DENY</access>
  		<priority>1</priority>
  	</Rule>
  </Rules>

It should now be possible to log on with username 'michaeljfox' and password 'back2$future` and perform a 'GetMap' on the layer 'topp:states', but nothing else.


