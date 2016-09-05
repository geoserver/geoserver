.. _security_tutorials_oauth2:

Authentication with OAuth2
==========================

This tutorial introduces GeoServer OAuth2 support and walks through the process of
setting up authentication aganist an OAuth2 provider. It is recommended that the 
:ref:`security_auth_chain` section be read before proceeding.

OAuth2 Protocol and GeoServer OAuth2 core module
------------------------------------------------

This module allows GeoServer to authenticate against the `OAuth2 Protocol <https://tools.ietf.org/html/rfc6749>`_.

In order to let the module work, it's mandatory to setup and configure both ``oauth2`` and ``oauth2-xxxx-extension``.

The first one contains the necessary dependencies of the OAuth2 core module. This module contains the implementation of the 
GeoServer security filter, the base classes for the OAuth2 Token services and the GeoServer GUI panel.

Since in almost all cases the only thing different between OAuth2 Providers are the endpoint URIs and the client connection
information (not only the keys - public and secret - but also the user profile representations), in order to allow GeoServer
connecting to a specific OAuth2 provider it is sufficient to install the OAuth2 Core module plugin (and correctly configure
the parameters through the GeoServer GUI - see next section for the details) and the concrete implementation of the OAuth2
REST token template and resource details.

Currently this module is shipped with a sample extension for Google OAuth2 Provider. This is a particular case since the 
Google JWT response is not standard and therefore we had to define and inject also a ``GoogleUserAuthenticationConverter`` taking
the Google REST response against a valid ``access_token`` and converting it to a OAuth2 standard one.

Other than this the most interesting part is the implementation of the base class ``GeoServerOAuth2SecurityConfiguration``.

The latter contains the Google implementation of the ``OAuth2RestTemplate``.

In the next section we will see how to install and configure the OAuth2 security filter on GeoServer authenticating against 
Google OAuth2 Provider.

Configure the Google authentication provider
--------------------------------------------

The first thing to do is to configure the OAuth2 Provider and obtain ``Client ID`` and ``Client Sectet`` keys.

#. Obtain OAuth 2.0 credentials from the Google API Console.

   Visit the `Google API Console <https://console.developers.google.com/>`_ to obtain OAuth 2.0 credentials such as a client ID and client secret 
   that are known to both Google and your application. The set of values varies based on what type of application you are building. 
   For example, a JavaScript application does not require a secret, but a web server application does.
   
    .. figure:: images/google1.jpg
       :align: center

#. Create a new ``OAuth client ID``

   Click on the ``Client credentials`` context menu as shown in the figure below.
   
    .. figure:: images/google2.jpg
       :align: center

#. Configure a new ``Web application``

   Add the ``Name`` and the ``Authorized redirect URIs`` like shown here below.
   
    .. note:: Always add two entries for each URI. One without the ending ``/`` and another one with it.
   
    .. figure:: images/google3.jpg
       :align: center

#. Take note of the ``Client ID`` and the ``Client Sectet``.

   At the end of the procedure Google will show-up a small dialog box with the ``Client ID`` and the ``Client Sectet``.
   Those info can be always accessed and updated from the `Google API Console <https://console.developers.google.com/>`_
   
    .. figure:: images/google4.jpg
       :align: center

#. Optionally customize the ``OAuth consent screen``.

   At any time it is possible to update and customize the ``OAuth consent screen``. You can put here your logo, app name, ToS and so on.

    .. figure:: images/google5.jpg
       :align: center

Configure the GeoServer OAuth2 filter
-------------------------------------

#. Start GeoServer and login to the web admin interface as the ``admin`` user.
#. Click the ``Authentication`` link located under the ``Security`` section of
   the navigation sidebar.

    .. figure:: images/filter1.jpg
       :align: center

#. Scroll down to the ``Authentication Filters`` panel and click the ``Add new`` link.

    .. figure:: images/filter2.jpg
       :align: center

#. Click the ``OAuth2`` link.

    .. figure:: images/filter3.jpg
       :align: center

#. Fill in the fields of the settings form as follows:

    .. figure:: images/filter4.jpg
       :align: center
   
    .. note:: 
	
	   #. ``Scopes`` should be::
	       
		   https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile
		
	   #. ``Client ID`` and ``Client Secret`` are the ones Google provided
	   
	   #. Choose a ``Role Service`` able to recognize user emails as IDs. By default a connected user will have ``ROLE_USER`` role
	
#. Update the filter chains by adding the new OAuth2 filter. 

   .. figure:: images/filter5.jpg
      :align: center

#. Select the OAuth2 Filter for each filter chain you want to protect with OAuth2.

   .. figure:: images/filter6.jpg
      :align: center

   Be sure to select and order correctly the OAuth2 Filter.

#. Save.

Test a Google OAuth2 login
--------------------------

#. Navigate to the GeoServer home page and log out of the admin account. 
#. Try to login again, you should be able now to see the external Google login form.

   .. figure:: images/test1.jpg
      :align: center

   .. figure:: images/test2.jpg
      :align: center

   .. figure:: images/test3.jpg
      :align: center

   .. figure:: images/test4.jpg
      :align: center

   .. figure:: images/test5.jpg
      :align: center

