.. _ncwms:

ncWMS WMS extensions support
============================

The **ncWMS module** adds to GeoServer the ability to support some of the ncWMS extensions to the WMS protocol and configuration.
In particular:

* Ability to create a named style by simply providing a list of colors, that will adapt to the layer in use based on request parameters and its statistics
* Ability to control the palette application in GetMap via a number of extra parameters

At the time of writing the extra calls to extract time series, elevation series, transects and NetCDF metadata are not supported.
The extension is however not NetCDF specific, but can be used with any single banded raster layer instead.

The Dynamic Palette style format
--------------------------------

A new "Dynamic palette" style format has been added that accepts a palette, one color per line, defining a color progression to be applied on raster data.
Each color can be defined using these possible syntaxes (same as ncWMS):
 
 * ``#RRGGBB``
 * ``#AARRGGBB``
 * ``0xRRGGBB``
 * ``0xAARRGGBB``
 
Comments can be added in the file by starting the line by a percentage sign. For example, a red to blue progression might look like::
 
     % Red to blue progression
     #FF0000
     #0000FF
     
.. figure:: images/redblue-editor.png
   :align: center
    
   *Configuring a dynamic palette style* 

Several ready to use palettes coming from the popular "color brewer" site are available in the `ncWMS source code repository <https://github.com/Reading-eScience-Centre/edal-java/tree/master/graphics/src/main/resources/palettes>`_.

The palette translates on the fly into a SLD with rendering transformation using the :ref:`community_colormap` module, in particular, 
the above style translates to the following style:


.. code-block:: xml
   :linenos:
   
    <?xml version="1.0" encoding="UTF-8"?>
    <sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" version="1.0.0">
      <sld:NamedLayer>
        <sld:Name/>
        <sld:UserStyle>
          <sld:Name/>
          <sld:FeatureTypeStyle>
            <sld:Transformation>
              <ogc:Function name="ras:DynamicColorMap">
                <ogc:Function name="parameter">
                  <ogc:Literal>data</ogc:Literal>
                </ogc:Function>
                <ogc:Function name="parameter">
                  <ogc:Literal>opacity</ogc:Literal>
                  <ogc:Function name="env">
                    <ogc:Literal>OPACITY</ogc:Literal>
                    <ogc:Literal>1.0</ogc:Literal>
                  </ogc:Function>
                </ogc:Function>
                <ogc:Function name="parameter">
                  <ogc:Literal>colorRamp</ogc:Literal>
                  <ogc:Function name="colormap">
                    <ogc:Literal>rgb(255,0,0);rgb(0,0,255)</ogc:Literal>
                    <ogc:Function name="env">
                      <ogc:Literal>COLORSCALERANGE_MIN</ogc:Literal>
                      <ogc:Function name="bandStats">
                        <ogc:Literal>0</ogc:Literal>
                        <ogc:Literal>minimum</ogc:Literal>
                      </ogc:Function>
                    </ogc:Function>
                    <ogc:Function name="env">
                      <ogc:Literal>COLORSCALERANGE_MAX</ogc:Literal>
                      <ogc:Function name="bandStats">
                        <ogc:Literal>0</ogc:Literal>
                        <ogc:Literal>maximum</ogc:Literal>
                      </ogc:Function>
                    </ogc:Function>
                    <ogc:Function name="env">
                      <ogc:Literal>BELOWMINCOLOR</ogc:Literal>
                      <ogc:Literal>rgba(0,0,0,0)</ogc:Literal>
                    </ogc:Function>
                    <ogc:Function name="env">
                      <ogc:Literal>ABOVEMAXCOLOR</ogc:Literal>
                      <ogc:Literal>rgba(0,0,0,0)</ogc:Literal>
                    </ogc:Function>
                    <ogc:Function name="env">
                      <ogc:Literal>LOGSCALE</ogc:Literal>
                      <ogc:Literal>false</ogc:Literal>
                    </ogc:Function>
                    <ogc:Function name="env">
                      <ogc:Literal>NUMCOLORBANDS</ogc:Literal>
                      <ogc:Literal>254</ogc:Literal>
                    </ogc:Function>
                  </ogc:Function>
                </ogc:Function>
              </ogc:Function>
            </sld:Transformation>
            <sld:Rule>
              <sld:RasterSymbolizer/>
            </sld:Rule>
          </sld:FeatureTypeStyle>
        </sld:UserStyle>
      </sld:NamedLayer>
    </sld:StyledLayerDescriptor>
  
The above explains a bit of how the palette is applied:

* By default a palette of 254 colors is generated between a min and max value, plus one color for anything below the minimum and another for anything above the maximum
* It is possible to pass the minimum and maximum values using the GetMap ``env`` parameter, if not provided, they are fetched from the configured band statistics (as found in the layer configuration)
* The overall opacity of the palette can be controlled (using a value between 0 and 1 to conform with the SLD opacity description)
* The scale can be either linear, or logarithmic

.. figure:: images/bandrange.png
   :align: center
    
   *Editing the defaults for min/max scale range values in the GeoServer layer editor* 


The above parameters can all be used at will to control the palette generation using the typical environment variable approach. However, it's also possible to use ncWMS own extensions, which
are adding direct parameters in the request. See the following section for details.

ncWMS GetMap extensions
-----------------------

This module also adds a dynamic translator taking the ncWMS GetMap vendor parameters and mapping them to the dynamic palette expectations. In particular (copying the parameter description 
from the ncWMS manual, with GeoServer specific annotations):

* COLORSCALERANGE: Of the form min,max this is the scale range used for plotting the data (mapped to the COLORSCALERANGE_MIN and COLORSCALERANGE_MAX env vars)
* NUMCOLORBANDS: The number of discrete colours to plot the data. Must be between 2 and 250 (mapped to the NUMCOLORBANDS env variable)
* ABOVEMAXCOLOR: The colour to plot values which are above the maximum end of the scale range. Colours are of the form 0xRRGGBB or 0xAARRGGBB, and it also accepts "transparent" and "extend"
* BELOWMINCOLOR: The colour to plot values which are below the minimum end of the scale range. Colours are of the form 0xRRGGBB or 0xAARRGGBB, and it also accepts "transparent" and "extend"
* LOGSCALE: "true" or "false" - whether to plot data with a logarithmic scale
* OPACITY: The percentage opacity of the final output image as a number between 0 and 100 (maps to OPACITY env var by translating it to a number between 0 and 1)
* ANIMATION: "true" or "false" - whether to generate an animation. The ncWMS documentation states that TIME has to be of the form ``starttime/endtime``,
  but currently TIME needs to be a list of discrete times instead. Animation requires using the "image/gif" as the response format (as the only format supporting animation) 

Here are a few examples based on the "ArcSample" arcgrid sample layer, containing annual precipitation data. The one band provided by this layer has been configured with a default range of 0 to 6000.

* Default output with the "redblue" palette:
  
  http://localhost:8080/geoserver/wms?STYLES=redblue&LAYERS=nurc%3AArc_Sample&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&BBOX=-180,-90,180,90&WIDTH=500&HEIGHT=250
  
.. figure:: images/redblue-default.png
   :align: center
   
* Adopting a logarithmic scale by adding ``&COLORSCALERANGE=1,6000&LOGSCALE=true`` (a logarithmic scale needs a positive minimum)

.. figure:: images/redblue-logscale.png
   :align: center

* Using just 5 colors in logarithmic mode by adding ``&COLORSCALERANGE=1,6000&LOGSCALE=true&NUMCOLORBANDS=5``

.. figure:: images/redblue-numcolors.png
   :align: center

* Limiting the range and specifying other colors above (gray) and below (yellow) by adding ``&COLORSCALERANGE=100,2000&BELOWMINCOLOR=0xFFFF00&ABOVEMAXCOLOR=0xAAAAAA``

.. figure:: images/redblue-range.png
   :align: center
   
