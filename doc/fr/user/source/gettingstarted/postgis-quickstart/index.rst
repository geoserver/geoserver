.. _postgis_quickstart:

Ajouter une table PostGIS
==========================

Ce tutorial montre les étapes pour publier une table PostGIS avec GeoServer.

.. note::

   Ce tutorial suppose que GeoServer fonctionne sur http://localhost:8090/geoserver/web.

.. note::

  Ce tutorial suppose que PostGIS a été précédemment instgall sur le système.

Commencer
----------

#. Téléchargez le fichier :download:`nyc_buildings.zip`. Il contient un dump 
   PostGIS d'un sous jeu de données des bâtiments de la ville de New York qui 
   sera utilisé dans ce tutorial.

#. Créez une base de données PostGIS nommée "nyc". Cela peut être réalisé avec 
   les commandes suivantes :
   ::

         createdb -T template_postgis nyc

   si l'installation de PostGIS n'a pas définie le modèle "postgis_template" alors 
   la séquence suivante fera la même chose :
   ::

        ...

#. Décompressez ``nyc_buildings.zip`` quelque part sur le système de fichier. 
   Vous obtiendrez un fichier ``nyc_buildings.sql``. 

#. Importez ``nyc_buildings.sql`` dans la base ``nyc``:
   ::

         psql -f nyc_buildings.sql nyc


Créer un nouveau store
-----------------------

La première étape est de créer un * dataStore* pour la base de données PostGIS 
"nyc". Le dataStore dira à GeoServer comment se connecter à la base de données.

    #. Dans un navigateur web allez sur http://localhost:8080/geoserver.

    #. Naviguez vers :menuselection:`Data-->Stores`.

	.. figure:: datastores.png
	   :align: center

	   *Ajouter un nouveau dataStore.*

    #. Créez un nouveau dataStore en cliquant sur le lien ``PostGIS NG``.

    #. En gardant le :guilabel:`Workspace` par défaut entrez le nom et la 
       description dans :guilabel:`informations basiques du store`.

	.. figure:: basicStore.png
	   :align: center

	   *Information basique du Store*

    #. Définissez les :guilabel:`paramètres de connexion` de la base PostGIS.

       .. list-table::

          * - ``dbtype``
            - postgisng
          * - ``host``
            - localhost
          * - ``post``
            - 5432
          * - ``database``
            - nyc
          * - ``schema``
            - public
          * - ``user``
            - postgres
          * - ``passwd``
            - entrez le mot de passe de postgres
          * - ``validate connections``
            - activez avec la case à cocher

       .. note::

          Les paramètres spécifiques **username** et **password** de l'utilisateur 
          qui a créé la base de données. En fonction de la manière dont PostgreSQL 
          est configuré le paramètre peut ne pas être nécessaire.
           
		.. figure:: connectionParameters.png
		   :align: center

		   *Paramètres de connexion*

    #. Cliquez sur le bouton ``Sauver``.

Configuration de la couche 
----------------------------

    #. Naviguez vers :menuselection:`Données-->Couches`.

    #. Sélectionez le bouton :guilabel:`Ajoutez une nouvelle ressource`.
	
    #. À partir de la liste déroulante :guilabel:`Choix de la nouvelle couche`, 
       sélectionnez cite:nyc_buidings.
	
	.. figure:: newlayerchooser.png
	   :align: center

	   *Sélection de la nouvelle couche dans la liste déroulante*	
	
    #. Dans la ligne de la couche résultante, sélectionnez le nom de la couche 
       nyc_buildings. 

	.. figure:: layerrow.png
	   :align: center

	   *Nouvelle ligne de la couche*
	
    #. La configuration suivante définie les paramètres de données et de 
       publication pour une couche. Entrez les :guilabel:`Informations basiques 
       de la ressource` pour nyc_buildings.  
	
	.. figure:: basicInfo.png
	   :align: center

	   *Information basique de la ressource*
	
    #. Générez les *limites* de la table de la base de données en cliquant sur 
       :guilabel:`Calcul à partir des données` puis sur :guilabel:`Calcul à partir des limites natives.`
	
	.. figure:: boundingbox.png
	   :align: center

	   *Générez la Bounding Box*
	
    #. Définissez le *style* de la couche d'abord en allant sur l'onglet 
       :guilabel:`Publication`.  

    #. Puis sélectionnez :guilabel:`polygone` à partir de la liste déroulante 
       :guilabel:`Style par défaut`.

	.. figure:: style.png
	   :align: center

	   *Sélection du style par défaut*
    
    #. Terminer la configuration de vos données et leur publication en descendant 
       en bas de la page et en cliquant sur :guilabel:`Sauver`.

Prévisualiser la couche
------------------------

   #. Afin de vérifier que la couche nyc_building est publiée nous allons la 
      prévisualiser. Naviguez sur :guilabel:`Prévisualiser la couche` et chercher 
      le lien de cite:nyc_building.

	.. figure:: layer-preview.png
	   :align: center

	   *Prévisualiser la couche*

   #. Cliquez sur le lien :guilabel:`OpenLayers` sour la colonne :guilabel:`Formats communs`. 

   #. Succès ! Une carte OpenLayers doit se charger avec le style par défaut des 
      polygones.

	.. figure:: openlayers.png
	   :align: center

	   *Carte OpenLayers de nyc_buildings*

.. yjacolin at free.fr 2011/07/07 r16069
