Configure the Google authentication provider
--------------------------------------------

The first thing to do is to configure the OAuth2 Provider and obtain ``Client ID`` and ``Client Secret`` keys.

#. Obtain OAuth 2.0 credentials from the Google API Console.

   Visit the `Google API Console <https://console.developers.google.com/>`_ to obtain OAuth 2.0 credentials such as a client ID and client secret 
   that are known to both Google and your application. The set of values varies based on what type of application you are building. 
   For example, a JavaScript application does not require a secret, but a web server application does.
   
   * Login with a valid Google Account 
   
     .. figure:: images/google_api_console001.png
        :align: center

   * Click on ``APIs & Services``
   
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

   .. figure:: images/google_api_console007.png
      :align: center

#. Configure a new ``Web application``

   * If it is the first time you create an ``OAuth Client ID``, you will be asked to create a new ``consent screen``
   
     .. figure:: images/google_api_console008.png
        :align: center

   * Customize the ``consent screen``
    
     .. warning:: This step is mandatory only if it's the first time you are defining a ``Web application`` on a new project.
        If you don't have an organization, you can only choose type External from the screen below.

     .. figure:: images/google_api_console009.png
        :align: center
        
   * Fill the form below and click on ``save and continue`` untill all tabs are filled.
   
     .. figure:: images/google_api_console010.png
       :align: center

     .. note:: It can be edited and updated also later (see last point of this section below)
        
   * From the credentials page, click on ``CREATE CREDENTIALS``> ``OAuth Client ID`` and select ``Application type`` -> ``Web application``
   
     .. warning:: This step is mandatory only if it's the first time you are defining a ``Web application`` on a new project.

     .. figure:: images/google_api_console010a.png
        :align: center

   * Add a ``Name`` and the ``Authorized redirect URIs`` like shown here below.
     
     .. note:: This sample creates a client working on the default local URL ``http://localhost:8080/geoserver``. Of course this will work only on a local instance and can't be used for a production system.
               
        However it is possible to add as many ``Authorized redirect URIs`` you need to a new ``Web application``. 
        
        It is also possible to create many ``Client credentials`` with customised ``consent screen`` and ``Web application``, depending on your specific needs.
        Every public GeoServer instance (or cluster of GeoServer belonging to a specific project) should have its own specific ``Client credentials``.
       
     .. figure:: images/google_api_console011.png
        :align: center

     .. note:: Always add two entries for each URI. One without the ending ``/`` and another one with it.
   
     .. figure:: images/google_api_console012.png
        :align: center

#. Click on ``Create`` and take note of the ``Client ID`` and the ``Client Secret``.

   At the end of the procedure Google will show-up a small dialog box with the ``Client ID`` and the ``Client Secret``.
   That info can be always accessed and updated from the `Google API Console <https://console.developers.google.com/>`_
   
   .. figure:: images/google_api_console013.png
      :align: center

#. Optionally customize the ``OAuth consent screen``.

   At any time it is possible to update and customize the ``OAuth consent screen``. You can put here your logo, app name, ToS and so on.

   .. figure:: images/google_api_console014.png
      :align: center

