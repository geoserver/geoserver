/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.data.test.MockData;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.utils.RuleReaderServiceAdapter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GeofenceAccessManagerVirtualServiceTest extends GeoServerSystemTestSupport {

    RuleReaderService CUSTOM_RULE_SERVICE;

    @Before
    public void setUp() {

        CUSTOM_RULE_SERVICE = new RuleReaderServiceAdapter() {
            @Override
            public AccessInfo getAccessInfo(RuleFilter filter) {
                if ("WFS".equalsIgnoreCase(filter.getService().getText())
                        && MockData.BRIDGES
                                .getLocalPart()
                                .equals(filter.getLayer().getText())) return new AccessInfo(GrantType.ALLOW);
                if ("WMS".equalsIgnoreCase(filter.getService().getText())
                        && MockData.BUILDINGS
                                .getLocalPart()
                                .equals(filter.getLayer().getText())) return new AccessInfo(GrantType.ALLOW);
                return new AccessInfo(GrantType.DENY);
            }
        };
    }

    MockHttpServletResponse getGetMapResponse(QName qlayer, boolean layerInContext) throws Exception {
        GeofenceAccessManager gf = GeoServerExtensions.bean(GeofenceAccessManager.class);
        gf.rulesService = CUSTOM_RULE_SERVICE;

        // Ensure workspace/layer exist in the test data
        // Skipping check bc the call goes through all the security stack
        //        Catalog catalog = getCatalog();
        //        assertNotNull(catalog.getLayerByName(getLayerId(qlayer)));

        // Build the virtual layer or virtual workspace path
        String context = qlayer.getPrefix();
        if (layerInContext) {
            context = context + "/" + qlayer.getLocalPart();
        }

        MockHttpServletResponse response =
                getAsServletResponse(context + "/ows?service=WMS&version=1.1.0&request=GetMap"
                        + "&layers=" + getLayerId(qlayer)
                        + "&styles="
                        + "&bbox=-180,-90,180,90"
                        + "&width=256&height=256&srs=EPSG:4326&format=image/png");

        return response;
    }

    Document getGetCapabilitiesResponse(QName qlayer, String service, boolean layerInContext) throws Exception {
        GeofenceAccessManager gf = GeoServerExtensions.bean(GeofenceAccessManager.class);
        gf.rulesService = CUSTOM_RULE_SERVICE;

        // Build the virtual layer or virtual workspace path
        String context = qlayer.getPrefix();
        if (layerInContext) {
            context = context + "/" + qlayer.getLocalPart();
        }

        String version = null;
        if ("WMS".equalsIgnoreCase(service)) version = "1.1.0";
        else if ("WFS".equalsIgnoreCase(service)) version = "2.0.0";
        else fail("Bad service " + service);

        return getAsDOM(context + "/ows?service=" + service + "&version=" + version + "&request=GetCapabilities");
    }

    @Test
    public void testVirtualLayerGetMapAllowed() throws Exception {
        MockHttpServletResponse response = getGetMapResponse(MockData.BUILDINGS, true);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    @Test
    public void testVirtualLayerGetMapDenied() throws Exception {
        MockHttpServletResponse response = getGetMapResponse(MockData.BRIDGES, true);

        assertEquals(200, response.getStatus());
        assertEquals("application/vnd.ogc.se_xml;charset=UTF-8", response.getContentType());
        String content = response.getContentAsString();
        MatcherAssert.assertThat(content, CoreMatchers.containsString("LayerNotDefined"));
    }

    @Test
    public void testVirtualWSGetMapAllowed() throws Exception {
        MockHttpServletResponse response = getGetMapResponse(MockData.BUILDINGS, false);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertTrue(response.getContentAsByteArray().length > 0);
    }

    @Test
    public void testVirtualWSGetMapDenied() throws Exception {
        MockHttpServletResponse response = getGetMapResponse(MockData.BRIDGES, false);

        assertEquals(200, response.getStatus());
        assertEquals("application/vnd.ogc.se_xml;charset=UTF-8", response.getContentType());
        String content = response.getContentAsString();
        MatcherAssert.assertThat(content, CoreMatchers.containsString("LayerNotDefined"));
    }

    @Test
    public void testVirtualLayerGetCapabilitiesWMSEnabled() throws Exception {
        // BUILDINGS has ALLOW grants for WMS
        Document capa = getGetCapabilitiesResponse(MockData.BUILDINGS, "WMS", true);
        // System.err.println(prettyPrint(capa));

        // request made on this virtual layer which is enabled for WMS
        assertXpathEvaluatesTo("1", "count(//Layer[Name/text()='Buildings'])", capa);
        // request made on other virtual layer, and bridges is disabled for WMS
        assertXpathEvaluatesTo("0", "count(//Layer[Name/text()='Bridges'])", capa);
    }

    @Test
    public void testVirtualLayerGetCapabilitiesWMSDisabled() throws Exception {
        // BRIDGES has DENY grants for WMS
        Document capa = getGetCapabilitiesResponse(MockData.BRIDGES, "WMS", true);
        //        System.err.println(prettyPrint(capa));

        // request made on other virtual layer, and this is enabled for WMS
        assertXpathEvaluatesTo("0", "count(//Layer[Name/text()='Buildings'])", capa);
        // request made on this virtual layer which is disabled for WMS
        assertXpathEvaluatesTo("0", "count(//Layer[Name/text()='Bridges'])", capa);
    }

    @Test
    public void testVirtualWSGetCapabilitiesWMS() throws Exception {
        // getting the capa for the whole WS
        Document capa = getGetCapabilitiesResponse(MockData.BRIDGES, "WMS", false);

        assertXpathEvaluatesTo("1", "count(//Layer[Name/text()='Buildings'])", capa);
        assertXpathEvaluatesTo("0", "count(//Layer[Name/text()='Bridges'])", capa);
    }

    private String getWFSCountXpath(QName layer) {
        return "count(//*[local-name()='FeatureType']/*[local-name()='Name' and text()='" + getLayerId(layer) + "'])";
    }

    @Test
    public void testVirtualLayerGetCapabilitiesWFSDisabled() throws Exception {
        // BUILDINGS has DENY grants for WFS
        Document capa = getGetCapabilitiesResponse(MockData.BUILDINGS, "WFS", true);

        // request made on this virtual layer which is disabled for WFS
        assertXpathEvaluatesTo("0", getWFSCountXpath(MockData.BUILDINGS), capa);
        // request made on other virtual layer, and bridges is enabled for WFS
        assertXpathEvaluatesTo("0", getWFSCountXpath(MockData.BRIDGES), capa);
    }

    @Test
    public void testVirtualLayerGetCapabilitiesWFSEnabled() throws Exception {
        // BRIDGES has ALLOW grants for WFS
        Document capa = getGetCapabilitiesResponse(MockData.BRIDGES, "WFS", true);
        //        System.err.println(prettyPrint(capa));

        // request made on other virtual layer, and this is disabled for WFS
        assertXpathEvaluatesTo("0", getWFSCountXpath(MockData.BUILDINGS), capa);
        // request made on this virtual layer which is enabled for WFS
        assertXpathEvaluatesTo("1", getWFSCountXpath(MockData.BRIDGES), capa);
    }

    @Test
    public void testVirtualWSGetCapabilitiesWFS() throws Exception {
        // getting the capa for the whole WS
        Document capa = getGetCapabilitiesResponse(MockData.BRIDGES, "WFS", false);

        assertXpathEvaluatesTo("0", getWFSCountXpath(MockData.BUILDINGS), capa);
        assertXpathEvaluatesTo("1", getWFSCountXpath(MockData.BRIDGES), capa);
    }

    public static String prettyPrint(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String ret = writer.toString();
        ret = ret.replaceAll("(?m)^.*<CRS>.*?</CRS>.*\\R?", "");
        ret = ret.replaceAll("(?m)^.*<SRS>.*?</SRS>.*\\R?", "");
        ret = ret.replaceAll("(?m)^\\s*$\\R?", "");
        return ret;
    }
}
