/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.OpenLayersMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;

import static org.geoserver.data.test.MockData.WORLD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("Population","Population.sld",
                org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest.class,catalog);
        testData.addVectorLayer(new QName(MockData.SF_URI, "states", MockData.SF_PREFIX), 
                Collections.EMPTY_MAP,"states.properties",org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest.class,catalog);
    } 
     
    @Test
    public void testRepeatedValues() throws Exception {
        String baseRequest = "wms?service=wms&version=1.3.0&bbox=" + bbox
                + "&styles=&layers=" + layers + "&format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326";
        
        // parameter repeated, but with the same value, should work
        MockHttpServletResponse response = getAsServletResponse(baseRequest + "&format=image/png");
        checkImage(response);
        
        // parameter repeated 2 times, but with the same value, should work
        response = getAsServletResponse(baseRequest + "&format=image/png&format=image/png");
        checkImage(response);
        
        // parameter repeated with 2 different values, should throw an exception
        Document dom = getAsDOM(baseRequest + "&format=image/jpeg");
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName()); 
        Element serviceException = (Element) dom.getDocumentElement().getElementsByTagName("ServiceException").item(0);
        assertEquals("InvalidParameterValue", serviceException.getAttribute("code"));
        assertEquals("FORMAT", serviceException.getAttribute("locator"));
    }
    
    @Test
    public void testSldBody10() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD10.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    @Test
    public void testSldBody10Validate() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD10.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        checkImage(response);
        
        Document dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD10_INVALID.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true", Charset.defaultCharset().displayName());
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true", Charset.defaultCharset().displayName());
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    @Test
    public void testSldBody11() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.1.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    @Test
    public void testSldBody11Validate() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.1.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        checkImage(response);
        
        Document dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.1.0" + "&SLD_BODY="
                + STATES_SLD11_INVALID.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true", Charset.defaultCharset().displayName());
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    @Test 
    public void testSldBody11NoVersion() throws Exception {
        //will fail beacuse sld version == 1.0
        Document dom = getAsDOM("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_VERSION=1.0.0" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true", Charset.defaultCharset().displayName());
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D") + "&VALIDATESCHEMA=true");
        checkImage(response);
    }

    @Test
    public void testSldGenerateRaster() throws Exception {
        Catalog catalog = getCatalog();
        StyleGenerator generator = new StyleGenerator(catalog);
        CoverageInfo coverage = catalog.getCoverageByName(WORLD.getLocalPart());

        StyleInfo style = generator.createStyle(Styles.handler("SLD"), coverage);
        catalog.add(style);

        MockHttpServletResponse response = getAsServletResponse("wms?bbox=-120,35,-100,45" +
                "&styles=" + style.getName() + "&layers=" + coverage.getName() +
                "&format=image/png&request=GetMap&width=80&height=40&srs=EPSG:4326");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        Color rgb = new Color(image.getRGB(5,5));
        assertEquals(rgb, new Color(170,170,170));

        checkImage(response);
    }
    
    
    @Test    
    public void testLayerGroupSingle() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.SINGLE, null);
        try {
            String url = "wms?LAYERS="
                    + group.getName()
                    + "&STYLES=&FORMAT=image%2Fpng&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            BufferedImage image = getAsImage(url, "image/png");

            assertPixel(image, 150, 160, Color.WHITE);
            // places
            assertPixel(image, 180, 16, COLOR_PLACES_GRAY);
            // lakes
            assertPixel(image, 90, 200, COLOR_LAKES_BLUE);
        } finally {
            catalog.remove(group);
        }        
    }

    @Test    
    public void testLayerGroupNamed() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
        try {
            String url = "wms?LAYERS="
                    + group.getName()
                    + "&STYLES=&FORMAT=image%2Fpng&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            BufferedImage image = getAsImage(url, "image/png");

            assertPixel(image, 150, 160, Color.WHITE);
            // places
            assertPixel(image, 180, 16, COLOR_PLACES_GRAY);
            // lakes
            assertPixel(image, 90, 200, COLOR_LAKES_BLUE);
        } finally {
            catalog.remove(group);
        }               
    }

    @Test
    public void testLayerGroupContainer() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.CONTAINER, null);
        try {
            String url = "wms?LAYERS="
                    + group.getName()
                    + "&STYLES=&FORMAT=image%2Fpng&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            // this group is not meant to be called directly so we should get an exception
            MockHttpServletResponse resp = getAsServletResponse(url);
            assertEquals("text/xml", resp.getContentType());
            
            Document dom = getAsDOM(url);
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName()); 
            
            Element serviceException = (Element) dom.getDocumentElement().getElementsByTagName("ServiceException").item(0);
            assertEquals("LayerNotDefined", serviceException.getAttribute("code"));
            assertEquals("layers", serviceException.getAttribute("locator"));
            assertEquals("Could not find layer " + group.getName(), serviceException.getTextContent().trim());
        } finally {
            catalog.remove(group);
        }
    }
    
    @Test
    public void testLayerGroupModeEo() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.EO, catalog.getLayerByName(getLayerId(MockData.LAKES)));
        try {
            String url = "wms?LAYERS="
                    + group.getName()
                    + "&STYLES=&FORMAT=image%2Fpng&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&BBOX=0.0000,-0.0020,0.0035,0.0010";
            BufferedImage image = getAsImage(url, "image/png");
            
            assertPixel(image, 150, 160, Color.WHITE);
            // no places
            assertPixel(image, 180, 16, Color.WHITE);
            // lakes
            assertPixel(image, 90, 200, COLOR_LAKES_BLUE);
        } finally {
            catalog.remove(group);
        }
    }

    @Test
    public void testWorkspaceQualifiedNoLayersError() throws Exception {
        Document dom = getAsDOM("sf/wms?bbox=" + bbox + "&styles="
                + "&layers=" + "&format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326", Charset.defaultCharset().displayName());
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        assertTrue(dom.getDocumentElement().getTextContent().contains("No LAYERS has been requested"));
    }
    
    @Test
    public void testSldExternalEntities() throws Exception {
        URL sldUrl = TestData.class.getResource("externalEntities.sld");
        String url = "wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&sld=" + sldUrl.toString();

        WMS wms = new WMS(getGeoServer());
        GeoServerInfo geoserverInfo = wms.getGeoServer().getGlobal();
        try {
            // enable entities in external SLD files
            geoserverInfo.setXmlExternalEntitiesEnabled(true);
            getGeoServer().save(geoserverInfo);
            
            // if entities evaluation is enabled
            // the parser will try to read a file on the local file system
            // if the file is found, its content will be used to replace the entity
            // if the file is not found the parser will throw a FileNotFoundException
            String response = getAsString(url);            
            assertTrue(response.indexOf("Error while getting SLD.") > -1);
            
            // disable entities
            geoserverInfo.setXmlExternalEntitiesEnabled(false);
            getGeoServer().save(geoserverInfo);

            // if entities evaluation is disabled
            // the parser will throw a MalformedURLException when it finds an entity
            response = getAsString(url);
            assertTrue(response.indexOf("Entity resolution disallowed") > -1);

            // try default: disabled entities
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);

            // if entities evaluation is disabled
            // the parser will throw a MalformedURLException when it finds an entity
            response = getAsString(url);
            assertTrue(response.indexOf("Entity resolution disallowed") > -1);
            
        } finally {
            // default
            geoserverInfo.setXmlExternalEntitiesEnabled(null);
            getGeoServer().save(geoserverInfo);             
        }
    }  
    
    @Test
    public void testAllowedMimeTypes() throws Exception {
        
        WMSInfo wms = getWMS().getServiceInfo();
        GetMapOutputFormat format = new RenderedImageMapOutputFormat(getWMS());        
        wms.getGetMapMimeTypes().add(format.getMimeType());
        wms.setGetMapMimeTypeCheckingEnabled(true);
        getGeoServer().save(wms);

     // check mime type allowed
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&version=1.3.0");
        checkImage(response);
        
        
     // check mime type not allowed                
        String result = getAsString("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format="+OpenLayersMapOutputFormat.MIME_TYPE+ "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&version=1.3.0");
        assertTrue(result.indexOf("ForbiddenFormat") > 0);        
                      
        wms.getGetMapMimeTypes().clear();
        wms.setGetMapMimeTypeCheckingEnabled(false);
        getGeoServer().save(wms);
        
        result = getAsString("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format="+OpenLayersMapOutputFormat.MIME_TYPE+ "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&version=1.3.0");

        assertTrue(result.indexOf("OpenLayers") > 0);
 
    }

}
