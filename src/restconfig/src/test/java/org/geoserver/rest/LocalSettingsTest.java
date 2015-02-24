/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.util.OwsUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LocalSettingsTest extends CatalogRESTTestSupport {

    @Before
    public void revertSettings() {
        revertSettings("sf");
    }

    @After
    public void clearLocalWorkspace() throws Exception {
        LocalWorkspace.remove();   
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON("/rest/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject settings = jsonObject.getJSONObject("settings");
        assertNotNull(settings);
        JSONObject workspace = settings.getJSONObject("workspace");
        assertEquals("sf", workspace.get("name"));
        assertEquals("UTF-8", settings.get("charset"));
        assertEquals("8", settings.get("numDecimals").toString().trim());
        assertEquals("false", settings.get("verbose").toString().trim());
        assertEquals("false", settings.get("verboseExceptions").toString().trim());
        JSONObject contact = settings.getJSONObject("contact");
        assertNotNull(contact);
        assertEquals("Andrea Aime", contact.get("contactPerson"));

    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/workspaces/sf/settings.xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("sf", "/settings/workspace/name", dom);
        assertXpathEvaluatesTo("UTF-8", "/settings/charset", dom);
        assertXpathEvaluatesTo("8", "/settings/numDecimals", dom);
        assertXpathEvaluatesTo("false", "/settings/verbose", dom);
        assertXpathEvaluatesTo("false", "/settings/verboseExceptions", dom);
        assertXpathEvaluatesTo("Andrea Aime", "/settings/contact/contactPerson", dom);
    }

    @Test
    public void testCreateAsJSON() throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.remove(geoServer.getSettings(geoServer.getCatalog().getWorkspaceByName("sf")));
        String json = "{'settings':{'workspace':{'name':'sf'},"
                + "'contact':{'addressCity':'Alexandria','addressCountry':'Egypt','addressType':'Work',"
                + "'contactEmail':'claudius.ptolomaeus@gmail.com','contactOrganization':'The ancient geographes INC',"
                + "'contactPerson':'Claudius Ptolomaeus','contactPosition':'Chief geographer'},"
                + "'charset':'UTF-8','numDecimals':10,'onlineResource':'http://geoserver.org',"
                + "'proxyBaseUrl':'http://proxy.url','verbose':false,'verboseExceptions':'true'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/workspaces/sf/settings",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject settings = jsonObject.getJSONObject("settings");
        assertNotNull(settings);
        JSONObject workspace = settings.getJSONObject("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        assertEquals("10", settings.get("numDecimals").toString().trim());
        assertEquals("http://geoserver.org", settings.get("onlineResource"));
        assertEquals("http://proxy.url", settings.get("proxyBaseUrl"));
        JSONObject contact = settings.getJSONObject("contact");
        assertEquals("Claudius Ptolomaeus", contact.get("contactPerson"));
        assertEquals("The ancient geographes INC", contact.get("contactOrganization"));
        assertEquals("Work", contact.get("addressType"));
        assertEquals("claudius.ptolomaeus@gmail.com", contact.get("contactEmail"));

    }

    @Test
    public void testCreateAsXML() throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.remove(geoServer.getSettings(geoServer.getCatalog().getWorkspaceByName("sf")));
        String xml = "<settings>" + "<workspace><name>sf</name></workspace>" + "<contact>"
                + "<addressCity>Alexandria</addressCity>"
                + "<addressCountry>Egypt</addressCountry>" + "<addressType>Work</addressType>"
                + "<contactEmail>claudius.ptolomaeus@gmail.com</contactEmail>"
                + "<contactOrganization>The ancient geographes INC</contactOrganization>"
                + "<contactPerson>Claudius Ptolomaeus</contactPerson>"
                + "<contactPosition>Chief geographer</contactPosition>" + "</contact>"
                + "<charset>UTF-8</charset>" + "<numDecimals>8</numDecimals>"
                + "<onlineResource>http://geoserver.org</onlineResource>"
                + "<proxyBaseUrl>http://proxy.url</proxyBaseUrl>"
                + "<verbose>false</verbose>" + "<verboseExceptions>false</verboseExceptions>"
                + "</settings>";
        MockHttpServletResponse response = putAsServletResponse("/rest/workspaces/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatusCode());

        Document dom = getAsDOM("/rest/workspaces/sf/settings.xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("sf", "/settings/workspace/name", dom);
        assertXpathEvaluatesTo("false", "/settings/verbose", dom);
        assertXpathEvaluatesTo("false", "/settings/verboseExceptions", dom);
        assertXpathEvaluatesTo("http://geoserver.org","/settings/onlineResource",dom);
        assertXpathEvaluatesTo("http://proxy.url","/settings/proxyBaseUrl",dom);
        assertXpathEvaluatesTo("Claudius Ptolomaeus","/settings/contact/contactPerson",dom);
        assertXpathEvaluatesTo("claudius.ptolomaeus@gmail.com","/settings/contact/contactEmail",dom);
        assertXpathEvaluatesTo("Chief geographer","/settings/contact/contactPosition",dom);
        assertXpathEvaluatesTo("The ancient geographes INC","/settings/contact/contactOrganization",dom);
        assertXpathEvaluatesTo("Egypt","/settings/contact/addressCountry",dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String inputJson = "{'settings':{'workspace':{'name':'sf'},"
                + "'contact':{'addressCity':'Cairo','addressCountry':'Egypt','addressType':'Work',"
                + "'contactEmail':'claudius.ptolomaeus@gmail.com','contactOrganization':'The ancient geographes INC',"
                + "'contactPerson':'Claudius Ptolomaeus','contactPosition':'Chief geographer'},"
                + "'charset':'UTF-8','numDecimals':8,'onlineResource':'http://geoserver2.org',"
                + "'proxyBaseUrl':'http://proxy2.url','verbose':true,'verboseExceptions':'true'}}";

        MockHttpServletResponse response = putAsServletResponse("/rest/workspaces/sf/settings",
                inputJson, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject settings = jsonObject.getJSONObject("settings");
        assertNotNull(settings);
        JSONObject workspace = settings.getJSONObject("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        assertEquals("8", settings.get("numDecimals").toString().trim());
        assertEquals("http://geoserver2.org", settings.get("onlineResource"));
        assertEquals("http://proxy2.url", settings.get("proxyBaseUrl"));
        assertEquals("true", settings.get("verbose").toString().trim());
        assertEquals("true", settings.get("verboseExceptions").toString().trim());
        JSONObject contact = settings.getJSONObject("contact");
        assertNotNull(contact);
        assertEquals("Claudius Ptolomaeus", contact.get("contactPerson"));
        assertEquals("Cairo", contact.get("addressCity"));
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =  "<settings>" + "<workspace><name>sf</name></workspace>" + "<contact>"
                + "<addressCity>Cairo</addressCity>"
                + "<addressCountry>Egypt</addressCountry>" + "<addressType>Work</addressType>"
                + "<contactEmail>claudius.ptolomaeus@gmail.com</contactEmail>"
                + "<contactOrganization>The ancient geographes INC</contactOrganization>"
                + "<contactPerson>Claudius Ptolomaeus</contactPerson>"
                + "<contactPosition>Chief geographer</contactPosition>" + "</contact>"
                + "<charset>UTF-8</charset>" + "<numDecimals>10</numDecimals>"
                + "<onlineResource>http://geoserver2.org</onlineResource>"
                + "<proxyBaseUrl>http://proxy2.url</proxyBaseUrl>"
                + "<verbose>true</verbose>" + "<verboseExceptions>true</verboseExceptions>"
                + "</settings>";
        MockHttpServletResponse response = putAsServletResponse("/rest/workspaces/sf/settings",
                xml, "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/workspaces/sf/settings.xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("sf", "/settings/workspace/name", dom);
        assertXpathEvaluatesTo("true", "/settings/verbose", dom);
        assertXpathEvaluatesTo("true", "/settings/verboseExceptions", dom);
        assertXpathEvaluatesTo("http://geoserver2.org","/settings/onlineResource",dom);
        assertXpathEvaluatesTo("http://proxy2.url","/settings/proxyBaseUrl",dom);
        assertXpathEvaluatesTo("Claudius Ptolomaeus","/settings/contact/contactPerson",dom);
        assertXpathEvaluatesTo("claudius.ptolomaeus@gmail.com","/settings/contact/contactEmail",dom);
        assertXpathEvaluatesTo("Chief geographer","/settings/contact/contactPosition",dom);
        assertXpathEvaluatesTo("The ancient geographes INC","/settings/contact/contactOrganization",dom);
        assertXpathEvaluatesTo("Cairo","/settings/contact/addressCity",dom);
    }

    @Test
    public void testDelete() throws Exception {
        JSON json = getAsJSON("/rest/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        assertEquals(200, deleteAsServletResponse("/rest/workspaces/sf/settings").getStatusCode());
        json = getAsJSON("/rest/workspaces/sf/settings.json");
        JSONObject deletedJson = (JSONObject) json;
        assertNull(deletedJson.get("workspace"));
    }

}
