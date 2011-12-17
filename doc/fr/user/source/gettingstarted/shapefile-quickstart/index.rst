.. _shapefile_quickstart:

Ajouter un Shapefile
=====================

Ce tutorial présente les étapes de publication d'un Shapefile avec GeoServer.

.. note::

   Ce tutorial suppose que GeoServer fonctionne sur http://localhost:8090/geoserver/web.

Commencer
----------

#. Téléchargez le fichier :download:`nyc_roads.zip`. Ce fichier contient un 
shapefile des routes de la ville de New York qui sera utilisé dans ce tutorial.

#. Décompressez le fichier `nyc_roads.zip`. Le répertoire extrait est constitué 
   des quatres fichiers suivantes :
   
   ::

      nyc_roads.shp
      nyc_roads.shx
      nyc_roads.dbf
      nyc_roads.prj

#. Déplacer le répertoire nyc_roads dans ``<GEOSERVER_DATA_DIR>/data`` où 
   ``GEOSERVER_DATA_DIR`` est la racine du répertoire données de GeoServer. Si 
   aucun changement n'a été fait dans la structure du fichier de GeoServer, le 
   chemin devrait être ``geoserver/data_dir/data/nyc_roads``. 
 
Créer un nouvel Workspace
---------------------------

La première étape est de créer un *workspace* pour le Shapefile. Le workspace 
est un conteneur utilisé pour grouper des couches semblables.


    #. Dans un navigateur allez sur http://localhost:8080/geoserver/web.

    #. Identifiez vous dans GeoServer comme décrit dans le démarrage rapide 
       :ref:`logging_in`.

    #. Naviguez dans :menuselection:`Données-->Workspaces`.

	.. figure:: ../../webadmin/images/data_workspaces.png
	   :align: center

	   *page Workspaces*

    #. Pour créer un nouvel workspace sélectionnez le bouton 
       :guilabel:`Ajoutez un nouvel workspace`. On vous demandera d'entrer un 
       :guilabel:`Nom` et l':guilabel:`URI du Namespace` du workspace. 

	.. figure:: new_workspace.png
	   :align: center

	   *Configurer un nouvel Worksapce*

    #. Entrez le nom ``nyc_roads`` et ``http://opengeo.org/nyc_roads`` pour l'URI. 
       Un nom de workspace est un nom décrivant votre projet et ne peut excéder 
       dix caractères ou contenir des espaces. L'URI (Uniform Resource 
       Identifier) du Namespace, est typiquement l'URL associée à votre projet, 
       avec peut-être un identifiant différent.
	
	.. figure:: workspace_nycroads.png
	   :align: center

	   *Workspace des routes de la ville de New York*

    #. Cliquez sur le bouton :guilabel:`Soumettre`. GeoServer rajoutera le 
       workspace nyc_roads en bas de la liste des Workspace.  

Créer un Store
---------------

    #. Naviguer vers :menuselection:`Data-->Stores`.

    #. Dans le but d'ajouter les données nyc_roads, nous devons créer un nouveau 
       store. CLiquez sur le bouton :guilabel:`Ajouter une nouveau Store`. Vous 
       serez dirigé vers une liste de types de données que GeoServer peut gérer.

	.. figure:: stores_nycroads.png
	   :align: center

	   *Sources de données*
	
    #. Puisque que nyc_roads est un shapefile, sélectionnez :guilabel:`Shapefile` 
       : *ESRI(tm) Shapefiles (.shp)*.
	
    #. Sur la page :guilabel:`Nouveau source de données vecteur` commencez par 
       configurer les :guilabel:`Informations de base du Store`. Sélectionnez le 
       workspace nyc_roads à partir de la liste déroulante, entrez ``NYC Roads`` 
       pour le nom puis entrez une brève description, comme ``Routes de la ville de New York.``
	
    #. Dans la partie :guilabel:`Paramètres de connexions` définissez la 
       localisation du shapefile--``file:data/nyc_roads/nyc_roads.shp``.  
	
	.. figure:: new_shapefile.png
	   :align: center

	   *Information des données et paramètres de nyc_roads*
	
    #. Pressez Sauver. Vous serrez redirigé vers la page 
       :guilabel:`Choix de la nouvelle couche` pour configurer la couche nyc_roads. 
	
Configuration de couche
------------------------

   #. Sur la page :guilabel:`Choix de la nouvelle couche`, sélectionnez le nom 
      de la couche nyc_roads. 

	.. figure:: new_layer.png
	   :align: center

	   *Choix de la nouvelle couche*
	
   #. La configuration suivante définie les données les paramètres de publication 
      pour une couche. Entrez un :guilabel:`Titre` court et un :guilabel:`Abstract` 
      pour le shapefile nyc_roads. 

	.. figure:: new_data.png
	   :align: center

	   *Information de base des ressources pour un Shapefile*

   #. Générez les *limites* du shapefileen cliquant sur :guilabel:`Calcul à partir des données` 
      puis :guilabel:`Calcul à partir des limites natives`.

	.. figure:: boundingbox.png
	   :align: center

	   *Générer la Bounding Box*
     
   #. Définissez le *style* du Shapefile en allant sur l'onglet :guilabel:`Publication`.  

   #. Puis sélectionnez :guilabel:`ligne` à partir de la liste :guilabel:`Style par défaut`.

	.. figure:: style.png
	   :align: center

	   *Sélection du style par défaut.*
	
   #. Terminer la configuration de vos données et leur publication en descendant 
      en bas de la page et en cliquant sur :guilabel:`Sauver`.

Prévisualiser la couche
------------------------

   #. Afin de vérifier que la couche nyc_roads est publiée nous allons la 
      prévisualiser. Naviguez sur :guilabel:`Prévisualiser la couche` et chercher 
      le lien de nyc_roads:nyc_roads.

	.. figure:: layer_preview.png
	   :align: center

	   *Prévisualiser la couche*

   #. Cliquez sur le lien :guilabel:`OpenLayers` sour la colonne :guilabel:`Formats communs`. 

   #. Succès ! Une carte OpenLayers doit se charger avec le style par défaut de 
      la ligne.

	.. figure:: openlayers.png
	   :align: center

	   *Carte OpenLayers de nyc_roads*

.. yjacolin at free.fr 2011/07/07 r16069
