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
-----------

The ``authkey`` module is meant to be used with OGC services. It won't work properly against the
administration GUI, nor RESTConfig.

Key providers
-------------

Key providers are responsible for mapping the authentication keys to a user. The authentication key
itself is a UUID (Universal unique Identifier). A key provider needs a user/group service and it is
responsible for synchronizing the authentication keys with the users contained in this service.  

Key provider using user properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This key provider uses a user property ``UUID`` to map the authentication key to the user. User 
properties are stored in the user/group service. Synchronizing is simple since the logic has
to search for users not having the property ``UUID`` and add it. The property value is a generated
UUID.

   UUID=b52d2068-0a9b-45d7-aacc-144d16322018

If the user/group service is read only, the property has to be added from outside, no synchronizing
is possible.


Key provider using a property file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This key provider uses a property file named ``authkeys.properties``. The default user/group service
is named ``default``. The ``authkeys.properties`` for this service would be located at

``$GEOSERVER_DATA_DIR/security/usergroup/default/authkeys.propeties``
 
A sample file looks as follows::

  # Format is authkey=username
  b52d2068-0a9b-45d7-aacc-144d16322018=admin
  1825efd3-20e1-4c18-9648-62c97d3ee5cb=sf
  ef18d7e7-963b-470f-9230-c7f9de166888=topp

This key provider also works for read only user/group services. Synchronizing adds new users not
having an entry in this file and removes entries for users deleted in the user/group service.

Configuration
-------------

Configuration can be done using the administrator GUI. There is a new type of authentication filter
named **authkey** offering the following options.

#. URL parameter name. This the name of URL parameter used in client HTTP requests. Default is ``authkey``.
#. Key Provider. GeoSever offers the providers described above.
#. User/group service to be used.

After configuring the filter it is necessary to put this filter on the authentication filter chain(s).

.. note::
   
   The administrator GUI for this filter has button **Synchronize**. Clicking on this button 
   saves the current configuration and triggers a synchronize. If users are added/removed from 
   the backing user/group service, the synchronize logic should be triggered.

Provider pluggability
---------------------

With some Java programming it is possible to programmatically create and register a new key to user 
name mapper that works under a different logic. 
For example, you could have daily tokens, token generators and the like.

In order to provide your custom mapper you have to implement the ``org.geoserver.securityAuthenticationKeyMapper``
interface and then register said bean in the Spring application context. Alternatively it is possible
to subclass from ``org.geoserver.security.AbstractAuthenticationKeyMapper``. A mapper (key provider) has
to implement

.. code-block:: java 

   
   /**
    * 
    * Maps a unique authentication key to a user name. Since user names are
    * unique within a {@link GeoServerUserGroupService} an individual mapper
    * is needed for each service offering this feature.
    * 
    * @author Andrea Aime - GeoSolution
    */
   public interface AuthenticationKeyMapper extends BeanNameAware {
   
       /**
        * Maps the key provided in the request to the {@link GeoServerUser} object
        * of the corresponding user, or returns null
        * if no corresponding user is found
        * 
        * Returns <code>null</code> if the user is disabled
        * 
        * @param key
        * @return
        */
       GeoServerUser getUser(String key) throws IOException;
       
       /**
        * Assures that each user in the corresponding {@link GeoServerUserGroupService} has
        * an authentication key.
        * 
        * returns the number of added authentication keys
        * 
        * @throws IOException
        */
       int synchronize() throws IOException;
               
       /**
        * Returns <code>true</code> it the mapper can deal with read only u 
        * user/group services
        * 
        * @return 
        */
       boolean supportsReadOnlyUserGroupService();
       
       String getBeanName();
       
       void setUserGroupServiceName(String serviceName);
       String getUserGroupServiceName();
       
       public GeoServerSecurityManager getSecurityManager();
       public void setSecurityManager(GeoServerSecurityManager securityManager);
       
   
}
   
        
The mapper would have to be registered in the Spring application context in a ``applicationContext.xml``
file in the root of your jar. Example for an implementation named ``com.mycompany.security.SuperpowersMapper``:

.. code-block:: xml 


	<?xml version="1.0" encoding="UTF-8"?>
	<!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
	<beans>
	  <bean id="superpowersMapper" class="com.mycompany.security.SuperpowersMapper"/>
	</beans>

At this point you can drop the ``authkey`` jar along with your custom mapper jar and use it in the
administrator GUI of the authentication key filter. 
