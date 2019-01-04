/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SecuredGetFeatureTest extends WFSTestSupport {

    public static QName NULL_GEOMETRIES =
            new QName(SystemTestData.CITE_URI, "NullGeometries", SystemTestData.CITE_PREFIX);

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {

        addUser("cite", "cite", null, Collections.singletonList("ROLE_CITE_READER"));

        addLayerAccessRule("*", "*", AccessMode.READ, "ROLE_NO_ONE");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "ROLE_NO_ONE");
        addLayerAccessRule(SystemTestData.CITE_PREFIX, "*", AccessMode.READ, "ROLE_CITE_READER");
    }

    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testGetNoAuthHide() throws Exception {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.HIDE);

        // no auth, hide mode, we should get an error stating the layer is not there
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        checkOws10Exception(doc);
        assertXpathEvaluatesTo("Unknown namespace [cite]", "//ows:ExceptionText/text()", doc);
    }

    @Test
    public void testGetNoAuthChallenge() throws Exception {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        // this test seems to fail on the build server without storing the rules...
        dao.storeRules();

        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        assertEquals(401, resp.getStatus());
        assertEquals("Basic realm=\"GeoServer Realm\"", resp.getHeader("WWW-Authenticate"));
    }

    @Test
    public void testInvalidAuthChallenge() throws Exception {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        MockHttpServletRequest request =
                createRequest(
                        "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        request.setMethod("GET");
        request.addHeader(
                "Authorization",
                "Basic " + new String(Base64.encodeBase64("cite:wrongpassword".getBytes())));

        MockHttpServletResponse resp = dispatch(request);
        assertEquals(401, resp.getStatus());
        assertEquals("Basic realm=\"GeoServer Realm\"", resp.getHeader("WWW-Authenticate"));
    }

    @Test
    public void testValidAuth() throws Exception {
        checkValidAuth("cite", "cite");
    }

    @Test
    public void testValidAuthAdmin() throws Exception {
        checkValidAuth("admin", "geoserver");
    }

    private void checkValidAuth(String username, String password)
            throws Exception, ParserConfigurationException, SAXException, IOException,
                    XpathException {
        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        setRequestAuth(username, password);
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.0.0&service=wfs&typeName="
                                + getLayerId(SystemTestData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", doc);
    }
}
