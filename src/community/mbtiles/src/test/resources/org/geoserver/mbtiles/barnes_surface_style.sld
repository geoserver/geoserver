<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" 
 xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
 xmlns="http://www.opengis.net/sld" 
 xmlns:ogc="http://www.opengis.net/ogc" 
 xmlns:xlink="http://www.w3.org/1999/xlink" 
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>BarnesContours</Name>
    <UserStyle>
      <Title>Barnes Surface</Title>
      <Abstract>A Barnes Surface</Abstract>
      <FeatureTypeStyle>
        <Transformation>
          <ogc:Function name="vec:BarnesSurface">
            <ogc:Function name="parameter">
              <ogc:Literal>data</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>valueAttr</ogc:Literal>
              <ogc:Literal>Double3</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
             <ogc:Literal>scale</ogc:Literal>
              <ogc:Literal>10000</ogc:Literal>
             </ogc:Function>
            <ogc:Function name="parameter">
             <ogc:Literal>convergence</ogc:Literal>
             <ogc:Literal>0.5</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
             <ogc:Literal>passes</ogc:Literal>
             <ogc:Literal>3</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
                               <ogc:Literal>minObservations</ogc:Literal>
                               <ogc:Literal>1</ogc:Literal>
                             </ogc:Function>
                             <ogc:Function name="parameter">
                               <ogc:Literal>maxObservationDistance</ogc:Literal>
                               <ogc:Literal>70000</ogc:Literal>
                             </ogc:Function>
                             <ogc:Function name="parameter">
                               <ogc:Literal>pixelsPerCell</ogc:Literal>
                               <ogc:Literal>10</ogc:Literal>
                             </ogc:Function>
                             <ogc:Function name="parameter">
             <ogc:Literal>queryBuffer</ogc:Literal>
            <ogc:Literal>100000</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>outputBBOX</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_bbox</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>outputWidth</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_width</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>outputHeight</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_height</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
          </ogc:Function>
        </Transformation>
        <Rule>
           <RasterSymbolizer>
                <Geometry>
                         <ogc:PropertyName>the_geom</ogc:PropertyName></Geometry>
                       <Opacity>0.8</Opacity>
                       <ColorMap type="ramp" >
                                <ColorMapEntry color="#FFFFFF" quantity="-990" label="nodata" opacity="0"/>
                                <ColorMapEntry color="#76F9FC" quantity="3.20" label="values" />
                                <ColorMapEntry color="#479364" quantity="3.25" label="values" />
                                <ColorMapEntry color="#2E6000" quantity="3.30" label="values" />
                                <ColorMapEntry color="#9AF20C" quantity="3.35" label="values" />
                                <ColorMapEntry color="#B7F318" quantity="3.40" label="values" />
                                <ColorMapEntry color="#FAF833" quantity="3.45" label="values" />
                                <ColorMapEntry color="#F9C933" quantity="3.50" label="values" />
                                <ColorMapEntry color="#ED7233" quantity="3.55" label="values" />
                                <ColorMapEntry color="#BB3026" quantity="999" label="values" />
                              </ColorMap>
                     </RasterSymbolizer>
                    </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>