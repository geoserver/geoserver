.. _sld_cookbook_points:

Points
======

Si les points paraîssent être le type de forme le plus simple, ne comprenant qu'une positino et pas d'autres dimensions, il existe de nombreuses façons différentes de symboliser un point en SLD.

.. warning:: Pour rester concis, les exemples de code présentés sur cette page ne sont **pas le code SLD complet** car ils ommettent les informations SLD de début et de fin.  Utilisez les liens pour télécharger les SLD complet de chaque exemple.

.. _sld_cookbook_points_attributes:

Exemple de couche ponctuelle
----------------------------

La :download:`couche ponctuelle <artifacts/sld_cookbook_point.zip>` utilisée pour les exemples ci-dessous contient nom et population des villes principales d'un pays fictif. Pour mémoire, la table d'attributs des points de cette couche est présentée ci-dessous.

.. list-table::
   :widths: 30 40 30

   * - **fid** (Feature ID)
     - **name** (City name)
     - **pop** (Population)
   * - point.1
     - Borfin
     - 157860
   * - point.2
     - Supox City
     - 578231
   * - point.3
     - Ruckis
     - 98159
   * - point.4
     - Thisland
     - 34879
   * - point.5
     - Synopolis
     - 24567
   * - point.6
     - San Glissando
     - 76024
   * - point.7
     - Detrania
     - 205609

:download:`Téléchargez le shapefile des points <artifacts/sld_cookbook_point.zip>`

.. _sld_cookbook_points_simplepoint:

Point simple
------------

Cet exemple symbolise les points sous la forme de cercles rouges de diamètre 6 pixels.

.. figure:: images/point_simplepoint.png
   :align: center

   *Simple point*
   
Code
~~~~

:download:`Voir et télécharger le SLD "Simple point" complet <artifacts/point_simplepoint.sld>`

.. code-block:: xml 
   :linenos: 

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



Details
~~~~~~~

Il y a un ``<Rule>`` dans un ``<FeatureTypeStyle>`` pour ce SLD, ce qui est la situation la plus simple possible.  (Les exemples suivants contiendront un ``<Rule>`` et un ``<FeatureTypeStyle>`` sauf précision.)  La symbolisation des points est effectuée par le ``<PointSymbolizer>`` (**lines 3-13**).  La **ligne 6** dit que la forme du symbole doit être un cercle, avec la **ligne 8** fixant la couleur de remplissage à rouge (``#FF0000``).  La **ligne 11** fixe la taille (diamètre) du graphisme à 6 pixels.


.. _sld_cookbook_points_simplepointwithstroke:

Exemple simple avec bord
------------------------

Cet exemple ajoute un trait (ou bord) autour du :`sld_cookbook_points_simplepoint`, avec le trait coloré en noir et muni d'une épaisseur de 2 pixels.

.. figure:: images/point_simplepointwithstroke.png
   :align: center

   *Point simple avec bord*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Point simple avec bord" <artifacts/point_simplepointwithstroke.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#FF0000</CssParameter>
                </Fill>
                <Stroke>
                  <CssParameter name="stroke">#000000</CssParameter>
                  <CssParameter name="stroke-width">2</CssParameter>
                </Stroke>
              </Mark>
              <Size>6</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Cet exemple est similaire à l'exemple :ref:`sld_cookbook_points_simplepoint`.  Les **lines 10-13** spécifient le trait, avec la **ligne 11** réglant couleur à noir (``#000000``) et la **ligne 12** réglant l'épaisseur à 2 pixels.


Carré avec rotation
-------------------

Cet exemple crée un carré au lieu d'un cercle, le colore en vert, le dimensionne à 12 pixels et le fait tourner de 45 degrés.

.. figure:: images/point_rotatedsquare.png
   :align: center

   *Carré avec rotation*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Carré avec rotation" <artifacts/point_rotatedsquare.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>square</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#009900</CssParameter>
                </Fill>
              </Mark>
              <Size>12</Size>
              <Rotation>45</Rotation>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Détails
~~~~~~~

Dans cet exemple, la **ligne 6** demande pour forme un carré, avec la  **ligne 8** réglant la couleur à vert foncé (``#009900``).  La **ligne 11** règle la taille à  12 pixels et la **ligne 12** règle la rotation à 45 degrés.


Triangle transparent
--------------------

Cet exemple dessine un triangle, crée un trait noir identique à l'exemple :ref:`sld_cookbook_points_simplepointwithstroke` , et règle le remplissage du triangle à 20% d'opacité (presque transparent).

.. figure:: images/point_transparenttriangle.png
   :align: center

   *Triangle transparent*

Code
~~~~   

:download:`Consultez et téléchargez le SLD complet "Triangle transparent" SLD <artifacts/point_transparenttriangle.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>triangle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#009900</CssParameter>
                  <CssParameter name="fill-opacity">0.2</CssParameter>
                </Fill>
                <Stroke>
                  <CssParameter name="stroke">#000000</CssParameter>
                  <CssParameter name="stroke-width">2</CssParameter>
                </Stroke>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Détails
~~~~~~~

Dans cet exemple, la **ligne 6** règle la forme, un triangle dans ce cas. La **ligne 8** règle la couleur de remplissage à vert foncé (``#009900``) et la **ligne 9** règle l'opacité à  0.2 (opaque à 20%).  Une valeur d'opacité de 1 signifie que la forme est dessinée avec une opacité de 100%, alors qu'une valeur d'opacité de 0 signifie que la forme est dessinée avec une opacité de 0%, soit complètement transparente. La valeur de 0.2 (20% d'opacité) signifie que le remplissage des points prend partiellement la couleur et le style de ce qui est dessiné en-dessous. Dans cet exemple, comme le fond est blanc, le vert foncé paraît plus clair. Si les points étaient posés sur fond noir, la couleur résultante serait plus foncée. Les **lignes 12-13** règlent la couleur de trait à noir (``#000000``) et la largeur à 2 pixels. Finalement, la **ligne 16** règle la taille du point à un diamètre de 12 pixels.

Point graphique
---------------

Cet exemple symbolise chaque point avec un graphisme au lieu d'une forme simple.

.. figure:: images/point_pointasgraphic.png
   :align: center

   *Point image*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Point image" <artifacts/point_pointasgraphic.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource
                  xlink:type="simple"
                  xlink:href="smileyface.png" />
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>32</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
	  


Détails
~~~~~~~

Ce style utilise une image au lieu d'une forme simple pour représenter les points. Dans le SLD, ceci est connu comme un ``<ExternalGraphic>``, pour le distinguer des formes courantes comme carrés et cercles qui sont "internes" au moteur de rendu. Les **lignes 5-10** spécifient les détails de cette image. La **ligne 8** paramètre le chemin et le nom de fichier de l'image, alors que la  **ligne 9** indique le format (type MIME) de l'image (image/png). Dans cet exemple, l'image est contenue dans le même répertoire que le SLD, aucune information de chemin n'est donc nécessaire en **ligne 8**, mais une URL complète pourrait être utilisée si souhaité. La **ligne 11** détermine la taille d'affichage de l'image; ceci peut être réglé indépendamment de la dimension propre de l'image, même si dans ce cas elles sont identiques (32 pixels). Si l'image était rectangulaire, la valeur ``<Size>`` s'appliquerait à la *hauteur* seule de l'image, avec la largeur réévaluée proportionnellement.

.. figure:: images/smileyface.png
   :align: center

   *Graphisme utilisé pour un point*

.. _sld_cookbook_points_pointwithdefaultlabel:

Point avec étiquette par défaut
-------------------------------

Cet exemple présente une étiquette textuelle sur le :ref:`sld_cookbook_points_simplepoint` affichant l'attibut "name" du point. Une étiquette sera représentée de cette façon en l'absence de personnalisation.

.. figure:: images/point_pointwithdefaultlabel.png
   :align: center

   *Point avec étiquette par défaut*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Point with default label" <artifacts/point_pointwithdefaultlabel.sld>`

.. code-block:: xml 
   :linenos:

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
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Détails
~~~~~~~

Les **lignes 3-13**, qui contiennent le  ``<PointSymbolizer>``, sont identiques à l'exemple :ref:`sld_cookbook_points_simplepoint` ci-dessus. L'étiquette est paramétrée dans le ``<TextSymbolizer>`` aux **lignes 14-27**.  Les **Lignes 15-17** déterminent le texte à afficher dans l'étiquette, dans ce cas la valeur de l'attibut "name". (Consultez la table des attributs dans la section :ref:`sld_cookbook_points_attributes` si nécessaire.) La **ligne 19** règle la couleur. Tous les autres paramètres concernant l'étiquette sont réglés aux valeurs par défaut du moteur de rendu, c'est à dire police Times New Roman, couleur noire, et taille de police 10 pixels. Le coin bas gauche de l'étiquette est aligné avec le centre du point.

.. _sld_cookbook_points_pointwithstyledlabel:

Point avec étiquette stylisée
-----------------------------

Cet exemple améliore le style de l'étiquette de l'exemple :ref:`sld_cookbook_points_pointwithdefaultlabel` en centrant l'étiquette au-dessus du point et en indiquant un nom de police et une taille différents.

.. figure:: images/point_pointwithstyledlabel.png
   :align: center

   *Point avec étiquette stylisée*

Code
~~~~   

:download:`Consultez et téléchargez le SLD complet "Point avec étiquette stylisée" <artifacts/point_pointwithstyledlabel.sld>`

.. code-block:: xml 
   :linenos:

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
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
              <CssParameter name="font-weight">bold</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.0</AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX>0</DisplacementX>
                  <DisplacementY>5</DisplacementY>
                </Displacement>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>


Détails
~~~~~~~

Dans cet exemple, les **lignes 3-13** sont identiques à l'exemple :ref:`sld_cookbook_points_simplepoint` ci-dessus.  Le ``<TextSymbolizer>`` au lignes 14-39 contient beaucoup d'autres détails concernant le style de l'étiquette que dans l'exemple précédent :ref:`sld_cookbook_points_pointwithdefaultlabel`.  Les **lignes 15-17** indique à nouveau l'emploi de l'attribut "name" pour afficher du texte. Les **lignes 18-23** règlent la police: la **ligne 19** paramètrent la famille de police à "Arial", la **ligne 20** paramètre la taille de font à 12, la **ligne 21** paramètre le style de police à "normal" (par opposition à "italic" ou "oblique"), et la **ligne 22** règle le poids de police à gras ou "bold" (par opposition à "normal"). Les **lignes 24-35** (``<LabelPlacement>``) déterminent le placement de l'étiquette par rapport au point.  ``<AnchorPoint>`` (**lines 26-29**) paramètre le point d'intersection entre l'étiquette et le point, avec ici (**ligne 27-28**) le point centré (0.5) horizontalement et verticalement aligné avec le bas (0.0) de l'étiquette.  Il y a aussi ``<Displacement>`` (**lignes 30-33**), qui règle le décalage de l'étiquette relativement à la ligne, dans ce cas 0 pixels horizontalement (**ligne 31**) et 5 pixels verticalement (**ligne 32**).  Finalement, la **ligne 37** règle la couleur de police de l'étiquette à noir (``#000000``).

Le résultat est une étiquette centrée et en gras placée légèrement au-dessus de chaque point.



Point avec rotation de l'étiquette
----------------------------------

Cet exemple est construit sur la base de l'exemple précédent :ref:`sld_cookbook_points_pointwithstyledlabel` en faisant tourner l'étiquette de 45 degrés, en positionnant les étiquettes plus loin des points, et en modifiant la couleur de l'étiquette à pourpre.

.. figure:: images/point_pointwithrotatedlabel.png
   :align: center

   *Point avec la rotation de l'étiquette*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Point avec la rotation de l'étiquette" <artifacts/point_pointwithrotatedlabel.sld>`

.. code-block:: xml 
   :linenos:

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
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
              <CssParameter name="font-weight">bold</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.0</AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX>0</DisplacementX>
                  <DisplacementY>25</DisplacementY>
                </Displacement>
                <Rotation>-45</Rotation>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill">#990099</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Détails
~~~~~~~

Cet exemple est similaire à :ref:`sld_cookbook_points_pointwithstyledlabel`, mais il y a trois différences importantes. La **ligne 32** spécifie un déplacement vertical de 25 pixels. La **ligne 34** spécifie une rotation de "-45", ou 45 degrés dans le sens inverse des aiguilles d'une montre. (Les valeurs de rotation vont en augmentant dans le sens des aiguilles d'une montre, c'est pourquoi cette valeur est négative.)  Finalement, la **ligne 38** paramètre la couleur de police à une nuance de pourpre (``#99099``).

Notez que, pendant le rendu, le déplacement prend effet avant la rotation, donc dans cet exemple le déplacement de 25 pixels en vertical subit lui-même une rotation de 45 degrés.


Points basés sur les attributs
------------------------------

Cet exemple fait varier la taille du symbole selon la valeur de l'attribut population ("pop").

.. figure:: images/point_attributebasedpoint.png
   :align: center

   *Points basés sur les attributs*
   
Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Points basés sur les attributs" <artifacts/point_attribute.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <Name>SmallPop</Name>
          <Title>1 to 50000</Title>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>pop</ogc:PropertyName>
              <ogc:Literal>50000</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0033CC</CssParameter>
                </Fill>
              </Mark>
              <Size>8</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>MediumPop</Name>
          <Title>50000 to 100000</Title>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>pop</ogc:PropertyName>
                <ogc:Literal>50000</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>pop</ogc:PropertyName>
                <ogc:Literal>100000</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0033CC</CssParameter>
                </Fill>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>LargePop</Name>
          <Title>Greater than 100000</Title>
          <ogc:Filter>
            <ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyName>pop</ogc:PropertyName>
              <ogc:Literal>100000</ogc:Literal>
            </ogc:PropertyIsGreaterThanOrEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0033CC</CssParameter>
                </Fill>
              </Mark>
              <Size>16</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Détails
~~~~~~~
   
.. note:: Se référer à  :ref:`sld_cookbook_points_attributes` pour voir les attributs des données. Cet exemple a mis de côté les étiquettes pour simplifier le style, mais vous pouvez vous référer à l'exemple :ref:`sld_cookbook_points_pointwithstyledlabel` pour voir à quels attributs correspondent quels points.

Le style contient trois règles. Chaque ``<Rule>`` fait varier le style de chaque point selon la valeur de l'attribut population ("pop"), de plus petites valeurs ayant pour effet des cercles plus petits, et les valeurs plus grandes un cercle plus grand.

Les trois règles sont conçues comme suit:

.. list-table::
   :widths: 20 30 30 20

   * - **Rule order**
     - **Rule name**
     - **Population** ("pop")
     - **Size**
   * - 1
     - SmallPop
     - Less than 50,000
     - 8
   * - 2
     - MediumPop
     - 50,000 to 100,000
     - 12
   * - 3
     - LargePop
     - Greater than 100,000
     - 16

L'ordre des règles n'a pas d'importance dans ce cas, car chaque forme est représentée par une seule règle.

La première règle, en **lignes 2-22**, spécifie la symbolisation des points dont l'attribut population est inférieur à 50 000. Les **lignes 5-10** règlent ce filtre, avec les **lignes 6-9** déterminant le filtre "inférieur à", la **ligne 7** précisant l'attribut ("pop"), et la **ligne 8** une valeur de 50 000.  Le symbole est un cercle (**ligne 14**), la couleur est bleu foncé (``#0033CC``, en **ligne 16**), et la taille est 8 pixels en diamètre (**ligne 19**).  

La deuxième règle, en **lignes 23-49**, spécifie une symbolisation pour les points dont l'attribut population est supérieur ou égal à 50 000 et inférieur à 100 000. Le filtre sur la population est réglé en **lignes 26-37**. Ce filtre est plus long que la première règle car deux critères au lieu d'un doivent être employés: un filtre "supérieur ou égal à" et un filtre "inférieur à".  Notez le ``And`` en **ligne 27** et **line 36**.  Ceci induit que les deux filtres doivent être vrais pour que la règle s'applique. La taille de la forme est réglée à 12 pixels en **line 46**.  Toutes les autres directives de style sont identiques à la première règle.

La troisième règle, en **lignes 50-70**, spécifie une symbolisation pour les points dont l'attribut population est supérieur ou égal à 100 000. Le filtre sur la population est réglé en **lignes 53-58**, et la seule autre différence est la taille du cercle, à 16 pixels (**line 67**) dans cette règle.

ce style a pour résultat que les cités avec une population plus grande ont des points plus grands.


Points basés sur le zoom
------------------------

Cet exemple modifie la symbolisation des points à des niveaux de zoom différents

.. figure:: images/point_zoombasedpointlarge.png
   :align: center

   *Zoom-based point: Zoomed in*

.. figure:: images/point_zoombasedpointmedium.png
   :align: center
   
   *Zoom-based point: Partially zoomed*

.. figure:: images/point_zoombasedpointsmall.png
   :align: center
   
   *Zoom-based point: Zoomed out*

   
Code
~~~~

:download:`Téléchargez et consultez le SLD "Points basés sur le zoom" <artifacts/point_zoom.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <Name>Large</Name>
          <MaxScaleDenominator>160000000</MaxScaleDenominator>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#CC3300</CssParameter>
                </Fill>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>Medium</Name>
          <MinScaleDenominator>160000000</MinScaleDenominator>
          <MaxScaleDenominator>320000000</MaxScaleDenominator>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#CC3300</CssParameter>
                </Fill>
              </Mark>
              <Size>8</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>Small</Name>
          <MinScaleDenominator>320000000</MinScaleDenominator>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#CC3300</CssParameter>
                </Fill>
              </Mark>
              <Size>4</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>




Détails
~~~~~~~

On souhaite souvent rendre les formes plus grandes avec les hauts niveaux de zooms pour créer une carte à l'apparence naturelle. Cet exemple symbolise les points de façon à faire varier leur taille selon le niveau de zoom (ou plus exactement, le dénominateur d'échelle). Les dénominateurs d'échelle correspondent à l'échelle de la carte. Un dénominateur d'échelle de 10 000 signifie que la carte a une échelle de 1/10 000e en unités associées à la projection de la carte.

.. note:: Déterminer des dénominateurs d'échelle (niveaux de zoom) pertinents va au-delà de cet exemple.

Le style contient trois règles. Les trois règles sont conçues comme suit:

.. list-table::
   :widths: 25 25 25 25 

   * - **Rule order**
     - **Rule name**
     - **Scale denominator**
     - **Point size**
   * - 1
     - Large
     - 1:160,000,000 or less
     - 12
   * - 2
     - Medium
     - 1:160,000,000 to 1:320,000,000
     - 8
   * - 3
     - Small
     - Greater than 1:320,000,000
     - 4

L'ordre de ces règles n'a pas d'importance car les échelles définies dans chaque règle ne se recoupent pas.

La première règle (**lignes 2-16**) correspond au plus petit dénominateur d'échelle, lorsque la vue est "zoomée".  Le seuil d'échelle est réglé en  **ligne 4**, pour que la règle s'applique à toute carte avec un dénominateur d'échelle de 160 000 000 ou moins.  La règle trace un cercle (**ligne 8**), coloré en rouge (``#CC3300`` en **lige 10**) avec une taille de 12 pixels (**ligne 13**).

La seconde règle (**lignes 17-32**) correspond aux échelles intermédiaires, lorsque la vue est "zoomée partiellement". Les seuils d'échelle sont réglés en **lignes 19-20**, pour que la règle s'applique à toute carte avec un dénominateur d'échelle compris entre 160 000 000 and 320 000 000.  (Le ``<MinScaleDenominator>`` est inclusif et le ``<MaxScaleDenominator>`` est exclusif pour qu'un niveau de zoom d'exactement320 000 000 ne s'applique *pas* ici.)  Mis à part l'échelle, la seule différence entre cette règle et la première est la taille du symbole, qui est à 8 pixels en **ligne 29**.

La troisièmre règle (**lignes 32-47**) est le dénominateur d'échelle le plus grand, correspondant à une carte avec "zoom arrière". Le seuil d'échelle est réglé en **line 35** pour que la règle s'applique à toute carte avec un dénominateur d'échelle supérieur ou égal à 320 000 000. A nouveau, la seule autre différence entre cette règle et les autres est la taille du symbole, qui est ici à 4 pixels en **ligne 44**.

Ce style a pour résultat des points représentés plus grands lorsque l'on zoome, et plus petits lorsque l'on effectue un zoom arrière.


.. fabrice at phung.fr 2011/09/20 r16266