<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<NamedLayer> <Name> area landmarks </Name>
    <UserStyle>
         <FeatureTypeStyle>
            <FeatureTypeName>Feature</FeatureTypeName>
			<Rule>  
	               <MinScaleDenominator>32000</MinScaleDenominator>
	    		   <LineSymbolizer>
	    		       <Stroke>
	    				<CssParameter name="stroke">
	    					<ogc:Literal>#666666</ogc:Literal>
	    				</CssParameter>
	    				<CssParameter name="stroke-width">
	    					<ogc:Literal>2</ogc:Literal>
	    				</CssParameter>
	    			</Stroke>
	    		   </LineSymbolizer>
            </Rule>

            <Rule>	<!-- thick line drawn first-->
				<MaxScaleDenominator>32000</MaxScaleDenominator>
				<LineSymbolizer>
					<Stroke>
						<CssParameter name="stroke">
							<ogc:Literal>#666666</ogc:Literal>
						</CssParameter>
						<CssParameter name="stroke-width">
							<ogc:Literal>7</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
            </Rule>
        </FeatureTypeStyle>
        <FeatureTypeStyle>
           <FeatureTypeName>Feature</FeatureTypeName>
           <Rule>	<!-- thin line drawn second -->
				<MaxScaleDenominator>32000</MaxScaleDenominator>
	            <LineSymbolizer>
	    		       <Stroke>
	    				<CssParameter name="stroke">
	    					<ogc:Literal>#FFFFFF</ogc:Literal>
	    				</CssParameter>
	    				<CssParameter name="stroke-width">
	    					<ogc:Literal>4</ogc:Literal>
	    				</CssParameter>
	    			</Stroke>
				</LineSymbolizer>
            </Rule> 
            <!-- label -->     
			<Rule>
				<MaxScaleDenominator>32000</MaxScaleDenominator>
				<TextSymbolizer>
					<Label>
						<ogc:PropertyName>NAME</ogc:PropertyName>
					</Label>

					<Font>
						<CssParameter name="font-family">Times New Roman</CssParameter>
						<CssParameter name="font-style">Normal</CssParameter>
						<CssParameter name="font-size">14</CssParameter>
						<CssParameter name="font-weight">bold</CssParameter>
					</Font>
					
					<LabelPlacement>
					  <LinePlacement>
					  </LinePlacement>
					</LabelPlacement>
					<Halo>
						<Radius>
							<ogc:Literal>2</ogc:Literal>
						</Radius>
						<Fill>
							<CssParameter name="fill">#FFFFFF</CssParameter>
							<CssParameter name="fill-opacity">0.85</CssParameter>				
						</Fill>
					</Halo>
					
					<Fill>
						<CssParameter name="fill">#000000</CssParameter>
					</Fill>
					
					<VendorOption name="group">true</VendorOption>
					
				</TextSymbolizer>
			</Rule>
        </FeatureTypeStyle>
        
    </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
