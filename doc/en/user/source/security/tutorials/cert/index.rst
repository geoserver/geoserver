.. _security_tutorials_cert:

Configuring X.509 Certificate Authentication
============================================

Certificate authentication involves the usage of public/private keys to identify oneself. This represents a much more secure alternative to basic user name and password schemes.

`X.509 <http://en.wikipedia.org/wiki/X.509>`_ is a well defined standard for the format of public key certificates. This tutorial walks through the process of setting up X.509 certificate authentication.

Prerequisites
-------------

This tutorial assumes the following:

* A web browser that supports the usage of client certificates for authentication, also referred to as "two-way SSL". This tutorial uses **Firefox**.
* An SSL-capable servlet container. This tutorial uses **Tomcat**.
* GeoServer is deployed in Tomcat.

Configure the user/group service
--------------------------------

Users authenticated via a X.509 certificate must be configured in GeoServer. For this a new user/group service will be added.

#. Login to the web admin interface as the ``admin`` user.

#. Click the ``Users, Groups, and Roles`` link located under the ``Security`` section of
   the navigation sidebar.
   
   .. figure:: images/cert1.jpg
   
#. Scroll down to the ``User Group Services`` panel and click the ``Add new`` link.

#. Create a new user/group service named :kbd:`cert-ugs` and fill out the settings form as follows:
   
   * Set :guilabel:`Password encryption` to :kbd:`Empty` since users will not authenticate via password.
   * Set :guilabel:`Password policy` to :kbd:`default`.

   .. figure:: images/cert2.jpg

#. Click :guilabel:`Save`.

#. Back on the ``Users, Groups, and Roles`` page, click the :guilabel:`cert-ugs` link.

   .. figure:: images/cert3.jpg

#. Select the :guilabel:`Users` tab and click the :guilabel:`Add new user` link.

   .. figure:: images/cert4.jpg

#. Add a new user named :kbd:`rod` the and assign the ``ADMIN`` role.

   .. figure:: images/cert5.jpg

#. Click :guilabel:`Save`.

#. Click the :guilabel:`Authentication` link located under the :guilabel:`Security` section of the navigation sidebar.

    .. figure:: images/cert6.jpg

#. Scroll down to the :guilabel:`Authentication Filters` panel and click the :guilabel:`Add new` link.

    .. figure:: images/cert7.jpg

#. Click the :guilabel:`X.509` link and fill out form as follows:

   * Set :guilabel:`Name` to "cert"
   * Set :guilabel:`Role source` to :kbd:`User group service` and set the associated drop-down to :kbd:`cert-ugs`

   .. figure:: images/cert8.jpg

#. Click :guilabel:`Save`.

#. Back on the authentication page, scroll down to the :guilabel:`Filter Chains` panel. 

#. Click :guilabel:`web` in the :guilabel:`Name` column.

#. Select the :guilabel:`cert` filter and position it after the :guilabel:`rememberme` filter. 

   .. figure:: images/cert9.jpg

#. Click :guilabel:`Close`.

#. You will be returned to the previous page. Click :guilabel:`Save`.

   .. warning::

      This last change requires both :guilabel:`Close` and then :guilabel:`Save` to be clicked. You may wish to return to the :guilabel:`web` dialog to verify that the change was made.

Download sample certificate files
---------------------------------

Rather than demonstrate how to create or obtain valid certificates, which is beyond the scope of this tutorial, sample files available as part of the spring security `sample applications <https://github.com/SpringSource/spring-security/tree/master/samples/certificates>`_ will be used.

Download and unpack the :download:`sample certificate files <sample_certs.zip>`. This archive contains the following files:

* :file:`ca.pem` is the certificate authority (CA) certificate issued by the "Spring Security Test CA" certificate authority. This file is used to sign the server and client certificates.
* :file:`server.jks` is the Java keystore containing the server certificate and private key used by Tomcat and presented to the user during the setup of the SSL connection.
* :file:`rod.p12` contains the client certificate / key combination used to perform client authentication via the web browser.

Configure Tomcat for SSL
------------------------

#. Copy the :file:`server.jks` file into the :file:`conf` directory under the root of the Tomcat installation.

#. Edit the Tomcat :file:`conf/server.xml` and add an SSL connector:

   .. code-block:: xml

       <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true" scheme="https" secure="true"
            clientAuth="true" sslProtocol="TLS" 
            keystoreFile="${catalina.home}/conf/server.jks"
            keystoreType="JKS" keystorePass="password"
            truststoreFile="${catalina.home}/conf/server.jks"
            truststoreType="JKS" truststorePass="password" />

   This enables SSL on port 8443.

#. By default, Tomcat has APR enabled. To disable it so the above configuration can work, remove or comment out the following line in the server.xml configration file

    .. code-block:: xml

      <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />   

#. Restart Tomcat.

Install the client certificate
------------------------------

#. In Firefox, select :guilabel:`Preferences` (or :menuselection:`Tools --> Options`) and navigate to the :guilabel:`Advanced` panel.

#. Select the :guilabel:`Encryption` tab (or the :guilabel:`Certificates` tab, depending on your version) and click the :guilabel:`View Certificates` button.

    .. figure:: images/cert10.jpg

#. On the :guilabel:`Your Certificates` panel click the :guilabel:`Import` button and select the :file:`rod.p12` file.

#. When prompted enter in the password :kbd:`password`.

    .. figure:: images/cert11.jpg

#. Click :guilabel:`OK` and close the Firefox Preferences.

Test certificate login
----------------------

#. Navigate to the GeoServer admin on port "8443" using HTTPS: https://localhost:8443/geoserver/web

#. You will be prompted for a certificate. Select the :guilabel:`rod` certificate for identification.

    .. figure:: images/cert12.jpg

#. When warned about the self-signed server certificate, click :guilabel:`Add Excception` to add a security exception.

    .. figure:: images/cert13.jpg

The result is that the user ``rod`` is now logged into the GeoServer admin interface.

    .. figure:: images/cert14.jpg

.. note:: 

  Starting with version 31, Firefox implements a new mechanism for using certificates, which will cause a *Issuer certificate is invalid error (sec_error_ca_cert_invalid)* error when trying to use a self-signed repository such as the one proposed. To avoid that, you can disable this mechanism by browsing to **about:config** and setting the **security.use_mozillapkix_verification** parameter to **false**.

    .. figure:: images/mozilla_pki.jpg