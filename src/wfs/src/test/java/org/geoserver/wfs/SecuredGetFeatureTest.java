package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.Filter;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class SecuredGetFeatureTest extends WFSTestSupport {
	
    public static QName NULL_GEOMETRIES = new QName(MockData.CITE_URI, "NullGeometries", MockData.CITE_PREFIX);
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();
        
        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,ROLE_CITE_READER");
        props.store(new FileOutputStream(users), "");
        
        File layers = new File(security, "layers.properties");
        props.put("*.*.r", "ROLE_NO_ONE");
        props.put("*.*.w", "ROLE_NO_ONE");
        props.put(MockData.CITE_PREFIX, "cite,ROLE_CITE_READER");
        props.put("cite.*.r", "cite,ROLE_CITE_READER");
        props.store(new FileOutputStream(layers), "");
    }
    
    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }
    
    
    public void testGetNoAuthHide() throws Exception {
        DataAccessRuleDAO dao = GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.HIDE);
        
        // no auth, hide mode, we should get an error stating the layer is not there
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&typeName=" + getLayerId(MockData.BUILDINGS));
        // print(doc);
        checkOws10Exception(doc);
        assertXpathEvaluatesTo("Unknown namespace [cite]", "//ows:ExceptionText/text()", doc);
    }
    
    public void testGetNoAuthChallenge() throws Exception {
        DataAccessRuleDAO dao = GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        //this test seems to fail on the build server without storing the rules...
        dao.storeRules();

        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(MockData.BUILDINGS));
        assertEquals(401, resp.getErrorCode());
        assertEquals("Basic realm=\"GeoServer Realm\"", resp.getHeader("WWW-Authenticate"));
    }
    
    public void testInvalidAuthChallenge() throws Exception {
        DataAccessRuleDAO dao = GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);
        
        MockHttpServletRequest request = createRequest("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(MockData.BUILDINGS));
        request.addHeader("Authorization",  "Basic " + new String(Base64.encodeBase64("cite:wrongpassword".getBytes())));

        MockHttpServletResponse resp = dispatch(request);
        assertEquals(401, resp.getErrorCode());
        assertEquals("Basic realm=\"GeoServer Realm\"", resp.getHeader("WWW-Authenticate"));
    }
    
    public void testValidAuth() throws Exception {
        checkValidAuth("cite", "cite");
    }
    
    public void testValidAuthAdmin() throws Exception {
        checkValidAuth("admin", "geoserver");
    }

    private void checkValidAuth(String username, String password) throws Exception,
            ParserConfigurationException, SAXException, IOException, XpathException {
        DataAccessRuleDAO dao = GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);
        
        authenticate(username, password);
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.0.0&service=wfs&typeName=" + getLayerId(MockData.BUILDINGS));
        // print(doc);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", doc);
    }
    
    

}
