.. _webadmin_basics:

Interface basique
===================

Cette secttion introduira les concepts basiques de l'interface d'administration 
web (souvent abbrégé en "admin web" dans le texte).


Page de bienvenue
------------------

Lorsque vous utilisez une installation normale, GeoServer démarrera un serveur 
web sur localhost accessible sur le port 8080 avec l'URL suivante :
::

   http://localhost:8080/geoserver/web

.. note:: Cette URL est dépendente de votre installation de GeoServer. Lors de 
   l'utilisation de l'installation WAR, par exemple, l'URL dépendra de la 
   configuration du conteneur.

Lorsque tout est correctement configuré, une page de bienvenu s'affiche dans 
votre navigateur.

.. figure:: images/web-admin.png
   :align: center
   
   *Page de bienvenue*
  
La page de bienvenue contient des liens vers différentes zones de configuration 
de GeoServer. La section :guilabel:`À propos de GeoServer` dans le menu 
:guilabel:`Server` fournie des liens externes vers la documentation de GeoServer, 
la home page et la liste des bugs. La page fourni également un accès pour 
s'identifier à la console de GeoServer. Cette mesure de sécurité empeche les 
personnes non identifiées de modifier votre configuration de GeoServer. 
L'identification par défaut est ``admin`` et ``geoserver``.  Cela doit être changé 
en éditant le fichier :file:`security/users.properties` dansz le répertoire 
:ref:`data_directory`.  

.. figure:: images/8080login.png
   :align: center
   
   *Login*

Sans être authentifié, le menu d'admin web amène vers la :guilabel:`Démo` et la 
:guilabel:`Prévisualisation des couches` de la console. La page :ref:`webadmin_demos` 
contient des liens utiles vers différentes pages d'informations, tandis que la 
page :ref:`layerpreview` fournie desz données spatiales dans différents formats 
de sortie.

Lorsque vous êtes identifié, des options supplémentaires sont présentes.

.. figure:: images/welcome_logged_in.png
   :align: center
   
   *Options supplémentaires lors de l'indentification*

Les spécifications des configurations des Web Coverage Service (WCS), Web Feature 
Service (WFS), et Web Map Service (WMS) de Geoserver peuvent être accéder à partir 
de la page de bienvenu également. Voir la section sur :ref:`services` pour plus 
d'informations.

.. yjacolin at free.fr 2011/07/07 r16069
