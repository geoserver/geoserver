.. _community_oidc_github:


Configure the GitHub authentication provider
--------------------------------------------

The first thing to do is to configure the OAuth2 Provider and obtain ``Client ID`` and ``Client Secret`` keys.


#. Go to your `GitHub settings <https://github.com/settings/profile>`_

#. At the very bottom of the left bar, click "<> Developer Settings"

    .. figure:: ../img/github-dev-settings.png
        :align: center

#. On the left, press "OAuth Apps" and then "New OAuth app"

    .. figure:: ../img/github-new-oauth2.png
        :align: center

#. Give the application:

   * A name (i.e. "gs-app")
   * The homepage of the geoserver (i.e. "http://localhost:8080/geoserver")
   * The authorization callback in the form of "http://localhost:8080/geoserver/web/login/oauth2/code/gitHub"
   * Press "Register application"

    .. figure:: ../img/github-oauth2-app.png
        :align: center


#. You will be taken to the application page.  Record the "Client ID" (you will need this in the GeoServer configuration)

    .. figure:: ../img/github-app-created.png
        :align: center

#. Press "Generate a new client secret"

    .. figure:: ../img/github-client-secret.png
        :align: center

#. Record the client secret (you will need this in the GeoServer configuration)

#. At the very bottom (scroll down), press "Update Application"



Configure GeoServer
-------------------

The next step is to configure your Google application as the OIDC IDP for GeoServer.

Create the OIDC Filter
^^^^^^^^^^^^^^^^^^^^^^

   * Login to GeoServer as an Admin
   
   * On the left bar under "Security", click "Authentication", and then "OpenID Connect Login"
       
      .. figure:: ../img/google-gs1.png
         :align: center

   * Give the it a name like "oidc-github", then click the "GitHub Login" checkbox and copy-and-paste in the Client ID and Client Secret (from when you configured the github client).
       
      .. figure:: ../img/github-gs1.png
         :align: center         

   * Go down to the bottom and configure the role source (if you want) - see :ref:`role source <community_oidc_role_source>`.  
     NOTE: GitHub's access token in Opaque (not a JWT) and it does NOT supply an ID Token (it is OAUTH2, not OIDC).

   * Press "Save" 



Allow Web Access (Filter Chain)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  * On the left bar under "Security", click "Authentication", and then click "Web" under "Filter Chains"
       
      .. figure:: ../img/google-filterchain1.png
         :align: center

   * Scroll down, and move the new GitHub OIDC Filter to the Selected side by pressing the "->" button.
       
      .. figure:: ../img/github-gs2.png
         :align: center

   * Move the new GitHub OIDC Filter above "anonymous" by pressing the up arrow button (See above diagram).
       
   * Press "Close"

   * Press "Save" 



Notes
=====

#. When you login, your username will be a number.  For privacy reasons, GitHub does not usually include the email address of the user!

      .. figure:: ../img/github-gs-loggedin.png
         :align: center

#. GitHub's Access Token is opaque, so :ref:`configure roles <community_oidc_role_source>`
#. GitHub is OAUTH2-only (not OIDC) so it does **not** have an ID Token
 