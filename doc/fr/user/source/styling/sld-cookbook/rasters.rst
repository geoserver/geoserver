.. _sld_cookbook_rasters:

Rasters
=======

Les rasters sont des données géographiques disposées dans une grille. Elles sont similaires à des images comme les fichiers PNG, mais au lieu que chaque point contienne des informations visuelles, il contient des informations géographiques sous forme numérique. Les rasters peuvent être vus comme une matrice géoréférencée de valeurs numériques.

Un exemple de raster est une couche modèle numérique d'élévation (DEM), qui possède des données d'élévation codées sous forme d'un à chaque point géoréférencé.

.. warning:: Pour rester concis, les exemples de code présentés sur cette page ne sont **pas le code SLD complet** car ils ommettent les informations SLD de début et de fin.  Utilisez les liens pour télécharger les SLD complet de chaque exemple.


Exemple de raster
-----------------

La :download:`couche raster <artifacts/sld_cookbook_raster.zip>` utilisée dans les exemples ci-dessous contient les données d'élévation d'un monde fictif. Les données sont stockées en projection EPSG:4326 (longitude/latitude) et des extremums de 70 à 256.  Si elles sont restituées en niveaux de gris, où les valeurs minimum sont colorées en noir et les valeurs maximum en blanc, le raster ressemblerait à ceci:

.. figure:: images/raster.png
   :align: center

   *Fichier raster restitué en niveaux de gris*

:download:`Téléchargez le raster et le shapefile <artifacts/sld_cookbook_raster.zip>`

.. _sld_cookbook_raster_twocolorgradient:


Gradient bicolore
-----------------

Cet exemple montre un style bicolore avec du vert à basse altitude et du brun à haute altitude.

.. figure:: images/raster_twocolorgradient.png
   :align: center

   *Gradient bicolore*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Gradient bicolore" <artifacts/raster_twocolorgradient.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap>
              <ColorMapEntry color="#008000" quantity="70" />
              <ColorMapEntry color="#663333" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Dans cet exemple, il y a un ``<Rule>`` dans un ``<FeatureTypeStyle>`` ce qui est la situation la plus simple possible.  Tous les exemples suivants partageront cette caractéristique.  La symbolisation des rasters est effectuée avec la balise ``<RasterSymbolizer>`` (**lignes 3-8**).

Cet exemple crée un gradient régulier entre deux couleurs correspondant à deux valeurs d'élévation. Le gradient est créé avec ``<ColorMap>`` en **lignes 4-7**. Chaque entrée dans ``<ColorMap>`` représente une entrée, ou "ancre", dans le gradient.  La **ligne 5** règle la valeur basse égale à 70 via le paramètre ``quantity``, valeur représentée en vert foncé (``#008000``). La **ligne 6** règle la valeur haute de 256, à nouveau via le paramètre ``quantity``, et la valeur est représentée en brun foncé (``#663333``).  Toutes les valeurs entre ces deux quantités sont interpolées linéairement: une valeur de 163 (le point médian entre 70 et 256) sera colorée avec une teinte médiane entre les deux couleurs (dans ce cas, environ ``#335717``, un brun-vert).

Gradient transparent
--------------------

Cet exemple crée le même gradient bicolore que :ref:`sld_cookbook_raster_twocolorgradient` de l'exemple précédent, mais rend la couche entière presque transparente en réglant une opacité à 30%.

.. figure:: images/raster_transparentgradient.png
   :align: center

   *Gradient transparent*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Transparent gradient" <artifacts/raster_transparentgradient.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <Opacity>0.3</Opacity>
            <ColorMap>
              <ColorMapEntry color="#008000" quantity="70" />
              <ColorMapEntry color="#663333" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Cet exemple est similaire à l'exemple :ref:`sld_cookbook_raster_twocolorgradient` mis à part l'ajout de la **ligne 4**, laquelle règle l'opacité de la couche à 0.3 (ou opaque à 30%). Une valeur d'opacité de 1 signifie que la forme est dessinée 100% opaque, alors qu'une valeur de 0 signifie qu'elle est affichée entièrement transparente. Une valeur de 0.3 signifie que le lraster prend partiellement la couleur et le style de ce qui est dessiné en-dessous. Comme le fond est blanc dans cet exemple, les coucleurs générées par ``<ColorMap>`` paraîssent plus claires, mais si le raster est superposé à un fond sombre la couleur résultante sera plus sombre.


Luminosité et contraste
-----------------------

Cet exemple normalise les couleurs de sortie, puis augmentent la luminosité d'un facteur 2.

.. figure:: images/raster_brightnessandcontrast.png
   :align: center

   *Luminosité et contraste*
 
Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Luminosité et contraste" <artifacts/raster_brightnessandcontrast.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ContrastEnhancement>
              <Normalize />
              <GammaValue>0.5</GammaValue>
            </ContrastEnhancement>
            <ColorMap>
              <ColorMapEntry color="#008000" quantity="70" />
              <ColorMapEntry color="#663333" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Cet exemple est similaire à :ref:`sld_cookbook_raster_twocolorgradient`, mis à part l'ajout de la balise ``<ContrastEnhancement>`` en **lignes 4-7**. La **ligne 5** normalise la sortie en augmentant le contraste à son extension maximum. La **ligne 6** ajuste la luminosité d'un facteur 0.5. Comme les valeurs inférieures à 1 rendent le résultat plus clair, une valeur de 0.5 rend le résultat deux fois plus clair.

Comme dans les exemples précédents, les **linges 8-11** determinent la ``<ColorMap>``, avec la **ligne 9** paramétrant la limite inférieure (70) à une couleur vert foncé (``#008000``) et la **ligne 10** paramétrant la limite supérieure (256) à une couleur marron foncé (``#663333``). 



Gradient à trois couleurs
-------------------------

Cet exemple crée un gradient à trois couleurs prises dans les couleurs primaires. De plus, le gradient ne prend pas en compte l'étendue complète des valeurs des données, ce qui aboutit à ne pas prendre en compte certaines données.

.. figure:: images/raster_threecolorgradient.png
   :align: center

   *Gradient à trois couleurs*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Three-color gradient" <artifacts/raster_threecolorgradient.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap>
              <ColorMapEntry color="#0000FF" quantity="150" />
              <ColorMapEntry color="#FFFF00" quantity="200" />
              <ColorMapEntry color="#FF0000" quantity="250" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Cet exemple crée un gradient à trois couleurs basé sur une ``<ColorMap>`` avec trois entrées en **lignes 4-8**: la **ligne 5** paramétrant la limite inférieure (150) à bleu (``#0000FF``), la **ligne 6** paramétrant une valeur intermédiaire (200) à jaune (``#FFFF00``), et la **ligne 7** paramétrant la limite supérieure (250) à rouge (``#FF0000``).

Comme nos valeurs de données vont de 70 à 256, certains points ne sont pas pris en compte par ce style. Les valeurs inférieures à la limite basse de la carte de couleurs (l'intervalle 70 à 149) sont rendues avec la même couleurs que la limite basse, à savoir bleu. D'autre part, les valeurs supérieures à la limite haute de la carte de couleur (l'intervalle 251 à 256) ne sont pas affichées du tout.


Canal alpha
-----------

Cet exemple crée un effet de "canal alpha" de sorte que les valeurs supérieures sont graduellement transparentes.

.. figure:: images/raster_alphachannel.png
   :align: center

   *Canal alpha*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Alpha channel" <artifacts/raster_alphachannel.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap>
              <ColorMapEntry color="#008000" quantity="70" />
              <ColorMapEntry color="#008000" quantity="256" opacity="0"/>
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Un canal alpha est une autre façon d'aboutir à une transparence variable. Tout comme les gradients lient les valeurs aux couleurs, chaque entrée de ``<ColorMap>`` peut avoir des valeurs d'opacité (la valeur par défaut étant 1.0 soit complètement opaque).

Dans cet exemple, il y a une ``<ColorMap>`` avec deux entrées: la **ligne 5** paramètre la limite inférieure de 70 à vert foncé (``#008000``), et la **ligne 6** spécifie la limite supérieure de 256 à vert foncé également, mais avec une valeur d'opacité de 0. Cela signifie que les valeurs à 256 seront affichées avec une opacité de 0% (entièrement transparent). Tout comme le gradient de couleurs, l'opacité est aussi interpolée linéairement, de sorte q'une valeur de 163 (le point médian entre 70 et 256) sera affichée avec une opacité de 50%.


Couleurs discrètes
------------------

Cet exemple présente un gradient qui n'est pas interpolé linairement, mais qui à la place fait correspondre précisément des valeurs à trois couleurs.

.. note:: Cet exemple met en exergue une extension SLD propre à GeoServer. Les couleurs discrètes ne font pas partie de la spécification SLD.

.. figure:: images/raster_discretecolors.png
   :align: center

   *Couleurs discrètes*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Couleurs discrètes" <artifacts/raster_discretecolors.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap type="intervals">
              <ColorMapEntry color="#008000" quantity="150" />
              <ColorMapEntry color="#663333" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Parfois, des bandes de couleur distribuées par paliers discrets sont plus appropriées que les gradients de couleur. Le paramètre ``type="intervals"`` ajouté à ``<ColorMap>`` en **ligne 4** indique l'emploi de couleurs discrètes à la place d'un gradient. Les valeurs dans chaque entrée correspondent à la limite supérieure pour la bande de couleur, de sorte que les couleurs sont reliées aux valeurs inférieures à la valeur d'une entrée, et supérieures ou égales à l'entrée suivante. Par exemple, la **ligne 5** colorie toutes les valeurs inférieures à 150 en vert foncé (``#008000``) et la **ligne 6** colorie toutes les valeurs inférieures à 256 et supérieures ou égales à 150 en brun foncé (``#663333``).


Gradient multicolore
--------------------

Cet exemple présente un gradient à huit couleurs.

.. figure:: images/raster_manycolorgradient.png
   :align: center

   *Gradient multicolore*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Gradient multicolore" <artifacts/raster_manycolorgradient.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap>
              <ColorMapEntry color="#000000" quantity="95" />
              <ColorMapEntry color="#0000FF" quantity="110" />
              <ColorMapEntry color="#00FF00" quantity="135" />
              <ColorMapEntry color="#FF0000" quantity="160" />
              <ColorMapEntry color="#FF00FF" quantity="185" />
              <ColorMapEntry color="#FFFF00" quantity="210" />
              <ColorMapEntry color="#00FFFF" quantity="235" />
              <ColorMapEntry color="#FFFFFF" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Il n'y a pas de limite au nombre d'entrées contenues dans  ``<ColorMap>`` (**lignes 4-13**).  Cet exemple a huit entrées:

.. list-table::
   :widths: 15 25 30 30 

   * - **Entry number**
     - **Value**
     - **Color**
     - **RGB code**
   * - 1
     - 95
     - Black
     - ``#000000``
   * - 2
     - 110
     - Blue
     - ``#0000FF``
   * - 3
     - 135
     - Green
     - ``#00FF00``
   * - 4
     - 160
     - Red
     - ``#FF0000``
   * - 5
     - 185
     - Purple
     - ``#FF00FF``
   * - 6
     - 210
     - Yellow
     - ``#FFFF00``
   * - 7
     - 235
     - Cyan
     - ``#00FFFF``
   * - 8
     - 256
     - White
     - ``#FFFFFF``

.. fabrice at phung.fr 2011/09/20 r16266