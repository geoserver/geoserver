.. _authkey:

Key authentication module 
=========================

The ``authkey`` module for GeoServer allows for a very simple authentication protocol designed for 
OGC clients that cannot handle any kind of security protocol, not even the HTTP basic authentication.

For these clients the module allows a minimal form of authentication by appending a unique key in the
URL that is used as the sole authentication token. Obviously this approach is open to security token
sniffing and must always be associated with the use of HTTPS connections. 

A sample authenticated request looks like::

  http://localhost:8080/geoserver/topp/wms?service=WMS&version=1.3.0&request=GetCapabilities&authkey=ef18d7e7-963b-470f-9230-c7f9de166888
  
Where ``authkey=ef18d7e7-963b-470f-9230-c7f9de166888`` is associated to a specific user (more on this later).
The capabilities document contains backlinks to the server itself, linking to the URLs that can be used
to perform GetMap, GetFeatureInfo and so on.
When the ``authkey`` parameter is provided the backlinks will contain the authentication key as well,
allowing any compliant WMS client to access secured resources. 

Limitations
------------------------

The ``authkey`` module is meant to be used with OGC services. It won't work properly against the
administration GUI, nor RESTConfig.

The default key provider
------------------------

By default the authentication module uses a property file located at ``$GEOSERVER_DATA_DIR/security/authkeys.propeties``
containing a mapping between the keys and the user names. A sample file looks as follows::

  # Format is authkey=username
  b52d2068-0a9b-45d7-aacc-144d16322018=admin
  1825efd3-20e1-4c18-9648-62c97d3ee5cb=sf
  ef18d7e7-963b-470f-9230-c7f9de166888=topp

If the file is missing by default the module will generate a new one where each existing user is
associated to a automatically generated unique key, and a hash mark in front to disable them: you can 
remove the hashmark to enable key authentication a specific user.

Provider pluggability
---------------------

With some Java programming it is possible to programmatically create and register a new key to user 
name mapper that works under a different logic. 
For example, you could have daily tokens, token generators and the like.

In order to provide your custom mapper you have to implement the ``org.geoserver.securityAuthenticationKeyMapper``
interface and then register said bean in the Spring application context. A very simple mapper statically
associating the ``superpowers`` key to the administrator would look as follows:

.. code-block:: java 


	public class SuperpowersMapper implements AuthenticationKeyMapper {
	
	    public String getUserName(String key) {
	        if("superpowers".equals(key)) {
	          return "admin";
	        } else {
	           return null;
	        }
	    }
	
	}
  
The mapper would have to be registered in the Spring application context in a ``applicationContext.xml``
file in the root of your jar. For example:

.. code-block:: xml 


	<?xml version="1.0" encoding="UTF-8"?>
	<!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
	<beans>
	  <bean id="superpowersMapper" class="com.mycompany.security.SuperpowersMapper"/>
	</beans>

At this point you can drop the ``authkey`` jar along with your custom mapper jar and the module
will pick up your mapper instead of the default property based one.