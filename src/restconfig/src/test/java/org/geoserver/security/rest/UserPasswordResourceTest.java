/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.io.IOException;
import java.text.MessageFormat;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerSystemTestSupport;

import org.restlet.data.Status;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for {@link UserPasswordResource}
 *
 * @author Emanuele Tajariol <etj at geo-solutions.it>
 */
public class UserPasswordResourceTest extends GeoServerSystemTestSupport {

    final static String UP_URI = "/rest/security/self/password";

    final static String USERNAME = "restuser";
    final static String USERPW = "restpassword";


    protected static XpathEngine xp;

    String xmlTemplate=
         "<"+UserPasswordResource.XML_ROOT_ELEM+">"+
           "<"+UserPasswordResource.UP_NEW_PW+">{0}</"+UserPasswordResource.UP_NEW_PW+">" +
          "</"+UserPasswordResource.XML_ROOT_ELEM+">";

    String xmlBadTemplate=
         "<"+UserPasswordResource.XML_ROOT_ELEM+">"+
           "<not_the_right_element>{0}</not_the_right_element>" +
          "</"+UserPasswordResource.XML_ROOT_ELEM+">";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // Create the test restuser if needed
        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(
                XMLUserGroupService.DEFAULT_NAME);

        if (service.getUserByUsername(USERNAME)==null) {
            GeoServerUser user = service.createUserObject(USERNAME, USERPW, true);
            GeoServerUserGroupStore store = service.createStore();

            store.addUser(user);
            store.store();
            service.load();
        }

        xp = XMLUnit.newXpathEngine();
    }


    public void resetUserPassword() throws IOException, PasswordPolicyException {
        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(
                XMLUserGroupService.DEFAULT_NAME);

        GeoServerUser user = service.getUserByUsername(USERNAME);
        user.setPassword(USERPW);

        GeoServerUserGroupStore store = service.createStore();
        store.updateUser(user);
        store.store();
        service.load();
    }


    public void login() throws Exception {
        resetUserPassword();

        login(USERNAME, USERPW, "ROLE_AUTHENTICATED");
    }

    @Test
    public void testGetAsAuthorized() throws Exception {
        login();

        assertEquals( Status.CLIENT_ERROR_METHOD_NOT_ALLOWED.getCode(),
                      getAsServletResponse(UP_URI).getStatus() );
    }

    @Test
    public void testGetAsNotAuthorized() throws Exception {
        logout();

        assertEquals( Status.CLIENT_ERROR_METHOD_NOT_ALLOWED.getCode(),
                      getAsServletResponse(UP_URI).getStatus() );
    }

    @Test
    public void testPutUnauthorized() throws Exception {
        logout();

        String body = MessageFormat.format(xmlTemplate, "new01");
        assertEquals( 405, putAsServletResponse(UP_URI, body, "text/xml").getStatus() );
    }

    @Test
    public void testPutInvalidNewPassword() throws Exception {
        login();

        String body = MessageFormat.format(xmlTemplate, "   ");
        assertEquals( Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                      putAsServletResponse(UP_URI, body, "text/xml").getStatus() );
    }

    @Test
    public void testPutInvalidElement() throws Exception {
        login();

        String body = MessageFormat.format(xmlBadTemplate, "newpw42");
        assertEquals( Status.CLIENT_ERROR_BAD_REQUEST.getCode(),
                      putAsServletResponse(UP_URI, body, "text/xml").getStatus() );
    }

    @Test
    public void testPutAsXML() throws Exception {
        login();

        String body = MessageFormat.format(xmlTemplate, "pw01");
        assertEquals( 200, putAsServletResponse(UP_URI,body,"text/xml").getStatus() );
    }

    @Test
    public void checkUpdatedPassword() throws Exception {
        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(
                XMLUserGroupService.DEFAULT_NAME);

        GeoServerUser user;

        login();

        // store proper starting encoding
        user = service.getUserByUsername(USERNAME);
        String originalPw = user.getPassword();

        String body = MessageFormat.format(xmlTemplate, "pw01");
        assertEquals( 200, putAsServletResponse(UP_URI,body,"text/xml").getStatus() );

        // check pw has been updated
        service.load();
        user = service.getUserByUsername(USERNAME);
        String pw1 = user.getPassword();
        assertNotEquals(originalPw, pw1);

        body = MessageFormat.format(xmlTemplate, "pw02");
        assertEquals( 200, putAsServletResponse(UP_URI,body,"text/xml").getStatus() );

        // check pw has been updated
        service.load();
        user = service.getUserByUsername(USERNAME);
        String pw2 = user.getPassword();
        assertNotEquals(originalPw, pw2);
        assertNotEquals(pw1, pw2);
    }

}
