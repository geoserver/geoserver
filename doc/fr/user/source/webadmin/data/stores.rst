.. _webadmin_stores:

Entrepôt
==========

Un entrepôt connecte une source de données qui contient des données raster ou 
vecteur. Une source de données peut être un fichier ou un groupe de fichier autant 
qu'une table dans une base de données, un fichier unique (comme un shapfile) ou 
un répertoire (telle que la bibliothèque du Format de Produit Vecteur). L'entrepôt 
construit est utilisé afin que les paramètres de connexion soient définie qu'une 
seule fois, plutôt que pour chaque données à partir d'une source. Pour cela, il 
est nécessaire d'enregistrer un entrepôt avant le chargement des données.

.. figure:: ../images/data_stores.png
   :align: center
   
   *Vues des stores*

Bien qu'il peut y avoir plusieurs formats potentiels pour une source de données, 
il y a seulement quatre types d'entrepôts. Pour les données raster, un entrepôt peut 
être un fichier. Pour les données vecteur, un entrepôt peut être un fichier, une 
base de données ou un serveur.

.. list-table::
   :widths: 15 85 

   * - **Icône du type**
     - **Description**
   * - .. image:: ../images/data_stores_type1.png
     - données raster dans un fichier
   * - .. image:: ../images/data_stores_type3.png
     - données vecteur dans un fichier
   * - .. image:: ../images/data_stores_type2.png
     - données vecteur dans une base de données
   * - .. image:: ../images/data_stores_type5.png
     - serveur vectoriel (web feature server)
     

Éditer un entrepôt
------------------
Afin de voir et d'éditer un entrepôt, cliquez sur le nom de l'entrepôt. Le 
contenu exact de cette page dépendra du format choisi (voir la section sur 
:ref:`data` pour des informations sur les formats de données). Dans l'exemple 
plus bas nous avons le contenu de l'entrepôt ``nurc:ArcGridSample``.

.. figure:: ../images/data_stores_edit.png
   :align: center
   
   *Éditer un entrepôt de données raster*

Bien que les paramètres de connexion vont varier en fonction du format de données, 
certaines informations basiques sont communes à plusieurs formats. La liste 
déroulante des Espaces de noms liste tous les espace de noms enregistrés. On 
assigne l'entrepôt à l'espace de nom sélectionné (``nurc``). :guilabel:`Nom de 
la source de données` est le nom de l'entrepôt qui sera listé dans la page de 
visualisation. :guilabel:`Description`est optionnel et s'affiche seulement dans 
l'interface d'administration. :guilabel:`Activé` permet d'activer ou de désactiver 
l'entrepôt, avec toutes les données qui y est définie.

Ajouter un entrepôt
--------------------
Les boutons ajouter et supprimer un espace de nom peuvent être trouvé en haut de 
la page Entrepôts.

.. figure:: ../images/data_stores_add_remove.png
   :align: center
   
   *Boutons pour ajouter et supprimer des entrepôts*

Pour ajouter un espace de nom, sélectionnez le bouton :guilabel:`Ajouter un 
nouvel entrepôt`. Une source de données vous sera demandée. GeoServer gère 
nativement plusieurs formats (et plus encore via les extensions). Cliquez sur la 
source de données appropriée pour continuer.

.. figure:: ../images/data_stores_chooser.png
   :align: center
   
   *Choisir une source de donnézes pour une nouvel entrepôt*

La page suivante configurera les entrepôts (l'exemple ci-dessous montre la page 
de configuration du raster ArcGrid). Cependant puisque les paramètres de connexion 
diffèrent d'une source de données à l'autre, le contenu exact de cette page 
dépendra du format spécifique de l'entrepôt. Lisez la section :re:f`data` sur les 
formats de données spécifiques.

.. figure:: ../images/data_stores_add.png
   :align: center
   
   *Page de configuration pour une source de données raster ArcGrid*

Supprimer un entrepôt
---------------------

Dans le but de supprimer un entrepôt, cliquez sur lacase à cocher correspondant 
à l'entrepôt. Plusieurs entretpôts peuvent être sélectionné pour des suppressions 
multiples.

.. figure:: ../images/data_stores_delete.png
   :align: left
   
   *Entrepôt sélectionné pour suppresion*

cliquez sur le bouton :guilabel:`Supprimer les entrepôts sélectionnés`. Vous 
devrez confirmer la suppression des données dans chaque entrepôt. Sélectionner
:guilabel:`OK` supprimer les entrepôts et vous redirige vers la page des entrepôts 
principales.

.. figure:: ../images/data_stores_delete_confirm.png
   :align: left
   
   *Confirmer la suppression des entrepôts*

.. yjacolin at free.fr 2011/11/18 r13133
