/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import jakarta.servlet.Filter;
import java.util.Collections;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class SecuredControllerAccessTest extends GeoServerSystemTestSupport {

    @Override
    protected List<Filter> getFilters() {
        // enable spring security
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Before
    public void setRequestCredentials() throws Exception {
        setRequestAuth("admin", "geoserver");
    }

    @Test
    public void testGetStatusAuthorized() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/about/status.xml");
        assertEquals(200, response.getStatus());
        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("1", "count(//status[name='GeoServer Main'])", dom);
    }

    @Test
    public void testGetStatusNotAuthorized() throws Exception {
        // reset authorization
        setRequestAuth(null, null);
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/about/status.xml");
        assertEquals(401, response.getStatus());
    }

    @Test
    public void testHeadStatus() throws Exception {
        // HEAD has been added in the REST security config for all tests
        MockHttpServletRequest request = createRequest(RestBaseController.ROOT_PATH + "/about/status.xml");
        request.setMethod("HEAD");
        MockHttpServletResponse response = dispatch(request, null);
        // used to be 403
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testOptionsStatus() throws Exception {
        // OPTIONS has been added in the REST security config for all tests
        MockHttpServletRequest request = createRequest(RestBaseController.ROOT_PATH + "/about/status.xml");
        request.setMethod("OPTIONS");
        MockHttpServletResponse response = dispatch(request, null);
        // used to be 403
        assertEquals(200, response.getStatus());
    }
}
