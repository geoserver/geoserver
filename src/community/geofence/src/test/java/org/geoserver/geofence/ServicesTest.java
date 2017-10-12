package org.geoserver.geofence;

import java.util.Collections;
import java.util.List;

import org.springframework.mock.web.MockHttpServletResponse;

import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.w3c.dom.Document;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import org.junit.Test;

public class ServicesTest extends GeofenceBaseTest {

    /**
     * Enable the Spring Security auth filters, otherwise there will be no auth
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections
                .singletonList((javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testAdmin() throws Exception {
        authenticate("admin", "geoserver");

        // check from the caps he can access everything
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1&service=WMS");
        // print(dom);

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("8", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCiteCapabilities() throws Exception {
        authenticate("cite", "cite");

        // check from the caps he can access cite and sf, but not others
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1&service=wms");
        // print(dom);

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCiteLayers() throws Exception {
        authenticate("cite", "cite");

        // try a getmap/reflector on a sf layer, should work
        MockHttpServletResponse response = getAsServletResponse(
                "wms/reflect?layers=" + getLayerId(MockData.BASIC_POLYGONS));
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());

        // try a getmap/reflector on a sf layer, should work
        response = getAsServletResponse("wms/reflect?layers=" + getLayerId(MockData.GENERICENTITY));
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());

        // try a getfeature on a sf layer
        response = getAsServletResponse("wfs?service=wfs&version=1.0.0&request=getfeature&typeName="
                + getLayerId(MockData.GENERICENTITY));
        assertEquals(200, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        String content = response.getContentAsString();
        LOGGER.info("Content: " + content);
        // assertTrue(content.contains("Unknown namespace [sf]"));
        assertTrue(content.contains("Feature type sf:GenericEntity unknown"));
    }
}
