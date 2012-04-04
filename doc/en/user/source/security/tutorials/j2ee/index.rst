.. _sec_tutorials_j2ee:

Configuring J2EE Authentication
===============================

Servlet containers such as Tomcat and Jetty offer their own options for 
authentication. Often it is desirable for an application such as GeoServer 
to use that existing authentication mechanisms rather than require its own
authentication configuration.

J2EE authentication allows GeoServer to delegate to the servlet container for
authentication. This tutorial walks through the process of setting up J2EE
authentication.

Prerequisites
-------------

This tutorial requires a servlet container capable of doing its own authentication. 
This tutorial uses Tomcat.

Deploy GeoServer in tomcat before proceeding.

Configure the J2EE authentication filter
----------------------------------------

In order to delegate to the container for authentication a filter must first be 
configured to recognize the container authentication.

#. Login to the GeoServer web admin interface as the ``admin`` user.
#. Click the ``Authentication`` link located under the ``Security`` section of
   the navigation sidebar.
   
   .. figure:: images/j2ee1.jpg
      :align: center
   
#. Scroll down to the ``Authentication Filter`` panel and click the ``Add new`` link.
#. Create a new filter named "j2ee" and fill out the settings form 
   as follows:
   
   * Set the ``Role service`` to "default"

   .. figure:: images/j2ee2.jpg
      :align: center

#. Save

#. Back on the authentication page scroll down to the ``Filter Chains`` panel. 
#. Select "Web UI" from the ``Request type`` drop down.
#. Select the ``j2ee`` filter and position it after the ``anonymous`` filter. 

   .. figure:: images/j2ee3.jpg
      :align: center

#. Save.

Configure Tomcat for authentication
-----------------------------------

By default Tomcat does not require authentication for web applications. In this 
section Tomcat will be configured to secure GeoServer requiring a basic authentication
login.

#. Shut down Tomcat.
#. Edit the ``conf/tomcat-users.xml`` under the Tomcat root directory and add a user 
   named "admin"::
   
     <user username="admin" password="password" roles="tomcat"/>
   
#. Edit the GeoServer ``web.xml`` file located at ``webapps/geoserver/WEB-INF/web.xml``
   under the Tomcat root directory and add the following at the end of the file directly
   before the closing ``</web-app>`` element::
   
    <security-constraint>
      <web-resource-collection>
        <url-pattern>/*</url-pattern>
         <http-method>GET</http-method>
         <http-method>POST</http-method>
      </web-resource-collection>
      <auth-constraint>
        <role-name>tomcat</role-name>
      </auth-constraint>
    </security-constraint>

    <login-config>
      <auth-method>BASIC</auth-method>
    </login-config>

#. Save ``web.xml`` and restart Tomcat.   

Test J2EE login
---------------

#. Navigate to the GeoServer web admin interface. The result should be a prompt
   to authenticate.
#. Enter in the username "admin" and password "password"

   .. figure:: images/j2ee4.jpg
      :align: center

The result should be the admin user logged into the GeoServer web admin.