.. _webadmin_workspaces:

Espace de travail
==================

Cette section permet de visualiser et configurer les espaces de travail. Analogue 
à un espace de nom, un espace de travail est un conteneur qui est utilisé pour 
organiser des objets. Dans GeoServer, un espace de travail est souvent utilisé 
pourgrouper des couches similaires ensembles. On se référe souvent à des couches 
individuelles par leur nom d'espace de travail, deux points, puis leur entrepôt 
(ex. topp:states). Deux couches différentes peuvent avoir le même nom du moment 
qu'ells sont dans deux espaces de travail différents (par exemple sf:states, 
topp:states).

.. figure:: ../images/data_workspaces.png
   :align: center
   
   *Page des espaces de travail*

Éditer un espace de travail
----------------------------

Afin de voir les détails et d'éditer un espace de travail, cliquez sur son nom.

.. figure:: ../images/data_workspaces_URI.png
   :align: center
   
   *Espace nommé "topp"*


Un esapcede travail consiste d'un nom et d'une URI d'espace de nom (Uniform 
Resource Identifier). Le nom de l'espace de travail a au maximum 10 caractères et 
ne peut pas contenir d'espace. Une URI est similaire à une URL, sauf qu'une URI 
ne nécessite pas de pointer vers un endroit sur le web, et doit seulement être un 
indentifiant unique. Pour une URI d'espace de travail, nous recommandons d'utiliser 
une URL associé à votre projet, avec éventuellement un nom final d'identification, 
comme ``http://www.openplans.org/topp`` pour l'espace de travail "topp".  
   
Ajouter ou supprimer un espace de travail
------------------------------------------
Les boutons pour ajouter et supprimer un espace de travail peut être trouvé en 
haut de la page de visualisation des espaces.

.. figure:: ../images/data_workspaces_add_remove.png
   :align: center
   
   *Boutons pour ajouter et supprimer des espaces de travail*

Pour ajouter un espace de travail, sélectionnez le bouton :guilabel:`Ajouter un 
nouvel espace de travail`. On vous demandera d'entrer le nom de l'espace et son 
URI.
   
.. figure:: ../images/data_workspaces_medford.png
   :align: center
   
   *Nouvel espace de travail avec un exemple*

Pour supprimer un espace de travail, cliquez sur la case à cocher correspondante 
de l'espace de travail. Comme pour le processus de suppression de couche, plusieurs 
espaces de travail peuvent être sélectionnés pour suppression dans une seule page 
de résultats. Cliquez sur le bouton :guilabel:`Supprimer les espaces de travail 
sélectionnés`. Vous devrez confirmer ou annuler la suppression. Cliquer sur 
:guilabel:`OK` supprimera l'espace de travail.

.. figure:: ../images/data_workspaces_rename_confirm.png
   :align: center
   
   *Confirmation de la suppression de l'espace de travail*

.. yjacolin at free.fr 2011/11/18 r13133
