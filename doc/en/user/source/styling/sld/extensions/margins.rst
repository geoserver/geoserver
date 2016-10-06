.. _margins:

Adding space around graphic fills
=================================

Starting with GeoServer 2.3.4 it is possible to add white space around symbols used inside graphic fills, effectively allowing to control the density of the symbols in the map.

.. code-block:: xml

          <PolygonSymbolizer>
            <Fill>
              <GraphicFill>
                <Graphic>
                  <ExternalGraphic>
                    <OnlineResource xlink:type="simple" xlink:href="./rockFillSymbol.png"/>
                    <Format>image/png</Format>
                  </ExternalGraphic>
                </Graphic>
              </GraphicFill>
            </Fill>
            <VendorOption name="graphic-margin">10</VendorOption>
          </PolygonSymbolizer>

The above forces 10 pixels of white space above, below and on either side of the symbol, effectively adding 20 pixels of white space between the symbols in the fill.
The ``graphic-margin`` can be expressed, just like the CSS margin, in four different ways:

* top,right,bottom,left (one explicit value per margin)
* top,right-left,bottom (three values, with right and left sharing the same value)
* top-bottom,right-left (two values, top and bottom sharing the same value)
* top-right-bottom-left (single value for all four margins)
   
The ability to specify different margins allows to use more than one symbol in a fill, and synchronize the relative positions of the various symbols to generate a composite fill:

.. code-block:: xml

          <PolygonSymbolizer>
            <Fill>
              <GraphicFill>
                <Graphic>
                  <ExternalGraphic>
                    <OnlineResource xlink:type="simple" xlink:href="./boulderGeometry.png"/>
                    <Format>image/png</Format>
                  </ExternalGraphic>
                </Graphic>
              </GraphicFill>
            </Fill>
            <VendorOption name="graphic-margin">35 17 17 35</VendorOption>
          </PolygonSymbolizer>
          <PolygonSymbolizer>
            <Fill>
              <GraphicFill>
                <Graphic>
                  <ExternalGraphic>
                    <OnlineResource xlink:type="simple" xlink:href="./roughGrassFillSymbol.png"/>
                    <Format>image/png</Format>
                  </ExternalGraphic>
                </Graphic>
              </GraphicFill>
            </Fill>
            <VendorOption name="graphic-margin">16 16 32 32</VendorOption>
          </PolygonSymbolizer>

.. cssclass:: no-border

   .. figure:: images/margin.png

