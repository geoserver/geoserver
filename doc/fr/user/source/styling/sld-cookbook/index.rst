.. _sld_cookbook:

Livre de recettes SLD
=====================

Le livre de recettes SLD est une collection de "recettes SLD" pour créer une variété de styles cartographiques. Tant que possible, chaque exemple est conçu pour montrer une seule fonctionnalité de SLD pour que le code puisse être copié de l'exemple et adapté lorsque vous créez vos propres SLD. Quoique ne représentant pas une référence exhaustive comme le :ref:`sld_reference` ou la `spécification OGC SLD 1.0 <http://www.opengeospatial.org/standards/sld>`_ le livre recettes SLD est conçu pour devenir une référence pratique, recensant des modèles de styles simples à comprendre..

Le livre de recettes SLD est divisé en quatre sections: les trois premières pour chacun des types vecteur (points, lignes, polygones) et la quatrième pour les rasters. Dans toute section, chaque exemple présente une capture d'écran montrant la sortie WMS de GeoServer, un extrait du code SLD pour référence, et un lien pour télécharger le SLD complet.

Chaque section utilise des données fabriquées spécialement pour le livre de recettes SLD, avec des shapefiles pour les données vecteur et des GeoTIFFs pour les données raster.  La projection des données est EPSG:4326. Tous les fichiers peuvent être aisément chargés dans GeoServer pour faire fonctionner les exemples.

.. list-table::
   :widths: 20 80

   * - **Data Type**
     - **Shapefile**
   * - Point
     - :download:`sld_cookbook_point.zip <artifacts/sld_cookbook_point.zip>`
   * - Ligne
     - :download:`sld_cookbook_line.zip <artifacts/sld_cookbook_line.zip>`
   * - Polygone
     - :download:`sld_cookbook_polygon.zip <artifacts/sld_cookbook_polygon.zip>`
   * - Raster
     - :download:`sld_cookbook_raster.zip <artifacts/sld_cookbook_raster.zip>`

.. toctree::
   :maxdepth: 2

   points
   lignes
   polygones
   rasters


.. fabrice at phung.fr 2011/09/20 r16266