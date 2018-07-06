.. _security_tutorials_cas:

Authentication with CAS
=======================

This tutorial introduces GeoServer CAS support and walks through the process of
setting up authentication against a CAS server. It is recommended that the 
:ref:`security_auth_chain` section be read before proceeding.

CAS server certificates
-----------------------

A running `CAS server <https://apereo.github.io/cas/5.3.x/index.html>`_ is needed. 

The first step is to import the server certificates into the the GeoServer JVM.

If you need to export the `CRT` from the CAS server, you must execute the following 
command on the server JVM:: 

  keytool -export -alias <server_name> -keystore <cas_jvm_keystore_path> -file server.crt

Once you have the `server.crt` file, the procedure to import the certificate into 
the JVM is the following one::

  keytool -import -trustcacerts -alias <server_name> -file server.crt -keystore <path_to_JRE_cacerts>

Enter the keystore password and confirm the certificate to be trustable.

Configure the CAS authentication provider
------------------------------------------

#. Start GeoServer and login to the web admin interface as the ``admin`` user.
#. Click the ``Authentication`` link located under the ``Security`` section of
   the navigation sidebar.

    .. figure:: images/cas1.jpg
       :align: center

#. Scroll down to the ``Authentication Filters`` panel and click the ``Add new`` link.

    .. figure:: images/cas2.jpg
       :align: center

#. Click the ``CAS`` link.

    .. figure:: images/cas3.jpg
       :align: center

#. Fill in the fields of the settings form as follows:

    .. figure:: images/cas4.jpg
       :align: center
   
#. Update the filter chains by adding the new CAS filter. 

   .. figure:: images/cas5.jpg
      :align: center

#. Select the CAS Filter for each filter chain you want to protect with CAS. 

   .. figure:: images/cas6.jpg
      :align: center

   Be sure to select and order correctly the CAS Filter.

#. Save.

Test a CAS login
-----------------

#. Navigate to the GeoServer home page and log out of the admin account. 
#. Try to login again, you should be able now to see the external CAS login form.

   .. figure:: images/cas7.jpg
      :align: center

