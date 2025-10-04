.. _community_oidc_filter:

Update the Filter chains
========================

#. Update the filter chains by adding the new OAUTH2/OIDC filter.

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
   
   The order of the authentication filters depends basically on which method you would like GeoServer to *try first*.
   
   .. note:: During the authentication process, the authentication filters of a ``Filter Chain`` are executed serially until one succeed (for more details please see the section :ref:`security_auth_chain`)
   
   .. warning:: If *Enable Redirect Authentication EntryPoint* = **True** for OAuth2 Filter, the ``web`` chain won't be able to login through the ``form`` method.
   
   .. figure:: images/oauth2filter003.png
      :align: center

   .. note:: Remember that the ``anonymous`` filter must be always the last one.

#. Save.

   .. figure:: images/oauth2filter004.png
      :align: center

 