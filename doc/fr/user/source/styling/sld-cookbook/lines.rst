.. _sld_cookbook_lines:

Lines
=====

Bien que les lignes n'ayant qu'une longueur sans épaisseur semblent être des formes simples, il existe beaucoup d'options et de trucs les représenter de façon satisfaisante.

.. warning:: Pour rester concis, les exemples de code présentés sur cette page ne sont **pas le code SLD complet** car ils ommettent les informations SLD de début et de fin.  Utilisez les liens pour télécharger les SLD complet de chaque exemple.

.. _sld_cookbook_lines_attributes:

Exemple de couche ligne
-----------------------

La :download:`couche ligne <artifacts/sld_cookbook_line.zip>` utilisée dans les exemples ci-dessous contient les données routières d'un pays fictif. Pour référence, le tableau des attributs des lignes de cette couche est incluse ci-dessous.

.. list-table::
   :widths: 30 40 30

   * - **fid** (Feature ID)
     - **name** (Road name)
     - **type** (Road class)
   * - line.1
     - Latway
     - highway
   * - line.2
     - Crescent Avenue
     - secondary
   * - line.3
     - Forest Avenue
     - secondary
   * - line.4
     - Longway
     - highway
   * - line.5
     - Saxer Avenue
     - secondary
   * - line.6
     - Ridge Avenue
     - secondary
   * - line.7
     - Holly Lane
     - local-road
   * - line.8
     - Mulberry Street
     - local-road
   * - line.9
     - Nathan Lane
     - local-road
   * - line.10
     - Central Street
     - local-road
   * - line.11
     - Lois Lane
     - local-road
   * - line.12
     - Rocky Road
     - local-road
   * - line.13
     - Fleet Street
     - local-road
   * - line.14
     - Diane Court
     - local-road
   * - line.15
     - Cedar Trail
     - local-road
   * - line.16
     - Victory Road
     - local-road
   * - line.17
     - Highland Road
     - local-road
   * - line.18
     - Easy Street
     - local-road
   * - line.19
     - Hill Street
     - local-road
   * - line.20
     - Country Road
     - local-road
   * - line.21
     - Main Street
     - local-road
   * - line.22
     - Jani Lane
     - local-road
   * - line.23
     - Shinbone Alley
     - local-road
   * - line.24
     - State Street
     - local-road
   * - line.25
     - River Road
     - local-road

:download:`Téléchargez le shapefile <artifacts/sld_cookbook_line.zip>`

.. _sld_cookbook_lines_simpleline:

Ligne simple
------------

Cet exemple précise que les lignes sont colorées en noir avec une épaisseur de 3 pixels.

.. figure:: images/line_simpleline.png
   :align: center

   *Ligne simple*

Code
~~~~

:download:`Consultez et télchargez le SLD complet "Ligne simple" <artifacts/line_simpleline.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#000000</CssParameter>
              <CssParameter name="stroke-width">3</CssParameter>    
            </Stroke>
          </LineSymbolizer>
       	</Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Dans ce SLD, il y a un ``<Rule>`` dans un ``<FeatureTypeStyle>`` , ce qui est la situation la plus simple possible. (Tous les exemples suivants contiendront un ``<Rule>`` et un ``<FeatureTypeStyle>`` , sauf mention contraire.)  Symboliser les lignes se fait avec ``<LineSymbolizer>`` (**lignes 3-8**).  La **ligne 5** règle la couleur des lignes à noir (``#000000``), et la **ligne 6** règle la largeur des lignes à 3 pixels.


Ligne avec bord
---------------

Cet exemple dessine des lignes avec un remplissage de 3 pixels et un trait gris de 1 pixel.

.. figure:: images/line_linewithborder.png
   :align: center

   *Ligne avec bord*

Code
~~~~

:download:`Consultez et télchargez le SLD complet "Ligne avec bord" <artifacts/line_linewithborder.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
         <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#333333</CssParameter>                           
              <CssParameter name="stroke-width">5</CssParameter>    
              <CssParameter name="stroke-linecap">round</CssParameter>    
            </Stroke> 
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
      <FeatureTypeStyle>
         <Rule>
          <LineSymbolizer>
          <Stroke>
              <CssParameter name="stroke">#6699FF</CssParameter>                           
              <CssParameter name="stroke-width">3</CssParameter> 
              <CssParameter name="stroke-linecap">round</CssParameter>  
            </Stroke>
          </LineSymbolizer>                                          
         </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

En SLD, les lignes n'ont pas de notion de "remplissage", mais seulement "trait". A la différence des polygones, il n'est pas possible de symboliser les "bords" d'une géométrie ligne. Il est cependant possible d'obtenir cet effent en traçant chaque ligne deux fois : une fois avec une certaine largeur, une nouvelle fois avec une largeur un peu plus faible. Ceci donne l'illusion d'un remplissage avec bords en recouvrant partout les lignes épaisses, sauf le long des bords des lignes plus fines.

Comme chaque ligne est dessinée deux fois, l'ordre de tracé est *très* important. Dans ce style, toutes les lignes grises sont dessinées d'abord avec le premier ``<FeatureTypeStyle>``, et sont suivies par toutes les lignes bleues du second ``<FeatureTypeStyle>``.  GeoServer va tracer tous les ``<FeatureTypeStyle>`` dans l'ordre d'apparition dans le SLD. Ceci permet non seulement de s'assurer que les lignes bleues ne seront pas recouvertes par les lignes grises, mais permet aussi un dessin propre aux intersections, à l'endroit où les lignes bleues se "connectent".

Dans cet exemple, les **lines 1-11** comprennent le premier ``<FeatureTypeStyle>``, qui est la ligne extérieure (le "trait").  La **ligne 5** règle la couleur de la ligne à gris foncé (``#333333``), la **ligne 6** règle la largeur de cette ligne à 5 pixels, et la **ligne 7** arrondit les extrémités de la ligne, plats par défaut.  (Lorsque l'on travaille avec des lignes avec bords, utiliser le paramètre ``stroke-linecap`` permet de s'assurer que les fins de lignes ont des bords correctement dessinés.)

Les **lignes 12-22** comprennent le second ``<FeatureTypeStyle>`` qui correspond à l'intérieur de la ligne (le "remplissage"). lA **Ligne 16** règle la couleur de la ligne à bleu moyen (``#6699FF``), la **ligne 17** règle la largeur de cette ligne à 3 pixels, et la **ligne 18** arrondit les extrémités des lignes au lieu de les dessiner plates.

Le résultat est une ligne bleue de 3 pixels avec un bord d'un pixel, puisque la ligne grise de 5 pixels affichera 1 pixel de part et d'autre de la ligne bleue de 3 pixels.

Ligne pointillée
----------------

Cet exemple modifie :ref:`sld_cookbook_lines_simpleline` pour créer une ligne pointillée constituée de segments de 5 pixels alternés avec des espaces de 2 pixels.

.. figure:: images/line_dashedline.png
   :align: center

   *Ligne pointillée*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "ligne pointillée" <artifacts/line_dashedline.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#0000FF</CssParameter>
              <CssParameter name="stroke-width">3</CssParameter>
              <CssParameter name="stroke-dasharray">5 2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Dans cet exemple, la **ligne 5** règle la couleur des lignes à bleu (``#0000FF``) et la **ligne 6** règle la largeur des lignes à 3 pixels. La **ligne 7** détermine la composition des tirets. la valeur ``5 2`` crée un motif de 5 pixels de ligne dessinée, suivi de 2 pixels sans ligne.


Voie ferrée (hachures)
----------------------

Cet exemple utilise des hachures pour fabriquer un style voie ferrée. Ligne et hachures sont en noir, avec une épaisseur de 2 pixels pour la ligne principale et de 1 pixel pour les hachures perpendiculaires.

.. note:: Cet exemple utilise une extension SLD de GeoServer. Les hachures ne font pas partie de la spécification SLD 1.0.

.. figure:: images/line_railroad.png
   :align: center

   *Voie ferrée (hachures)*

Code
~~~~

:download:`Consultez et télchargez le SLD complet "Voie ferrée (hachures)" <artifacts/line_railroad.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#333333</CssParameter>
              <CssParameter name="stroke-width">3</CssParameter>
            </Stroke>
          </LineSymbolizer> 
        </Rule>         
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <GraphicStroke>
                <Graphic>
                  <Mark>
                    <WellKnownName>shape://vertline</WellKnownName>
                    <Stroke>
                      <CssParameter name="stroke">#333333</CssParameter>
                      <CssParameter name="stroke-width">1</CssParameter>
                    </Stroke>
                  </Mark>
                  <Size>12</Size>
                </Graphic>
              </GraphicStroke>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Détails
~~~~~~~

Dans cet exemple il y a deux règles, chacune contenant un ``<LineSymbolizer>`` (Chaque ``<LineSymbolizer>`` doit être dans une règle distincte). La première règle, en  **lignes 2-8**, trace une ligne standarde, avec la **ligne 5** dessinant les lignes en gris foncé (``#333333``) et la **ligne 6** paramétrant la largeur des lignes à 2 pixels.

Les hachures sont invoquées dans la deuxième règle, en **lignes 10-27**. La **ligne 16** dit que la règle trace des hachures verticales (``shape://vertline``) perpendiculaires à la géométrie de la ligne. Les **lignes 18-19** règlent la couleur des hachures à gris foncé (``#333333``) et la largeur à 1 pixel. Enfin, la ``ligne 22`` dit que la longueur des hachures et la distance entre chaque hachure doivent être de 12 pixels.

Symboles graphiques espacés
---------------------------

Cet exemple utilise un symbole graphique "trait" et une matrice de hachures pour créer un type de ligne "point espace". En l'absence de matrice de hachures, les lignes seraient remplies de points successifs se touchant.

L'ajout de la matrice de hachures permet de contrôler la taille de l'espace entre un symbole et le suivant.

.. note:: Cet exemple ne fonctionnera probablement pas avec des systèmes tiers supportant SLD. Bien que ce soit parfaitement conforme à SLD, à notre connaissance aucun autre système ne permet de combiner l'utilisation de ``dasharray`` et de symboles graphiques "trait" (la spécification SLD ne dit pas ce que cette combinaison est censée produire). 

.. figure:: images/line_dashspace.png
   :align: center

   *Symboles espacés le long d'une ligne*

Code
~~~~

:download:`Consultez et téléchargez le SLD complet "Symboles espacés" <artifacts/line_dashspace.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <GraphicStroke>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Stroke>
                      <CssParameter name="stroke">#333333</CssParameter>
                      <CssParameter name="stroke-width">1</CssParameter>
                    </Stroke>
                    <Fill>
                      <CssParameter name="stroke">#666666</CssParameter>  
                    </Fill>
                  </Mark>
                  <Size>4</Size>
                  <CssParameter name="stroke-dasharray">4 6</CssParameter>
                </Graphic>
              </GraphicStroke>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
      
Détails
~~~~~~~
Cet exemple, comme les précédents, utilise un ``GraphicStroke`` pour placer un symbole graphique le long d'une ligne.
Le symbole défini en **lignes 7-16** est un cercle gris de 4 pixels avec un contour gris foncé.
L'espacement entre les symboles est contrôlé par ``dasharray`` en **ligne 18**, réglant 4 pixels de tracé, juste assez pour dessiner le cercle, puis 6 pixels crayon levé, ce qui produit un espacement.


.. _sld_cookbook_lines_defaultlabel:

Alternating symbols with dash offsets
-------------------------------------

This example shows how to create a complex line style which alternates a symbol and a line segment.
The example builds on the knowledge gathered in previous sections:

  * `dasharray` allows to control pen down/pen up behavior and generate dashed lines
  * `GraphicStroke` allows to place symbols along a line
  * combining the two togheter it's possible to control symbol spacing
  
This example adds the usage of `dashoffset`, which controls at which point of the ``dasharray`` sequence the renderer starts drawing the repeating pattern. For example, having a dash array of ``5 10`` and a dash offset of ``7`` the renderer would start the repeating pattern 7 pixels after its beginnig, so it would jump over the "5 pixels pen down" section and 2 more pixels in the pen up section, performing a residual of 8 pixels up, then 5 down, 10 up, and so on.

This can be used to create two synchronized sequences of dash arrays, one drawing line segments, and the other symbols along a line, like in the following example.

.. note:: This example is not likely to work with other systems supporting SLD. While the SLD is perfectly compliant we are not aware of other systems allowing to combine the usage of ``dasharray`` and graphics strokes (the SLD specification does not say what this combination is supposed to produce). 

.. figure:: images/line_dashdot.png
   :align: center

   *Dash and symbol*

Code
~~~~

:download:`View and download the full "Spaced symbols" SLD <artifacts/line_dashdot.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#0000FF</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
              <CssParameter name="stroke-dasharray">10 10</CssParameter>
            </Stroke>
          </LineSymbolizer>
          <LineSymbolizer>
            <Stroke>
              <GraphicStroke>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Stroke>
                      <CssParameter name="stroke">#000033</CssParameter>
                      <CssParameter name="stroke-width">1</CssParameter>
                    </Stroke>
                  </Mark>
                  <Size>5</Size>
                  <CssParameter name="stroke-dasharray">5 15</CssParameter>
                  <CssParameter name="stroke-dashoffset">7.5</CssParameter>
                </Graphic>
              </GraphicStroke>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Details
~~~~~~~

In this example two dash array based line symbolizers are used to generate an alternating sequence.
The first one, defined at **lines 3-9** is a simple line dash array alternating 10 pixels of pen down with 10 pixels of pen up. 
The second one, defined at **lines 10-27** alternates a 5 pixels wide empty circle with 15 pixels of white space.
In order to have the two symbolizers alternate the second one uses a dashoffset of 7.5, making the sequence start with 12.5 pixels of white space, then a circle (which is then centered between the two line segments of the other pattern), then 15 pixels of white space, and so on.

Line with default label
-----------------------

This example shows a text label on the simple line.  This is how a label will be displayed in the absence of any other customization.

.. figure:: images/line_linewithdefaultlabel.png
   :align: center

   *Line with default label*

Code
~~~~

:download:`View and download the full "Line with default label" SLD <artifacts/line_linewithdefaultlabel.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
            </Stroke>
          </LineSymbolizer>
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

Details
~~~~~~~

In this example, there is one rule with a ``<LineSymbolizer>`` and a ``<TextSymbolizer>``.  The ``<LineSymbolizer>`` (**lines 3-7**) draws red lines (``#FF0000``).  Since no width is specified, the default is set to 1 pixel.  The ``<TextSymbolizer>`` (**lines 8-15**) determines the labeling of the lines.  **Lines 9-11** specify that the text of the label will be determined by the value of the "name" attribute for each line.  (Refer to the attribute table in the :ref:`sld_cookbook_lines_attributes` section if necessary.)  **Line 13** sets the text color to black.  All other details about the label are set to the renderer default, which here is Times New Roman font, font color black, and font size of 10 pixels.


.. _sld_cookbook_lines_labelfollowingline:

Label following line
--------------------

This example renders the text label to follow the contour of the lines.

.. note:: Labels following lines is an SLD extension specific to GeoServer.  It is not part of the SLD 1.0 specification.

.. figure:: images/line_labelfollowingline.png
   :align: center

   *Label following line*

Code
~~~~

:download:`View and download the full "Label following line" SLD <artifacts/line_labelfollowingline.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
            </Stroke>
          </LineSymbolizer>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
            <VendorOption name="followLine">true</VendorOption>
            <LabelPlacement>
              <LinePlacement />
            </LabelPlacement>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Details
~~~~~~~

As the :ref:`sld_cookbook_lines_defaultlabel` example showed, the default label behavior isn't very optimal.  The label is displayed at a tangent to the line itself, leading to uncertainty as to which label corresponds to which line.

This example is similar to the :ref:`sld_cookbook_lines_defaultlabel` example with the exception of **lines 15-18**.  **Line 15** sets the option to have the label follow the line, while **lines 16-18** specify that the label is placed along a line.  If ``<LinePlacement />`` is not specified in an SLD, then ``<PointPlacement />`` is assumed, which isn't compatible with line-specific rendering options.

.. note:: Not all labels are shown due to label conflict resolution.  See the next section on :ref:`sld_cookbook_lines_optimizedlabel` for an example of how to maximize label display.


.. _sld_cookbook_lines_optimizedlabel:

Optimized label placement
-------------------------

This example optimizes label placement for lines such that the maximum number of labels are displayed.

.. note:: This example uses options that are specific to GeoServer and are not part of the SLD 1.0 specification.


.. figure:: images/line_optimizedlabel.png
   :align: center

   *Optimized label*

Code
~~~~

:download:`View and download the full "Optimized label" SLD <artifacts/line_optimizedlabel.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
            </Stroke>
          </LineSymbolizer>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
            <VendorOption name="followLine">true</VendorOption>
            <VendorOption name="maxAngleDelta">90</VendorOption>
            <VendorOption name="maxDisplacement">400</VendorOption>
            <VendorOption name="repeat">150</VendorOption>
            <LabelPlacement>
              <LinePlacement />
            </LabelPlacement>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Details
~~~~~~~

GeoServer uses "conflict resolution" to ensure that labels aren't drawn on top of other labels, obscuring them both.  This accounts for the reason why many lines don't have labels in the previous example, :ref:`sld_cookbook_lines_labelfollowingline`.  While this setting can be toggled, it is usually a good idea to leave it on and use other label placement options to ensure that labels are drawn as often as desired and in the correct places.  This example does just that.

This example is similar to the previous example, :ref:`sld_cookbook_lines_labelfollowingline`.  The only differences are contained in **lines 16-18**.  **Line 16** sets the maximum angle that the label will follow.  This sets the label to never bend more than 90 degrees to prevent the label from becoming illegible due to a pronounced curve or angle.  **Line 17** sets the maximum displacement of the label to be 400 pixels.  In order to resolve conflicts with overlapping labels, GeoServer will attempt to move the labels such that they are no longer overlapping.  This value sets how far the label can be moved relative to its original placement.  Finally, **line 18** sets the labels to be repeated every 150 pixels.  A feature will typically receive only one label, but this can cause confusion for long lines. Setting the label to repeat ensures that the line is always labeled locally.


.. _sld_cookbook_lines_optimizedstyledlabel:

Optimized and styled label
--------------------------

This example improves the style of the labels from the :ref:`sld_cookbook_lines_optimizedlabel` example.

.. figure:: images/line_optimizedstyledlabel.png
   :align: center

   *Optimized and styled label*

Code
~~~~

:download:`View and download the full "Optimized and styled label" SLD <artifacts/line_optimizedstyledlabel.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
            </Stroke>
          </LineSymbolizer>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-size">10</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
              <CssParameter name="font-weight">bold</CssParameter>
            </Font>
            <VendorOption name="followLine">true</VendorOption>
            <VendorOption name="maxAngleDelta">90</VendorOption>
            <VendorOption name="maxDisplacement">400</VendorOption>
            <VendorOption name="repeat">150</VendorOption>
            <LabelPlacement>
              <LinePlacement />
            </LabelPlacement>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Details
~~~~~~~

This example is similar to the :ref:`sld_cookbook_lines_optimizedlabel`.  The only difference is in the font information, which is contained in **lines 15-20**.  **Line 16** sets the font family to be "Arial", **line 17** sets the font size to 10, **line 18** sets the font style to "normal" (as opposed to "italic" or "oblique"), and **line 19** sets the font weight to "bold" (as opposed to "normal").


Attribute-based line
--------------------

This example styles the lines differently based on the "type" (Road class) attribute.

.. figure:: images/line_attributebasedline.png
   :align: center

   *Attribute-based line*

Code
~~~~

:download:`View and download the full "Attribute-based line" SLD <artifacts/line_attributebasedline.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <Name>local-road</Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>type</ogc:PropertyName>
              <ogc:Literal>local-road</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#009933</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
      <FeatureTypeStyle>
        <Rule>
          <Name>secondary</Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>type</ogc:PropertyName>
              <ogc:Literal>secondary</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#0055CC</CssParameter>
              <CssParameter name="stroke-width">3</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
      <FeatureTypeStyle>
        <Rule>
        <Name>highway</Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>type</ogc:PropertyName>
              <ogc:Literal>highway</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
              <CssParameter name="stroke-width">6</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>


Details
~~~~~~~

.. note:: Refer to the :ref:`sld_cookbook_lines_attributes` to see the attributes for the layer.  This example has eschewed labels in order to simplify the style, but you can refer to the example :ref:`sld_cookbook_lines_optimizedstyledlabel` to see which attributes correspond to which points.

There are three types of road classes in our fictional country, ranging from back roads to high-speed freeways: "highway", "secondary", and "local-road".  In order to handle each case separately, there is more than one ``<FeatureTypeStyle>``, each containing a single rule.  This ensures that each road type is rendered in order, as each ``<FeatureTypeStyle>`` is drawn based on the order in which it appears in the SLD.

The three rules are designed as follows:

.. list-table::
   :widths: 20 30 30 20

   * - **Rule order**
     - **Rule name / type**
     - **Color**
     - **Size**
   * - 1
     - local-road
     - ``#009933`` (green)
     - 2
   * - 2
     - secondary
     - ``#0055CC`` (blue)
     - 3
   * - 3
     - highway
     - ``#FF0000`` (red)
     - 6

**Lines 2-16** comprise the first ``<Rule>``.  **Lines 4-9** set the filter for this rule, such that the "type" attribute has a value of "local-road".  If this condition is true for a particular line, the rule is rendered according to the ``<LineSymbolizer>`` which is on **lines 10-15**.  **Lines 12-13** set the color of the line to be a dark green (``#009933``) and the width to be 2 pixels.

**Lines 19-33** comprise the second ``<Rule>``.  **Lines 21-26** set the filter for this rule, such that the "type" attribute has a value of "secondary".  If this condition is true for a particular line, the rule is rendered according to the ``<LineSymbolizer>`` which is on **lines 27-32**.  **Lines 29-30** set the color of the line to be a dark blue (``#0055CC``) and the width to be 3 pixels, making the lines slightly thicker than the "local-road" lines and also a different color.

**Lines 36-50** comprise the third and final ``<Rule>``.  **Lines 38-43** set the filter for this rule, such that the "type" attribute has a value of "primary".  If this condition is true for a particular line, the rule is rendered according to the ``<LineSymbolizer>`` which is on **lines 44-49**.  **Lines 46-47** set the color of the line to be a bright red (``#FF0000``) and the width to be 6 pixels, so that these lines are rendered on top of and thicker than the other two road classes.  In this way, the "primary" roads are given priority in the map rendering.


Zoom-based line
---------------

This example alters the :ref:`sld_cookbook_lines_simpleline` style at different zoom levels.

.. figure:: images/line_zoombasedlinelarge.png
   :align: center

   *Zoom-based line: Zoomed in*


.. figure:: images/line_zoombasedlinemedium.png
   :align: center

   *Zoom-based line: Partially zoomed*


.. figure:: images/line_zoombasedlinesmall.png
   :align: center

   *Zoom-based line: Zoomed out*

Code
~~~~

:download:`View and download the full "Zoom-based line" SLD <artifacts/line_zoombasedline.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <Name>Large</Name>
          <MaxScaleDenominator>180000000</MaxScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#009933</CssParameter>
              <CssParameter name="stroke-width">6</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Medium</Name>
          <MinScaleDenominator>180000000</MinScaleDenominator>
          <MaxScaleDenominator>360000000</MaxScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#009933</CssParameter>
              <CssParameter name="stroke-width">4</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Name>Small</Name>
          <MinScaleDenominator>360000000</MinScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#009933</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Details
~~~~~~~

It is often desirable to make shapes larger at higher zoom levels when creating a natural-looking map. This example varies the thickness of the lines according to the zoom level (or more accurately, scale denominator).  Scale denominators refer to the scale of the map.  A scale denominator of 10,000 means the map has a scale of 1:10,000 in the units of the map projection.

.. note:: Determining the appropriate scale denominators (zoom levels) to use is beyond the scope of this example.

This style contains three rules.  The three rules are designed as follows:

.. list-table::
   :widths: 15 25 40 20 

   * - **Rule order**
     - **Rule name**
     - **Scale denominator**
     - **Line width**
   * - 1
     - Large
     - 1:180,000,000 or less
     - 6
   * - 2
     - Medium
     - 1:180,000,000 to 1:360,000,000
     - 4
   * - 3
     - Small
     - Greater than 1:360,000,000
     - 2

The order of these rules does not matter since the scales denominated in each rule do not overlap.

The first rule (**lines 2-11**) is the smallest scale denominator, corresponding to when the view is "zoomed in".  The scale rule is set on **line 4**, so that the rule will apply to any map with a scale denominator of 180,000,000 or less.  **Line 7-8** draws the line to be dark green (``#009933``) with a width of 6 pixels.

The second rule (**lines 12-22**) is the intermediate scale denominator, corresponding to when the view is "partially zoomed".  **Lines 14-15** set the scale such that the rule will apply to any map with scale denominators between 180,000,000 and 360,000,000.  (The ``<MinScaleDenominator>`` is inclusive and the ``<MaxScaleDenominator>`` is exclusive, so a zoom level of exactly 360,000,000 would *not* apply here.)  Aside from the scale, the only difference between this rule and the previous is the width of the lines, which is set to 4 pixels on **line 19**.

The third rule (**lines 23-32**) is the largest scale denominator, corresponding to when the map is "zoomed out".  The scale rule is set on **line 25**, so that the rule will apply to any map with a scale denominator of 360,000,000 or greater.  Again, the only other difference between this rule and the others is the width of the lines, which is set to 2 pixels on **line 29**.

The result of this style is that lines are drawn with larger widths as one zooms in and smaller widths as one zooms out.

