package org.geoserver.wms.wms_1_3;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetMapIntegrationTest extends WMSTestSupport {

    String bbox = "-130,24,-66,50";
    String styles = "states";
    String layers = "sf:states";

    public static final String STATES_SLD10 =
        "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" version=\"1.0.0\">"+
        " <NamedLayer>"+
        "  <Name>sf:states</Name>"+
        "  <UserStyle>"+
        "   <Name>UserSelection</Name>"+
        "   <FeatureTypeStyle>"+
        "    <Rule>"+
        "     <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">"+
        "      <ogc:PropertyIsEqualTo>"+
        "       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName>"+
        "       <ogc:Literal>IL</ogc:Literal>"+
        "      </ogc:PropertyIsEqualTo>"+
        "     </ogc:Filter>"+
        "     <PolygonSymbolizer>"+
        "      <Fill>"+
        "       <CssParameter name=\"fill\">#FF0000</CssParameter>"+
        "      </Fill>"+
        "     </PolygonSymbolizer>"+
        "    </Rule>"+
        "    <Rule>"+
        "     <LineSymbolizer>"+
        "      <Stroke/>"+
        "     </LineSymbolizer>"+
        "    </Rule>"+
        "   </FeatureTypeStyle>"+
        "  </UserStyle>"+
        " </NamedLayer>"+
        "</StyledLayerDescriptor>";

    public static final String STATES_SLD10_INVALID =
        "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" version=\"1.0.0\">"+
        " <NamedLayer>"+
        "  <Name>sf:states</Name>"+
        "  <UserStyle>"+
        "   <Name>UserSelection</Name>"+
        "   <FeatureTypeStyle>"+
        "    <Rule>"+
        "     <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">"+
        "      <ogc:PropertyIsEqualTo>"+
        "       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName>"+
        "       <ogc:Literal>IL</ogc:Literal>"+
        "      </ogc:PropertyIsEqualTo>"+
        "     </ogc:Filter>"+
        "     <PolygonSymbolizer>"+
        "      <Font/> <!-- invalid! -->" + 
        "      <Fill>"+
        "       <CssParameter name=\"fill\">#FF0000</CssParameter>"+
        "      </Fill>"+
        "     </PolygonSymbolizer>"+
        "    </Rule>"+
        "    <Rule>"+
        "     <LineSymbolizer>"+
        "      <Stroke/>"+
        "     </LineSymbolizer>"+
        "    </Rule>"+
        "   </FeatureTypeStyle>"+
        "  </UserStyle>"+
        " </NamedLayer>"+
        "</StyledLayerDescriptor>";

     public static final String STATES_SLD11 =
        "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" " +
        "       xmlns:se=\"http://www.opengis.net/se\" version=\"1.1.0\"> "+
        " <NamedLayer> "+
        "  <se:Name>sf:states</se:Name> "+
        "  <UserStyle> "+
        "   <se:Name>UserSelection</se:Name> "+
        "   <se:FeatureTypeStyle> "+
        "    <se:Rule> "+
        "     <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
        "      <ogc:PropertyIsEqualTo> "+
        "       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName> "+
        "       <ogc:Literal>IL</ogc:Literal> "+
        "      </ogc:PropertyIsEqualTo> "+
        "     </ogc:Filter> "+
        "     <se:PolygonSymbolizer> "+
        "      <se:Fill> "+
        "       <se:SvgParameter name=\"fill\">#FF0000</se:SvgParameter> "+
        "      </se:Fill> "+
        "     </se:PolygonSymbolizer> "+
        "    </se:Rule> "+
        "    <se:Rule> "+
        "     <se:LineSymbolizer> "+
        "      <se:Stroke/> "+
        "     </se:LineSymbolizer> "+
        "    </se:Rule> "+
        "   </se:FeatureTypeStyle> "+
        "  </UserStyle> "+
        " </NamedLayer> "+
        "</StyledLayerDescriptor>";
     
     public static final String STATES_SLD11_INVALID =
         "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" " +
         "       xmlns:se=\"http://www.opengis.net/se\" version=\"1.1.0\"> "+
         " <NamedLayer> "+
         "  <se:Name>sf:states</se:Name> "+
         "  <UserStyle> "+
         "   <se:Name>UserSelection</se:Name> "+
         "   <se:FeatureTypeStyle> "+
         "    <se:Rule> "+
         "     <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\"> "+
         "      <ogc:PropertyIsEqualTo> "+
         "       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName> "+
         "       <ogc:Literal>IL</ogc:Literal> "+
         "      </ogc:PropertyIsEqualTo> "+
         "     </ogc:Filter> "+
         "     <se:PolygonSymbolizer> "+
         "      <se:Fill> "+
         "       <se:SvgParameter name=\"fill\">#FF0000</se:SvgParameter> "+
         "      </se:Fill> "+
         "      <se:Font/> <!-- invalid -->" +
         "     </se:PolygonSymbolizer> "+
         "    </se:Rule> "+
         "    <se:Rule> "+
         "     <se:LineSymbolizer> "+
         "      <se:Stroke/> "+
         "     </se:LineSymbolizer> "+
         "    </se:Rule> "+
         "   </se:FeatureTypeStyle> "+
         "  </UserStyle> "+
         " </NamedLayer> "+
         "</StyledLayerDescriptor>";
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("Population",
                org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest.class.getResource("Population.sld"));
        dataDirectory.addPropertiesType(new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest.class.getResource("states.properties"), null);
    }
    
    public void testSldBody10() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD10.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    public void testSldBody10Validate() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD10.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        checkImage(response);
        
        Document dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD10_INVALID.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    public void testSldBody11() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.1.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    public void testSldBody11Validate() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.1.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        checkImage(response);
        
        Document dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.1.0" + "&SLD_BODY="
                + STATES_SLD11_INVALID.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    public void testSldBody11NoVersion() throws Exception {
        //will fail beacuse sld version == 1.0
        Document dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        checkImage(response);
    }
}
