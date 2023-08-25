/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.J2eeRoleServiceConfig;
import org.geoserver.util.IOUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GeoServerJ2eeRoleServiceTest extends AbstractSecurityServiceTest {

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        J2eeRoleServiceConfig config = new J2eeRoleServiceConfig();
        config.setName(name);
        config.setClassName(GeoServerJ2eeRoleService.class.getName());
        getSecurityManager().saveRoleService(config);
        // System.out.println(getDataDirectory().root().getAbsoluteFile());
        return null;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        createRoleService("test1");
        createRoleService("test2");
    }

    @Test
    public void testNoRoles() throws Exception {
        assertIsValid("web1.xml");
        copyWebXML("web1.xml");
        GeoServerRoleService service = getSecurityManager().loadRoleService("test1");
        checkEmpty(service);
    }

    @Test
    public void testRoles() throws Exception {
        assertIsValid("web2.xml");
        copyWebXML("web2.xml");
        GeoServerRoleService service = getSecurityManager().loadRoleService("test2");
        assertEquals(4, service.getRoleCount());
        assertTrue(service.getRoles().contains(new GeoServerRole("role1")));
        assertTrue(service.getRoles().contains(new GeoServerRole("role2")));
        assertTrue(service.getRoles().contains(new GeoServerRole("employee")));
        assertTrue(service.getRoles().contains(new GeoServerRole("MGR")));
    }

    protected void copyWebXML(String name) throws IOException {
        File dataDir = getDataDirectory().root();
        File to = new File(dataDir, "WEB-INF");
        to = new File(to, "web.xml");
        IOUtils.copy(getClass().getResourceAsStream(name), to);
    }

    protected void assertIsValid(String name) throws IOException, SAXException {
        // makes sure web.xml is XSD compliant (without requiring internet access in the process)
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema =
                factory.newSchema(
                        new Source[] {
                            new StreamSource(
                                    new File(
                                            "src/test/resources/org/geoserver/security/xsd/xml.xsd")),
                            new StreamSource(
                                    new File(
                                            "src/test/resources/org/geoserver/security/xsd/web-app_3_1.xsd")),
                            new StreamSource(
                                    new File(
                                            "src/test/resources/org/geoserver/security/xsd/web-common_3_1.xsd")),
                            new StreamSource(
                                    new File(
                                            "src/test/resources/org/geoserver/security/xsd/javaee_7.xsd")),
                            new StreamSource(
                                    new File(
                                            "src/test/resources/org/geoserver/security/xsd/javaee_web_services_client_1_4.xsd")),
                            new StreamSource(
                                    new File(
                                            "src/test/resources/org/geoserver/security/xsd/jsp_2_3.xsd"))
                        });
        Validator validator = schema.newValidator();
        validator.validate(
                new StreamSource(
                        new File("src/test/resources/org/geoserver/security/impl/" + name)));
    }
}
