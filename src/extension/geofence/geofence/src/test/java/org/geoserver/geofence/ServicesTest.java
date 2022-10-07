/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ServicesTest extends GeofenceBaseTest {

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    /** Enable the Spring Security auth filters, otherwise there will be no auth */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testAdmin() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        this.username = "admin";
        this.password = "geoserver";

        // check from the caps he can access everything
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1&service=WMS");
        // print(dom);

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("8", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCiteCapabilities() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        //        loginAsCite();
        this.username = "cite";
        this.password = "cite";

        // check from the caps he can access cite and sf, but not others
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1&service=WMS");
        print(dom);

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCiteLayers() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        loginAsCite();

        // try a getfeature on a sf layer
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=wfs&version=1.0.0&request=getfeature&typeName="
                                + getLayerId(MockData.GENERICENTITY));
        assertEquals(200, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        String content = response.getContentAsString();
        LOGGER.info("Content: " + content);
        //        assertTrue(content.contains("Unknown namespace [sf]"));
        assertTrue(content.contains("Feature type sf:GenericEntity unknown"));
    }
}
