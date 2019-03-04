/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.io.IOException;
import org.geoserver.rest.security.xml.JaxbUser;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.xml.XMLUserGroupService;
import org.junit.Test;

/**
 * Unit tests designed in line with issue reported at
 * https://osgeo-org.atlassian.net/browse/GEOS-8486
 *
 * @author ImranR
 */
public class XStreamParserTest extends SecurityRESTTestSupport {

    private final String restSecurityUserGroupUsersUrl = "/rest/security/usergroup/users";

    private final String xmlBody =
            "<user><userName>Jim</userName><password>password</password><enabled>true</enabled></user>";

    private final String jsonBody =
            "{\"user\":{\"userName\":\"mr_json\",\"password\":\"pass_me_a_json\",\"enabled\":true}}";

    @Test
    public void jsonReadWriteTest() throws Exception, IOException {
        // basic marshalling / unmarshalling
        XStream xstream = new XStream(new JettisonMappedXmlDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("user", JaxbUser.class);

        JaxbUser user = new JaxbUser();
        user.setUserName("test");
        user.setPassword("pass");
        user.setEnabled(true);

        String jsonString = xstream.toXML(user);
        assertNotNull(jsonString);

        JaxbUser parseUser = (JaxbUser) xstream.fromXML(jsonBody);

        assertNotNull(parseUser);
    }

    @Test
    public void postUserXMLTest() throws Exception, IOException {

        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        // 201 means resources has been successfully created
        assertEquals(
                201,
                postAsServletResponse(restSecurityUserGroupUsersUrl, xmlBody, "application/xml")
                        .getStatus());

        GeoServerUser user = service.getUserByUsername("Jim");
        // check if user exists
        assertNotNull(user);

        logout();
    }

    @Test
    public void postUserJSONTest() throws Exception, IOException {
        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        // 201 means resources has been successfully created
        assertEquals(
                201,
                postAsServletResponse(restSecurityUserGroupUsersUrl, jsonBody, "application/json")
                        .getStatus());

        GeoServerUser user = service.getUserByUsername("mr_json");
        // check if user exists
        assertNotNull(user);

        logout();
    }
}
