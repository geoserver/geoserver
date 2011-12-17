.. _sld_intro:

Introduction à SLD
===================

Les données géospatiales n'ont pas de composante visuelle intrinsèque. Pour voir une donnée, elle doit être symbolisée. Cela signifie préciser couleur, épaisseur et autres attributs visibles. Dans GeoServer, cette symbolisation est effectuée en utilisant un langage balisé appelé `Styled Layer Descriptor <http://www.opengeospatial.org/standards/sld>`_, ou SLD pour faire court. SLD est un langage balisé basé sur XML et est très puissant, mais il peut être intimidant. Cette page fournit des instructions élémentaires sur ce que l'on peut faire avec SLD et comment GeoServer le prend en charge.

.. note:: Comme GeoServer utilise exclusivement SLD pour la symbolisation (ou "stylage", NdT), les termes "SLD" et "styles" seront utilisés indifféremment.

Types de symbolisation
----------------------

Les données que GeoServer peut servir sont réparties en trois classes de forme:  **Points, lignes et polygones**. Les lignes (des formes unidimensionnelles) sont les plus simples, car elles n'ont que des arêtes (également appelé "stroke" pour "trait") à symboliser. Les polygones, des formes bidimensionnelles, ont des arêtes et un intérieur (également appelé "fill" pour "remplissage"). Les points, même s'il n'ont pas de dimension, ont également des arêtes et un intérieur (et une taille) qui peuvent être symbolisés. Pour les remplissages, on peut spécifier une couleur; pour les traits, couleur et épaisseur peuvent être spécifiés.

Il est possible de fabriquer des symbolisations plus élaborées que couleur et trait. Les points peuvent être représentés avec des formes connues telles que cercles, carrés, étoiles, et même des graphismes personnalisés ou du texte. Les lignes peuvent être symbolisées avec des pointillés ou des hachures. Les polygones peuvent être remplis avec des motifs graphiques personnalisés. Les styles peuvent être basés sur des attributs dans les données, pour que certains objets soient représentés différemment. Les labels textuels sur les objets sont également possibles. Les objets peuvent être symbolisés différemment selon le niveau de zoom, avec la taille de l'objet déterminant la façon dont il est représenté. Les possibilités sont vastes.

Métadonnées de style
--------------------

GeoServer et SLD
-----------------

Toute couche (featuretype) enregistrée dans GeoServer a besoin d'avoir au moins un style associé. GeoServer est livré avec quelques styles simples, et il est possible d'y adjoindre autant de styles que souhaité. Il est possible de modifier à tout moment le style associé à une couche dans la page :ref:`webadmin_layers` de :ref:`web_admin`. Lorsqu'on souhaite ajouter simultanément une couche et un style à GeoServer, le style devrait être ajouté en premier, pour que la nouvelle couche puisse être associée immédiatement au style. Vous pouvez ajouter un style dans le menu :ref:`webadmin_styles` de :ref:`web_admin`.

Définitions
-----------

Symbolizer
``````````

Rule
````

FeatureTypeStyle
````````````````


Un style simple
---------------

Ce SLD prend une couche contenant des points, et les symbolise sous forme de cercles rouges avec une taille de 6 pixels.  (C'est le premier exemple dans la section :ref:`sld_cookbook_points` de :ref:`sld_cookbook`.)

.. code-block:: xml 
   :linenos: 

   <?xml version="1.0" encoding="ISO-8859-1"?>
   <StyledLayerDescriptor version="1.0.0" 
       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
       xmlns="http://www.opengis.net/sld" 
       xmlns:ogc="http://www.opengis.net/ogc" 
       xmlns:xlink="http://www.w3.org/1999/xlink" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     <NamedLayer>
       <Name>Simple point</Name>
       <UserStyle>
         <Title>GeoServer SLD Cook Book: Simple point</Title>
         <FeatureTypeStyle>
           <Rule>
             <PointSymbolizer>
               <Graphic>
                 <Mark>
                   <WellKnownName>circle</WellKnownName>
                   <Fill>
                     <CssParameter name="fill">#FF0000</CssParameter>
                   </Fill>
                 </Mark>
                 <Size>6</Size>
               </Graphic>
             </PointSymbolizer>
           </Rule>
         </FeatureTypeStyle>
       </UserStyle>
     </NamedLayer>
   </StyledLayerDescriptor>

   
La longueur de cet exemple simple ne doit pas vous intimider; seules quelques lignes sont réellement utiles à la compréhension. La **ligne 14** dit que nous utilisons un symboliseur ponctuel ("PointSymbolizer"), un style pour les données ponctuelles. La **ligne 17** signifie que nous allons utiliser un "well known name" (un nom connu), un cercle, pour représenter les points.  Il y a beaucoup de noms connus pour les formes, comme "square" (carré), "star" (étoile), "triangle", etc.  Les **lignes 18-20** signifient que l'on remplit la forme avec une couleur valant  ``#FF0000`` (rouge). C'est un code couleur RGB, écrit en hexadécimal, sous la forme #RRVVBB. Finalement, la **ligne 22** signifie que la taille de la forme est de 6 pixels en largeur. Le reste de la structure contient des métadonnées à propos du style, comme Name/Title/Abstract (nom/titre/résumé).

On trouve bien d'autres exemples dans le :ref:`sld_cookbook`.

.. note:: Vous vous apercevrez que certaines balisent portent un préfixe, comme ``ogc:``. Ces préfixes matérialisent des **espaces de nom XML**.  Dans les balises des lignes **lines 2-7**, il y a deux espaces de nom XML, l'un appelé ``xmlns``, et l'autre appelé ``xmlns:ogc``.  Les balises correspondant au premier espace de nom ne requièrent pas de préfixe, mais celles correspondant au deuxième requièrent le préfixe  ``ogc:``.  Il faut mentionner que le nom des espaces de nom n'est pas important: le premier espace de nom pourrait être ``xmlns:sld`` (comme c'est l'usage) et ainsi toutes les balises de cet exemple devraient avoir pour préfixe ``sld:``.  L'important est qu'à l'espace de nom corresponde bien les balises associées.

Dépannage
---------

SLD est une forme de langage de programmation, peu différent de la création d'une page web ou de la rédaction d'un script. En conséquence, vous pouvez rencontrer des problèmes qui auront besoin d'un dépannage. Lorsqu'un style est ajouté à GeoServer, il est automatiquement soumis à validation selon la spécification OGC SLD (cette vérification peut être court-circuite), mais les erreurs ne seront pas traquées. Il est très facile d'avoir des erreurs de syntaxe dissimulées dans un SLD valide. La plupart du temps, cela occasionnera une carte ne présentant aucun objet (une carte blanche), mais quelquefois des erreurs iront jusqu'à bloquer le chargement de la carte.

La façon la plus facile de corriger les erreurs dans un SLD est de les isoler. Si le SLD est long et comprend de nombreux filtres et règles différents, essayez de supprimer temporairement certains d'entre eux pour voir si les erreurs disparaîssent.

Pour réduire les erreurs lors de la création d'un SLD, il est recommandé d'utiliser un éditeur de texte prévu pour travailler avec XML. Les éditeurs adaptés à XML savent rendre la recherche et la suppression d'erreur plus facile en fournissant de la coloration syntaxique et (parfois) une détection d'erreur intégrée.

.. fabrice at phung.fr 2011/09/20 r16266
