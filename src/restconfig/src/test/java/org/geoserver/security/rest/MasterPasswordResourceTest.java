/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.text.MessageFormat;

import net.sf.json.JSONObject;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for {@link MasterPasswordResource}
 * 
 * @author christian
 *
 */
public class MasterPasswordResourceTest extends SecurityRESTTestSupport {

    final static String MP_URI_JSON="/rest/security/masterpw.json";
    final static String MP_URI_XML="/rest/security/masterpw.xml";
    
    String xmlTemplate=
         "<root>"+
           "<"+MasterPasswordResource.MP_CURRENT_KEY+">{0}</"+MasterPasswordResource.MP_CURRENT_KEY+">" +
           "<"+MasterPasswordResource.MP_NEW_KEY+">{1}</"+MasterPasswordResource.MP_NEW_KEY+">" +                 
          "</root>";
    
    String jsonTemplate=
            "{\""+MasterPasswordResource.MP_CURRENT_KEY+"\":\"%s\"," +
            "\""+MasterPasswordResource.MP_NEW_KEY+"\":\"%s\"}";
     
    @Test
    public void testGetAsXML() throws Exception {        
        Document dom = getAsDOM( MP_URI_XML);
        assertEquals( "root", dom.getDocumentElement().getNodeName() );
        assertEquals( "geoserver", xp.evaluate( "/root/"+MasterPasswordResource.MP_CURRENT_KEY, dom) );
    }
    
    @Test
    public void testGetAsXMLNotAuthorized() throws Exception {  
        logout();
        assertEquals( 403, getAsServletResponse(MP_URI_XML).getStatusCode() );
    }
    
    @Test
    public void testGetAsJSON() throws Exception {
        JSONObject json = (JSONObject) getAsJSON( MP_URI_JSON );
        String password = (String) json.get( MasterPasswordResource.MP_CURRENT_KEY);                
        assertEquals( "geoserver", password );
    }    
    @Test
    public void testUnallowedMethod() throws Exception{
        boolean failed = false;
        try {
            getSecurityManager().getMasterPasswordForREST();
        } catch (IOException ex) {
            failed=true;
        }
        assertTrue(failed);        
    }    
    @Test
    public void testPutUnauthorized() throws Exception {
        logout();
        String body = MessageFormat.format(xmlTemplate, "geoserver", "abc");
        assertEquals( 405, putAsServletResponse(MP_URI_XML,body,"text/xml").getStatusCode() );
    }
    
    @Test
    public void testPutInvalidNewPassword() throws Exception {
        String body = MessageFormat.format(xmlTemplate, "geoserver", "abc");
        assertEquals( 422, putAsServletResponse(MP_URI_XML,body,"text/xml").getStatusCode() );
    }
    
    @Test
    public void testPutInvalidCurrentPassword() throws Exception {
        String body = MessageFormat.format(xmlTemplate, "geoserverXY", "geoserver1");
        assertEquals( 422, putAsServletResponse(MP_URI_XML,body,"text/xml").getStatusCode() );
    }

    @Test
    public void testPutAsXML() throws Exception {
        
        String body = MessageFormat.format(xmlTemplate, "geoserver", "geoserver1");
        assertEquals( 200, putAsServletResponse(MP_URI_XML,body,"text/xml").getStatusCode() );        
        assertTrue(getSecurityManager().checkMasterPassword("geoserver1"));
        
        body = MessageFormat.format(xmlTemplate, "geoserver1", "geoserver");
        assertEquals( 200, putAsServletResponse(MP_URI_XML,body,"text/xml").getStatusCode() );
        assertTrue(getSecurityManager().checkMasterPassword("geoserver"));
    }
    
    @Test
    public void testPutAsJSON() throws Exception {
        
        String body = String.format(jsonTemplate, "geoserver", "geoserver1");
        assertEquals( 200, putAsServletResponse(MP_URI_JSON,body,"text/json").getStatusCode() );        
        assertTrue(getSecurityManager().checkMasterPassword("geoserver1"));  
        
        body = String.format(jsonTemplate, "geoserver1", "geoserver");
        assertEquals( 200, putAsServletResponse(MP_URI_JSON,body,"text/json").getStatusCode() );
        assertTrue(getSecurityManager().checkMasterPassword("geoserver"));
    }



    
}
