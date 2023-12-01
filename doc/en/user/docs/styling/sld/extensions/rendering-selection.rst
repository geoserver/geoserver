.. _rendering_selection:

Rendering Selection
====================

GeoServer provides a ``VendorOption`` to define whether a particular element ``Rule``, ``FeatureTypeStyle`` or ``Symbolizer`` should be applied to a ``getLegendGraphic`` output or to a ``getMap`` output.

This allows to generate legends from the SLD that can be better looking and more expressive, without the underlying complexity of the actual rendered style. Other systems have a dedicated language to build legends instead. The advantage of using the same language is that dynamic behaviors, like rule removal based on the area being rendered, can be easily retained.

The vendor option is named ``inclusion``, e.g.:

* <VendorOption name="inclusion">legendOnly</VendorOption>

* <VendorOption name="inclusion">mapOnly</VendorOption>

Valid values are:

* ``legendOnly`` the element will be skipped when applying the style to the data to render map.
* ``mapOnly`` the element will be skipped when applying the style to the data to render legend.
* ``normal`` will have the same effect then omitting the VendorOption: the SLD element will be used for both map and legend (same effect as not specifying the vendor option).


Take as an example the following style: for each Rule two symbolizers are defined one that will be skipped when rendering the legend and one that will be skipped when rendering the map and loads the legend icon from an external svg file. 

.. code-block:: xml
 
 <?xml version="1.0" encoding="UTF-8"?>
 <StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
   <NamedLayer>
      <Name>Style example</Name>
      <UserStyle>
         <FeatureTypeStyle>
            <Rule>
               <ogc:Filter>
                  <ogc:PropertyIsLessThan>
                     <ogc:PropertyName>numericValue</ogc:PropertyName>
                     <ogc:Literal>90</ogc:Literal>
                  </ogc:PropertyIsLessThan>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                           <CssParameter name="fill">0xFF0000</CssParameter>
                        </Fill>
                     </Mark>
                     <Size>32</Size>
                  </Graphic>
                  <VendorOption name="inclusion">mapOnly</VendorOption>
               </PointSymbolizer>
               <PointSymbolizer>
                  <Graphic>
                     <ExternalGraphic>
                        <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon1.svg" />
                        <Format>image/svg+xml</Format>
                     </ExternalGraphic>
                     <Size>20</Size>
                  </Graphic>
                  <VendorOption name="inclusion">legendOnly</VendorOption>
               </PointSymbolizer>
            </Rule>
            <Rule>
               <ogc:Filter>
                  <ogc:And>
                     <ogc:PropertyIsGreaterThanOrEqualTo>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>90</ogc:Literal>
                     </ogc:PropertyIsGreaterThanOrEqualTo>
                     <ogc:PropertyIsLessThan>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>180</ogc:Literal>
                     </ogc:PropertyIsLessThan>
                  </ogc:And>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                           <CssParameter name="fill">#6495ED</CssParameter>
                        </Fill>
                     </Mark>
                     <Size>32</Size>
                  </Graphic>
                  <VendorOption name="inclusion">mapOnly</VendorOption>
               </PointSymbolizer>
               <PointSymbolizer>
                  <Graphic>
                     <ExternalGraphic>
                        <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon2.svg" />
                        <Format>image/svg+xml</Format>
                     </ExternalGraphic>
                     <Size>20</Size>
                  </Graphic>
                  <VendorOption name="inclusion">legendOnly</VendorOption>
               </PointSymbolizer>
            </Rule>
         </FeatureTypeStyle>
      </UserStyle>
   </NamedLayer>
 </StyledLayerDescriptor>


The same result could have been obtained by defining each rule two time each one with a single symbolizer, and defining the vendor options at the rule level.

.. code-block:: xml
  
  <?xml version="1.0" encoding="UTF-8"?>
 <StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
   <NamedLayer>
      <Name>Style example</Name>
      <UserStyle>
         <FeatureTypeStyle>
            <Rule>
               <ogc:Filter>
                  <ogc:PropertyIsLessThan>
                     <ogc:PropertyName>numericValue</ogc:PropertyName>
                     <ogc:Literal>90</ogc:Literal>
                  </ogc:PropertyIsLessThan>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                           <CssParameter name="fill">0xFF0000</CssParameter>
                        </Fill>
                     </Mark>
                     <Size>32</Size>
                  </Graphic>
               </PointSymbolizer>
               <VendorOption name="inclusion">mapOnly</VendorOption>
            </Rule>
            <Rule>
               <ogc:Filter>
                  <ogc:And>
                     <ogc:PropertyIsGreaterThanOrEqualTo>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>90</ogc:Literal>
                     </ogc:PropertyIsGreaterThanOrEqualTo>
                     <ogc:PropertyIsLessThan>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>180</ogc:Literal>
                     </ogc:PropertyIsLessThan>
                  </ogc:And>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                           <CssParameter name="fill">#6495ED</CssParameter>
                        </Fill>
                     </Mark>
                     <Size>32</Size>
                  </Graphic>
                  <VendorOption name="inclusion">mapOnly</VendorOption>
               </PointSymbolizer>
            </Rule>
            <Rule>
               <ogc:Filter>
                  <ogc:PropertyIsLessThan>
                     <ogc:PropertyName>numericValue</ogc:PropertyName>
                     <ogc:Literal>90</ogc:Literal>
                  </ogc:PropertyIsLessThan>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <ExternalGraphic>
                        <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon1.svg" />
                        <Format>image/svg+xml</Format>
                     </ExternalGraphic>
                     <Size>20</Size>
                  </Graphic>
                  <VendorOption name="inclusion">legendOnly</VendorOption>
               </PointSymbolizer>
            </Rule>
            <Rule>
               <ogc:Filter>
                  <ogc:And>
                     <ogc:PropertyIsGreaterThanOrEqualTo>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>90</ogc:Literal>
                     </ogc:PropertyIsGreaterThanOrEqualTo>
                     <ogc:PropertyIsLessThan>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>180</ogc:Literal>
                     </ogc:PropertyIsLessThan>
                  </ogc:And>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <ExternalGraphic>
                        <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon2.svg" />
                        <Format>image/svg+xml</Format>
                     </ExternalGraphic>
                     <Size>20</Size>
                  </Graphic>
                  <VendorOption name="inclusion">legendOnly</VendorOption>
               </PointSymbolizer>
            </Rule>
         </FeatureTypeStyle>
      </UserStyle>
   </NamedLayer>
 </StyledLayerDescriptor>



A third way to obtain the same result could be to define vendor options at the FeatureTypeStyle level.

.. code-block:: xml
  
   <?xml version="1.0" encoding="UTF-8"?>
 <StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
   <NamedLayer>
      <Name>Style example</Name>
      <UserStyle>
      <FeatureTypeStyle>
            <Rule>
               <ogc:Filter>
                  <ogc:PropertyIsLessThan>
                     <ogc:PropertyName>numericValue</ogc:PropertyName>
                     <ogc:Literal>90</ogc:Literal>
                  </ogc:PropertyIsLessThan>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                           <CssParameter name="fill">0xFF0000</CssParameter>
                        </Fill>
                     </Mark>
                     <Size>32</Size>
                  </Graphic>
               </PointSymbolizer>
            </Rule>
            <Rule>
               <ogc:Filter>
                  <ogc:And>
                     <ogc:PropertyIsGreaterThanOrEqualTo>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>90</ogc:Literal>
                     </ogc:PropertyIsGreaterThanOrEqualTo>
                     <ogc:PropertyIsLessThan>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>180</ogc:Literal>
                     </ogc:PropertyIsLessThan>
                  </ogc:And>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                           <CssParameter name="fill">#6495ED</CssParameter>
                        </Fill>
                     </Mark>
                     <Size>32</Size>
                  </Graphic>
               </PointSymbolizer>
            </Rule>
            <VendorOption name="inclusion">mapOnly</VendorOption>
         </FeatureTypeStyle>
         <FeatureTypeStyle>
            <Rule>
               <ogc:Filter>
                  <ogc:PropertyIsLessThan>
                     <ogc:PropertyName>numericValue</ogc:PropertyName>
                     <ogc:Literal>90</ogc:Literal>
                  </ogc:PropertyIsLessThan>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <ExternalGraphic>
                        <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon1.svg" />
                        <Format>image/svg+xml</Format>
                     </ExternalGraphic>
                     <Size>20</Size>
                  </Graphic>
               </PointSymbolizer>
            </Rule>
            <Rule>
               <ogc:Filter>
                  <ogc:And>
                     <ogc:PropertyIsGreaterThanOrEqualTo>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>90</ogc:Literal>
                     </ogc:PropertyIsGreaterThanOrEqualTo>
                     <ogc:PropertyIsLessThan>
                        <ogc:PropertyName>numericValue</ogc:PropertyName>
                        <ogc:Literal>180</ogc:Literal>
                     </ogc:PropertyIsLessThan>
                  </ogc:And>
               </ogc:Filter>
               <PointSymbolizer>
                  <Graphic>
                     <ExternalGraphic>
                        <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon2.svg" />
                        <Format>image/svg+xml</Format>
                     </ExternalGraphic>
                     <Size>20</Size>
                  </Graphic>
               </PointSymbolizer>
            </Rule>
            <VendorOption name="inclusion">legendOnly</VendorOption>
         </FeatureTypeStyle>
      </UserStyle>
   </NamedLayer>
 </StyledLayerDescriptor>
