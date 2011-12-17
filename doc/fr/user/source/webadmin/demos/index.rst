.. _webadmin_demos:

Démos
=====

Cette page contient des liens utiles vers différentes page d'information sur 
GeoServer et ses fonctionnalités. Vous n'avez pas besoin d'être identifié dans 
GeoServer pour accéder à cette page.

.. figure:: ../images/demos_view.png
   :align: center
   
   *Page de démos*

Requêtes de démo
-----------------

Cette page propose des exemple de requêtes WMS, WFS et WCS pour GeoServer que 
vous pouvez utiliser, éxaminer et modifier. Sélectionnez une requête à partir de 
la liste déroulante. 

.. figure:: ../images/demos_requests.png
   :align: center

   *Selection des requêtes de démo*

Les requêtes Web Feature Service (:ref:`wfs`) et les Web Coverage Service 
(:ref:`wcs`) afficheront l'URL de la requête et le corps XML. Les requêtes des 
Web Map Service (:ref:`wms`) afficheront seulement l'URL de la requête.


.. figure:: ../images/demos_requests_WFS.png
   :align: center
   
   *Requête DescribeFeatureType d'exemple pour le WFS 1.1*

Cliquez sur :guilabel:`Soumettre` pour envoyer la requête à GeoServer. Pour les 
requêtes WFS et WCS, GeoServer génerera automatiquement une réponse XML.

.. figure:: ../images/demos_requests_schema.png
   :align: center
   
   *Réponse XML à parir d'une requête DescribeFeatureType d'exemple pour du WFS 1.1*

Soumettre un requête GetMap d'un WMS affichera une image basée sur les données 
géographiques fournies.

.. figure:: ../images/demos_requests_WMS_map.png
   :align: left
   
   *Requête GetMap du service WMS par OpenLayers*

Les requêtes GetFeatureInfo des services WMS récupère des informations d'une 
entité particulière d'une image cartographique.

.. figure:: ../images/demos_requests_WMS_feature.png
   :align: left
   
   *Requêtes GetFeatureInfo des services WMS*

.. _srs_list:

SRS
---

GeoServer gère nativement environ 4 à00 Système de Référence Saptiale (SRS), 
appellé également **projection** et d'autres peuvent être ajoutés. Un système de 
Référence Spatiale définie un ellipsoïde, un datum en utilisant cet ellipsoïde et 
un système de coordonnées géocentrique, géographique ou projeté. Cette page liste 
toutes les informations SRS connu par GeoServer.

.. figure:: ../images/demos_SRS.png
   :align: left
   
   *Liste de tous les Sytème de Référence Spatiale (SRS) connu de GeoServer*


La colonne :guilabel:`Code` fait référence à l'entier unique définir par l'auteur 
de ce système de référence spatiale. Chaque code est lié à une page de description 
plus détailée que l'on peut visualiser en cliquant sur ce code.

.. figure:: ../images/demos_SRS_page.png
   :align: left
   
   *Détails pour le SRS EPSG:2000*

Le titre de chaque SRS est composé du nom de l'auteur et de l'identifiant entier 
unique (code) définie par l'auteur. Dans l'exemple ci-dessus, l'auteur est le 
`European Petroleum Survey Group <http://www.epsg.org/>`_ (EPSG) et le code est 
2000. Les champs sont comme ceci :

:guilabel:`Description` : Une courte description du SRS.

:guilabel:`WKT` : Une chaîne de caractères décrivant le SRS. WKT signifie "Well Known Text."

:guilabel:`Zone de validité` : L'étendue du SRS.

.. yjacolin at free.fr 2011/11/18 r13133
