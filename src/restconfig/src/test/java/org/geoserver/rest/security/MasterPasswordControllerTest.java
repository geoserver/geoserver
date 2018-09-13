/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.MessageFormat;
import net.sf.json.JSONObject;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for {@link MasterPasswordController}
 *
 * @author christian
 */
public class MasterPasswordControllerTest extends SecurityRESTTestSupport {

    static final String MP_URI_JSON = RestBaseController.ROOT_PATH + "/security/masterpw.json";

    static final String MP_URI_XML = RestBaseController.ROOT_PATH + "/security/masterpw.xml";

    String xmlTemplate =
            "<"
                    + MasterPasswordController.XML_ROOT_ELEM
                    + ">"
                    + "<"
                    + MasterPasswordController.MP_CURRENT_KEY
                    + ">{0}</"
                    + MasterPasswordController.MP_CURRENT_KEY
                    + ">"
                    + "<"
                    + MasterPasswordController.MP_NEW_KEY
                    + ">{1}</"
                    + MasterPasswordController.MP_NEW_KEY
                    + ">"
                    + "</"
                    + MasterPasswordController.XML_ROOT_ELEM
                    + ">";

    String jsonTemplate =
            "{\""
                    + MasterPasswordController.MP_CURRENT_KEY
                    + "\":\"%s\","
                    + "\""
                    + MasterPasswordController.MP_NEW_KEY
                    + "\":\"%s\"}";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // We need to enable Master Root login first
        MasterPasswordProviderConfig masterPasswordConfig =
                getSecurityManager()
                        .loadMasterPassswordProviderConfig(
                                getSecurityManager().getMasterPasswordConfig().getProviderName());
        masterPasswordConfig.setLoginEnabled(true);
        getSecurityManager().saveMasterPasswordProviderConfig(masterPasswordConfig);
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(MP_URI_XML, 200);
        assertEquals(
                MasterPasswordController.XML_ROOT_ELEM, dom.getDocumentElement().getNodeName());
        assertEquals(
                "geoserver",
                xp.evaluate(
                        "/"
                                + MasterPasswordController.XML_ROOT_ELEM
                                + "/"
                                + MasterPasswordController.MP_CURRENT_KEY,
                        dom));
    }

    @Test
    public void testGetAsXMLNotAuthorized() throws Exception {
        logout();
        assertEquals(403, getAsServletResponse(MP_URI_XML).getStatus());
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(MP_URI_JSON);
        String password = (String) json.get(MasterPasswordController.MP_CURRENT_KEY);
        assertEquals("geoserver", password);
    }

    @Test
    public void testUnallowedMethod() throws Exception {
        boolean failed = false;
        try {
            getSecurityManager().getMasterPasswordForREST();
        } catch (IOException ex) {
            failed = true;
        }
        assertTrue(failed);
    }

    @Test
    public void testPutUnauthorized() throws Exception {
        logout();
        String body = MessageFormat.format(xmlTemplate, "geoserver", "abc");
        assertEquals(405, putAsServletResponse(MP_URI_XML, body, "text/xml").getStatus());
    }

    @Test
    public void testPutInvalidNewPassword() throws Exception {
        String body = MessageFormat.format(xmlTemplate, "geoserver", "abc");
        assertEquals(422, putAsServletResponse(MP_URI_XML, body, "text/xml").getStatus());
    }

    @Test
    public void testPutInvalidCurrentPassword() throws Exception {
        String body = MessageFormat.format(xmlTemplate, "geoserverXY", "geoserver1");
        assertEquals(422, putAsServletResponse(MP_URI_XML, body, "text/xml").getStatus());
    }

    @Test
    public void testPutAsXML() throws Exception {

        String body = MessageFormat.format(xmlTemplate, "geoserver", "geoserver1");
        assertEquals(200, putAsServletResponse(MP_URI_XML, body, "text/xml").getStatus());
        assertTrue(getSecurityManager().checkMasterPassword("geoserver1"));

        body = MessageFormat.format(xmlTemplate, "geoserver1", "geoserver");
        assertEquals(200, putAsServletResponse(MP_URI_XML, body, "text/xml").getStatus());
        assertTrue(getSecurityManager().checkMasterPassword("geoserver"));
    }

    @Test
    public void testPutAsJSON() throws Exception {

        String body = String.format(jsonTemplate, "geoserver", "geoserver1");
        assertEquals(200, putAsServletResponse(MP_URI_JSON, body, "text/json").getStatus());
        assertTrue(getSecurityManager().checkMasterPassword("geoserver1"));

        body = String.format(jsonTemplate, "geoserver1", "geoserver");
        assertEquals(200, putAsServletResponse(MP_URI_JSON, body, "text/json").getStatus());
        assertTrue(getSecurityManager().checkMasterPassword("geoserver"));
    }
}
