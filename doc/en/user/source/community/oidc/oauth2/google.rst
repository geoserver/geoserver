.. _community_oidc_google:


Configure the Google authentication provider
============================================

The first thing to do is to configure the OAuth2 Provider and obtain ``Client ID`` and ``Client Secret`` keys.

Configure the Google IDP
------------------------

#. Obtain OAuth 2.0 credentials from the Google API Console.

   Visit the `Google API Console <https://console.developers.google.com/>`_ to obtain OAuth 2.0 credentials such as a client ID and client secret 
   that are known to both Google and your application. The set of values varies based on what type of application you are building. 
   For example, a JavaScript application does not require a secret, but a web server application does.
   
   * Login with a valid Google Account 
   * Click on ``Create project``
   
     .. figure:: ../img/google-create-project1.png
        :align: center
        
   * give the project a name like ``geoserver-oidc`` and press "Create"
   
     .. figure:: ../img/google-create-project2.png
        :align: center

   * Click on ``Credentials`` (left column)
   
     .. figure:: ../img/google-credentials.png
        :align: center

   * Click on "+ Create credentials" (top bar)
   
     .. figure:: ../img/google-credentials2.png
        :align: center

   * Choose "OAuth client ID"
   
     .. figure:: ../img/google-credentials3.png
        :align: center

   * Click on "Configure consent Screen"
   
     .. figure:: ../img/google-credentials4.png
        :align: center

   * Press "Get Started"
   
     .. figure:: ../img/google-credentials5.png
        :align: center

   * Type in an "App name" (like "test-gs"), choose your Email address, and then press "Next"
   
      .. figure:: ../img/google-credentials6.png
         :align: center

   * In the Audience section, choose "External" then press "Next"
   
      .. figure:: ../img/google-credentials7.png
         :align: center

   * Type in a contact email, then press "Next"
   
      .. figure:: ../img/google-credentials8.png
         :align: center

   * Agree to the terms, then press "Continue", and then "Create"
   
      .. figure:: ../img/google-credentials9.png
         :align: center

   * Go to Clients (Left Bar), press the 3-vertical-dots ,and then press "+ Create Client"
   
      .. figure:: ../img/google-credentials10.png
         :align: center

   * Choose "Web Application" and name the web application  (i.e. "gs test app")
   
      .. figure:: ../img/google-credentials11.png
         :align: center

   * Go down to "Authorized redirect URIs" and press "+ Add URI", type in "http://localhost:8080/geoserver/web/login/oauth2/code/google", then press "Create"
   
      .. figure:: ../img/google-credentials12.png
         :align: center

   .. tip::

      The exact redirect URI that GeoServer will use is shown as the read-only
      :guilabel:`Redirect URI` field in the filter configuration form. In production,
      use that value instead of ``localhost``. See :ref:`Redirect Base URI <community_oidc_redirect_base_uri>`.

   *  Record your Client ID and Client Secret, then press "Ok"
    
      * **You will not be able to retrieve your client secret once you press "ok"**
   
      .. figure:: ../img/google-credentials13.png
         :align: center


   *  Go to "Audience" (left bar), go down to "Test Users", press "+Add users", and add your google email as the test user.
       
      .. figure:: ../img/google-credentials14.png
         :align: center

   * Press Save


Configure GeoServer
-------------------

The next step is to configure your Google application as the OIDC IDP for GeoServer.

Create the OIDC Filter
^^^^^^^^^^^^^^^^^^^^^^

   * Login to GeoServer as an Admin
   
   * On the left bar under "Security", click "Authentication", and then "OpenID Connect Login"
       
      .. figure:: ../img/google-gs1.png
         :align: center

   * Give the it a name like "test-google", then from the :guilabel:`Provider` dropdown select :guilabel:`Google` and copy-and-paste in the Client ID and Client Secret (from when you configured the google client).
       
      .. figure:: ../img/google-gs2.png
         :align: center

   * Go down to the bottom and configure the role source (if you want) - see :ref:`role source <community_oidc_role_source>`

   * Press "Save" 

Allow Web Access (Filter Chain)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  * On the left bar under "Security", click "Authentication", and then click "Web" under "Filter Chains"
       
      .. figure:: ../img/google-filterchain1.png
         :align: center

   * Scroll down, and move the new Google OIDC Filter to the Selected side by pressing the "->" button.
       
      .. figure:: ../img/google-filterchain2.png
         :align: center

   * Move the new Google OIDC Filter above "anonymous" by pressing the up arrow button.
       
      .. figure:: ../img/google-filterchain3.png
         :align: center

   * Press "Close"

   * Press "Save" 


Notes
-----

See :ref:`troubleshooting <community_oidc_troubleshooting>`.

1. Google's Access Token is opaque, so :ref:`configure roles <community_oidc_role_source>` via the ID Token
2. Google's ID Token does not contain very much info


      .. code-block:: json

            {
               "iss": "https://accounts.google.com",
               "azp": "...",
               "aud": "...",
               "sub": "..",
               "email": "dblasby@gmail.com",
               "email_verified": true,
               "at_hash": "1iKn2vPzlGpK-aY2n3",
               "nonce": "Gi-fBHjrpUdC3o8K6zYhIbEdv1Jz6Zu0IF3sIT",
               "name": "David Blasby",
               "picture": "https://lh3.googleusercontent.com/a/ACg8ocLEhY",
               "given_name": "David",
               "family_name": "Blasby",
               "iat": 175,
               "exp": 175
            }
