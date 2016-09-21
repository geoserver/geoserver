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
   
   * Login with a valid Google Account 
   
        .. figure:: images/google_api_console001.png
           :align: center

   * Click on ``API Manager``
   
        .. figure:: images/google_api_console002.png
           :align: center

   * Click on ``Credentials``
   
        .. figure:: images/google_api_console003.png
           :align: center
           
        .. note:: The first time you land here, Google will ask to create at least one project
        
            .. figure:: images/google_api_console004.png
               :align: center
               
            For the purpose of this tutorial we will create a sample project. You are free to create other projects or update existing ones through the `Google API Console <https://console.developers.google.com/>`_ later.
            
            .. figure:: images/google_api_console005.png
               :align: center
            
            If no ``Credentials`` are present, you will be asked to create new one.
            
            .. figure:: images/google_api_console006.png
               :align: center

#. Select an existing (or create a new one) ``OAuth Client ID``

   Click on the ``Client credentials`` context menu as shown in the figure below.
   
    .. figure:: images/google_api_console007.png
       :align: center

#. Configure a new ``Web application``

   * If it is the first time you create a ``OAuth Client ID``, you will be asked to create a new ``consent screen``
   
        .. figure:: images/google_api_console008.png
           :align: center

   * Customize the ``consent screen``
    
        .. warning:: This step is mandatory only if it's the first time you are defining a ``Web application`` on a new project.

        .. figure:: images/google_api_console009.png
           :align: center
   
        .. note:: It can be edited and updated also later (see last point of this section below)
        
   * Select ``Application type`` -> ``Web application``
   
        .. warning:: This step is mandatory only if it's the first time you are defining a ``Web application`` on a new project.

        .. figure:: images/google_api_console010.png
           :align: center
   
   * Add a ``Name`` and the ``Authorized redirect URIs`` like shown here below.
   
        .. note:: This sample creates a client working on the default local URL ``http://localhost:8080/geoserver``. Of course this will work only on a local instance and can't be used for a production system.
                  
                  However it is possible to add as many ``Authorized redirect URIs`` you need to a new ``Web application``. 
                  
                  It is also possible create many ``Client credentials`` with customised ``consent screen`` and ``Web application``, depending on your specific needs.
                  Every public GeoServer instance (or cluster of GeoServer belonging to a specific project) should have it's own specific ``Client credentials``.
       
        .. figure:: images/google_api_console011.png
           :align: center

        .. note:: Always add two entries for each URI. One without the ending ``/`` and another one with it.
       
        .. figure:: images/google_api_console012.png
           :align: center

#. Click on ``Create`` and take note of the ``Client ID`` and the ``Client Sectet``.

   At the end of the procedure Google will show-up a small dialog box with the ``Client ID`` and the ``Client Sectet``.
   Those info can be always accessed and updated from the `Google API Console <https://console.developers.google.com/>`_
   
    .. figure:: images/google_api_console013.png
       :align: center

#. Optionally customize the ``OAuth consent screen``.

   At any time it is possible to update and customize the ``OAuth consent screen``. You can put here your logo, app name, ToS and so on.

    .. figure:: images/google_api_console014.png
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

    .. figure:: images/oauth2chain001.png
       :align: center

    The default values provided with the plugin are valid for the Google OAuth2 Provider and are the following one:
    
    .. code-block:: shell

        "Enable Redirect Authentication EntryPoint" = False
        "Access Token URI" = https://accounts.google.com/o/oauth2/token
        "User Authorization URI" = https://accounts.google.com/o/oauth2/auth
        "Redirect URI" = http://localhost:8080/geoserver
        "Check Token Endpoint URL" = https://www.googleapis.com/oauth2/v1/tokeninfo
        "Logout URI" = https://accounts.google.com/logout
        "Scopes" = https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile
    
    .. note:: 
	
	   #. ``Client ID`` and ``Client Secret`` are the ones Google provided
	   
	   #. Choose a ``Role Service`` able to recognize user emails as IDs. By default a connected user will have ``ROLE_USER`` role
       
    .. warning:: Few workds on **Enable Redirect Authentication EntryPoint** option
    
            This option allows you to decide whether or not *force* automatic redirection to OAuth2 Access Token URI or not for authentication.
            
            What does that means?
            
            * *Enable Redirect Authentication EntryPoint* = True
            
                If not already authenticated (or no valid **Access Token** is provided in the query string), this option will **force** a redirection to the OAuth2 Provider Login page.
                
                This may cause unwanted behavior since it will override every other esplicit login method like ``form``. In other words if the filter is applied for instance to the ``web`` endpoint, it won't be possible to access to the GeoServer Admin GUI using the standard login method via browser.
                
            * *Enable Redirect Authentication EntryPoint* = False
            
                In order to avoid the above issue, by disabling this option you will be **forced** to use an esplicit Authentication Endpoint to login via the OAuth2 Provider login page.
                
                If not already authenticated (or no valid **Access Token** is provided in the query string), you **must** authenticate through the following URLs:
                
                #. *GeoServer OAuth2 Authorization Endpoint*; ``http://<host:port>/geoserver/j_spring_outh2_login``
                
                #. *OAuth2 Provider Esplicit User Authorization Endpoint*; this must be adapted for your specific OAuth2 Provider, the protocol stated that it should be 
                
                    ::
                    
                        https://<USER_AUTHORIZATION_URI>?scope=<SCOPES>&response_type=code&redirect_uri=<REDIRECT_URI>&client_id=<CLIENT_ID>
                
                    For google OAuth2 Provider is:
                    
                    ::
                    
                        https://accounts.google.com/o/oauth2/auth?scope%3Dhttps://www.googleapis.com/auth/userinfo.email%2Bhttps://www.googleapis.com/auth/userinfo.profile%26response_type%3Dcode%26redirect_uri%3D<REDIRECT_URI>%26client_id%3D<CLIENT_ID>
	
#. Update the filter chains by adding the new OAuth2 filter.

   Once everything has been configured you should be able to see the new ``oauth2`` filter available among the ``Authentication Filters`` list
   
   .. figure:: images/oauth2filter001.png
      :align: center
   
   Through this it will be always possible to modify / update the filter options, or create more of them.
   
   The next step is to add the filter to the ``Filter Chains`` you want to protect with OAuth2 also
   
   .. figure:: images/oauth2filter002.png
      :align: center

#. Select the OAuth2 Filter for each filter chain you want to protect with OAuth2.

   If you need to protect **all** the GeoServer services and the GeoServer Admin GUI too with OAuth2, you need to add the ``oauth2`` filter to all the following chains
   
   * ``web``
   
   * ``rest``
   
   * ``gwc``
   
   * ``default``
   
   The order of the authentication filters depends basically on which method you would like GeoServer *try first*.
   
   .. note:: During the authtentication process, the authentication filters of a ``Filter Chain`` are executed serially until one succeed (for more details please see the section :ref:`security_auth_chain`)
   
   .. warning:: If *Enable Redirect Authentication EntryPoint* = **True** for OAuth2 Filter, the ``web`` chain won't be able to login through the ``form`` method.
   
   .. figure:: images/oauth2filter003.png
      :align: center

   .. note:: Remember that the ``anonymous`` filter must be always the last one.

#. Save.

   .. figure:: images/oauth2filter004.png
      :align: center

Test the Google OAuth2 Provider Based Login
-------------------------------------------

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

