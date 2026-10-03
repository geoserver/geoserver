/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;

/** End-to-end XML marshalling tests for the Role Service REST API. */
public class RoleServiceControllerMarshallingTest extends GeoServerSystemTestSupport {

    private static final String BASE = RestBaseController.ROOT_PATH + "/security/roleservices";
    private static final String CT_XML = "application/xml";

    private String createdServiceName;

    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        super.loginAsAdmin();
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (createdServiceName != null) {
                SecurityRoleServiceConfig config = getSecurityManager().loadRoleServiceConfig(createdServiceName);
                if (config != null) {
                    getSecurityManager().removeRoleService(config);
                }
            }
        } finally {
            createdServiceName = null;
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testPostCreatesXmlRoleServiceViaRest() throws Exception {
        String serviceName = newServiceName();
        createdServiceName = serviceName;

        MockHttpServletResponse post = doPost(BASE, xmlRoleService(serviceName), CT_XML, CT_XML);
        assertEquals(201, post.getStatus());

        SecurityRoleServiceConfig config = getSecurityManager().loadRoleServiceConfig(serviceName);
        assertNotNull(config);
        assertTrue(config instanceof XMLRoleServiceConfig);
        assertEquals(XMLRoleService.class.getName(), config.getClassName());
        assertEquals(serviceName + ".xml", ((XMLRoleServiceConfig) config).getFileName());

        MockHttpServletResponse view = doGet(BASE + "/" + serviceName, CT_XML);
        assertEquals(200, view.getStatus());

        Document dom;
        try (ByteArrayInputStream in = new ByteArrayInputStream(view.getContentAsByteArray())) {
            dom = dom(in);
        }
        assertXpathEvaluatesTo(serviceName, "/org.geoserver.security.xml.XMLRoleServiceConfig/name", dom);
        assertXpathEvaluatesTo(
                XMLRoleService.class.getName(), "/org.geoserver.security.xml.XMLRoleServiceConfig/className", dom);
        assertXpathEvaluatesTo(serviceName + ".xml", "/org.geoserver.security.xml.XMLRoleServiceConfig/fileName", dom);
    }

    private static String newServiceName() {
        return "rs-xml-" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String xmlRoleService(String name) {
        return "<org.geoserver.security.xml.XMLRoleServiceConfig>"
                + "<name>" + name + "</name>"
                + "<className>" + XMLRoleService.class.getName() + "</className>"
                + "<fileName>" + name + ".xml</fileName>"
                + "<checkInterval>0</checkInterval>"
                + "<validating>false</validating>"
                + "</org.geoserver.security.xml.XMLRoleServiceConfig>";
    }

    private MockHttpServletResponse doGet(String path, String accept) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("GET");
        request.addHeader("Accept", accept);
        return dispatch(request);
    }

    private MockHttpServletResponse doPost(String path, String body, String contentType, String accept)
            throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType(contentType);
        request.addHeader("Accept", accept);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return dispatch(request);
    }
}
