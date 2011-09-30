/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetMapIntegrationTest extends WMSTestSupport {
    String bbox = "-130,24,-66,50";

    String styles = "states";

    String layers = "sf:states";

    public static final String STATES_SLD = "<StyledLayerDescriptor version=\"1.0.0\">"
            + "<UserLayer><Name>sf:states</Name><UserStyle><Name>UserSelection</Name>"
            + "<FeatureTypeStyle><Rule><Filter xmlns:gml=\"http://www.opengis.net/gml\">"
            + "<PropertyIsEqualTo><PropertyName>STATE_ABBR</PropertyName><Literal>IL</Literal></PropertyIsEqualTo>"
            + "</Filter><PolygonSymbolizer><Fill><CssParameter name=\"fill\">#FF0000</CssParameter></Fill>"
            + "</PolygonSymbolizer></Rule><Rule><LineSymbolizer><Stroke/></LineSymbolizer></Rule>"
            + "</FeatureTypeStyle></UserStyle></UserLayer></StyledLayerDescriptor>";
    
    public static final String STATES_SLD11 = 
        "<StyledLayerDescriptor version=\"1.1.0\"> "+
        " <UserLayer> "+
        "  <Name>sf:states</Name> "+
        "  <UserStyle> "+
        "   <Name>UserSelection</Name> "+
        "   <se:FeatureTypeStyle xmlns:se=\"http://www.opengis.net/se\"> "+
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
        " </UserLayer> "+
        "</StyledLayerDescriptor>";


    public static final String STATES_GETMAP = //
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n "
            + "<ogc:GetMap service=\"WMS\"  version=\"1.1.1\" \n "
            + "        xmlns:gml=\"http://www.opengis.net/gml\"\n "
            + "        xmlns:ogc=\"http://www.opengis.net/ows\"\n "
            + "        xmlns:sld=\"http://www.opengis.net/sld\"\n "
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n "
            + "        xsi:schemaLocation=\"http://www.opengis.net/ows GetMap.xsd http://www.opengis.net/gml geometry.xsd http://www.opengis.net/sld StyledLayerDescriptor.xsd \">\n "
            + "        <sld:StyledLayerDescriptor>\n " + "                <sld:NamedLayer>\n "
            + "                        <sld:Name>sf:states</sld:Name>\n "
            + "                        <sld:NamedStyle>\n "
            + "                                <sld:Name>Default</sld:Name>\n "
            + "                        </sld:NamedStyle>\n "
            + "                </sld:NamedLayer>\n " + "        </sld:StyledLayerDescriptor>\n "
            + "        <ogc:BoundingBox srsName=\"4326\">\n " + "                <gml:coord>\n "
            + "                        <gml:X>-130</gml:X>\n "
            + "                        <gml:Y>24</gml:Y>\n " + "                </gml:coord>\n "
            + "                <gml:coord>\n " + "                        <gml:X>-66</gml:X>\n "
            + "                        <gml:Y>50</gml:Y>\n " + "                </gml:coord>\n "
            + "        </ogc:BoundingBox>\n " + "        <ogc:Output>\n "
            + "                <ogc:Format>image/png</ogc:Format>\n "
            + "                <ogc:Size>\n "
            + "                        <ogc:Width>550</ogc:Width>\n "
            + "                        <ogc:Height>250</ogc:Height>\n "
            + "                </ogc:Size>\n " + "        </ogc:Output>\n " + "</ogc:GetMap>\n ";

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetMapIntegrationTest());
    }
    
    @Override
    public void setUpInternal() throws Exception {
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("Population",
                GetMapIntegrationTest.class.getResource("Population.sld"));
        dataDirectory.addPropertiesType(new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                getClass().getResource("states.properties"), null);
        
        
        // add a parametric style to the mix
        dataDirectory.addStyle("parametric", WMSTestSupport.class.getResource("map/parametric.sld"));
    }

    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    public void testImage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        checkImage(response);
    }
    
    public void testGeotiffMime() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=&layers=" + layers + "&Format=image/geotiff" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        assertEquals("image/geotiff", response.getContentType());
        assertEquals("inline; filename=sf-states.tif", response.getHeader("Content-Disposition"));
    }

    public void testSldBody() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    public void testSldBody11() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD11.replaceAll("=", "%3D"));
        checkImage(response);
    }
    
    public void testSldBodyNoVersion() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox + "&styles="
                + "&layers=" + layers + "&Format=image/png" + "&request=GetMap" + "&width=550"
                + "&height=250" + "&srs=EPSG:4326" + "&SLD_BODY="
                + STATES_SLD.replace(" version=\"1.1.0\"", "").replaceAll("=", "%3D"));
        checkImage(response);
    }

    public void testSldBodyPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?bbox=" + bbox
                + "&format=image/png&request=GetMap&width=550&height=250" + "&srs=EPSG:4326",
                STATES_SLD);

        checkImage(response);
    }
    
    public void testSldBodyPost11() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?bbox=" + bbox
                + "&format=image/png&request=GetMap&width=550&height=250" + "&srs=EPSG:4326",
                STATES_SLD11);

        checkImage(response);
    }

    public void testXmlPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wms?", STATES_GETMAP);
        checkImage(response);
    }

    public void testRemoteOWSGet() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER))
            return;

        ServletResponse response = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format=image/png"
                + "&layers="
                + RemoteOWSTestSupport.TOPP_STATES
                + ","
                + MockData.BASIC_POLYGONS.getPrefix()
                + ":"
                + MockData.BASIC_POLYGONS.getLocalPart()
                + "&styles=Population,"
                + MockData.BASIC_POLYGONS.getLocalPart()
                + "&remote_ows_type=WFS"
                + "&remote_ows_url="
                + RemoteOWSTestSupport.WFS_SERVER_URL
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("image/png", response.getContentType());
    }

    public void testRemoteOWSUserStyleGet() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) {
            return;
        }

        URL url = GetMapIntegrationTest.class.getResource("remoteOws.sld");

        ServletResponse response = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format=image/png"
                + "&sld="
                + url.toString()
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("image/png", response.getContentType());
    }

    public void testWorkspaceQualified() throws Exception {

        Document doc = getAsDOM("cite/wms?request=getmap&service=wms"
                + "&layers=PrimitiveGeoFeature&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90", true);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());

        ServletResponse response = getAsServletResponse("cite/wms?request=getmap&service=wms"
                + "&layers=Lakes&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90");
        assertEquals("image/png", response.getContentType());
    }

    public void testLayerQualified() throws Exception {
        Document doc = getAsDOM("cite/Ponds/wms?request=getmap&service=wms"
                + "&layers=Forests&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90", true);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());

        ServletResponse response = getAsServletResponse("cite/Ponds/wms?request=getmap&service=wms"
                + "&layers=Ponds&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-180,-90,180,90");
        assertEquals("image/png", response.getContentType());
    }
    
    public void testGroupWorkspaceQualified() throws Exception {
        // check the group works without workspace qualification
        String url = "wms?request=getmap&service=wms"
                + "&layers=nature&width=100&height=100&format=image/png"
                + "&srs=epsg:4326&bbox=-0.002,-0.003,0.005,0.002";
        ServletResponse response = getAsServletResponse(url);
        assertEquals("image/png", response.getContentType());
        
        // see that it works also with workspace qualification
        response = getAsServletResponse("cite/" + url);
        assertEquals("image/png", response.getContentType());
    }
    
    public void testEnvDefault() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=parametric&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326");
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));

        int[] rgba = new int[3];
        // fully black pixel in the middle of the map
        image.getData().getPixel(250, 125, rgba);
        assertEquals(0, rgba[0]);
        assertEquals(0, rgba[1]);
        assertEquals(0, rgba[2]);
    }
    
    public void testEnvRed() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=" + bbox
                + "&styles=parametric&layers=" + layers + "&Format=image/png" + "&request=GetMap"
                + "&width=550" + "&height=250" + "&srs=EPSG:4326&env=color:0xFF0000");
        assertEquals("image/png", response.getContentType());
        
        RenderedImage image = ImageIO.read(getBinaryInputStream(response));

        int[] rgba = new int[3];
        // fully red pixel in the middle of the map
        image.getData().getPixel(250, 125, rgba);
        assertEquals(255, rgba[0]);
        assertEquals(0, rgba[1]);
        assertEquals(0, rgba[2]);
    }

}
