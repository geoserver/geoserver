.. _label_obstacles:

Label Obstacles
===============

GeoServer implements an algorithm for label conflict 
resolution, to prevent labels from overlapping one another. 
By default this algorithm only considers conflicts with other labels. 
This can result in labels 
overlapping other symbolizers, which may produce an undesirable effect. 

.. cssclass:: no-border

   .. figure:: images/label-obstacle1.jpg  
   .. figure:: images/label-obstacle2.jpg


GeoServer supports a vendor option called ``labelObstacle`` that allows
marking a symbolizer as an obstacle.
This tells the labeller to avoid rendering labels that overlap it.

.. warning::

   Beware of marking a line or poly symbolizer as a label obstacle. The label conflict resolving routine is
   based on the bounding box so marking as a label obstacle will result in no label overlapping not only
   the geometry itself, but its bounding box as well.

.. code-block:: xml

	<?xml version="1.0" encoding="ISO-8859-1"?>
	<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
	  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	  <NamedLayer>
	    <UserStyle>

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
                <VendorOption name="labelObstacle">true</VendorOption>
              </PointSymbolizer>
            </Rule>
          </FeatureTypeStyle>
	
	    </UserStyle>
	  </NamedLayer>
	</StyledLayerDescriptor>

.. cssclass:: no-border

   .. figure:: images/obs-externalGraphic1.png  
   .. figure:: images/obs-externalGraphic2.png
	
Applying the obstacle to a regular point style:

.. code-block:: xml

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
	  <VendorOption name="labelObstacle">true</VendorOption>
	</PointSymbolizer>

.. cssclass:: no-border

   .. figure:: images/obs-mark1.png  
   .. figure:: images/obs-mark2.png

Applying the obstacle to line/polygon style style:

.. cssclass:: no-border

   .. figure:: images/obs-line1.png  
   .. figure:: images/obs-line2.png
   .. figure:: images/obs-poly1.png  
   .. figure:: images/obs-poly2.png


