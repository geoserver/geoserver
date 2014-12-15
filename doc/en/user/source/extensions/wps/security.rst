.. _security_wps:

WPS Security
============

Geoserver service security is normally based on the generic <OGC security configuration `_sec_service`>_, however, when it
comes to WPS there is also a need to restrict single process availability. 
WPS security allows access to be determined on a per process group or per single process, similarly
to how data security restricts access to layers.

Each process and process group can be either disabled entirely, or subject to access control based on the
user roles.

The WPS security configurations can be changed using the Web Administration Interface. 
The "Security"/"WPS security" page contains list of WPS groups with ability to enable/disable them, 
limit their access to specific roles, and links to process list and process access mode configuration.

.. figure:: images/security-groups.png
   :align: center
   
   *The WPS security page*
   
The process access mode configuration specifies how GeoServer will advertise 
secured processes and behave when a secured processes is accessed without the necessary privileges.
The parameter can assume the values: HIDE (default), CHALLENGE, MIXED:

* In **HIDE** mode the processes not available to the current user will simply be removed, denying their existence to the clients,
  in particular they will not be listed to GetCapabilities output and a direct access will result in GeoServer claiming the process does not exists
* In **CHALLENGE** mode all the processes will be available in the GetCapabilities output, but a authentication 
  request will be raised if a secured process is requested via any other service call by a user that does not have sufficient access rights
* In **MIXED** mode the secured processes will be hidden from the GetCapabilities document to the users not having sufficient access rights, 
  but an authentication request will be raised if a secured process is requested anyways via any other WPS request 
  
The list of roles attached to each group or process will determine which users can access the
processes, if the list is empty the group/process will be available to all users (unless it has
been disabled, in which case it won't be available to anyone).
The role editor provide auto-completion to ease up filling values, and allowing quick copy and paste of 
role lists from one process definition to the other. 
The roles string must be a simple semicolon separated list:

.. figure:: images/security-process.png
   :align: center

   *The role editor inside the process list page*
