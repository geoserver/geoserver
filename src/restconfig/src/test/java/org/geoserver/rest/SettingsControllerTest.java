/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.config.*;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class SettingsControllerTest extends CatalogRESTTestSupport {

    protected GeoServer geoServer;

    @Before
    public void init() {
        geoServer = getGeoServer();

        // revert global settings
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getJAI().setAllowInterpolation(false);
        global.getJAI().setMemoryThreshold(0.75d);
        global.getJAI().setTilePriority(5);
        global.getCoverageAccess().setQueueType(CoverageAccessInfo.QueueType.UNBOUNDED);
        getGeoServer().save(global);

        revertSettings(null);

        // revert local settings
        revertSettings("sf");
    }

    public void initContact() {
        ContactInfo contactInfo = new ContactInfoImpl();
        contactInfo.setAddress("1600 Pennsylvania Avenue");
        contactInfo.setAddressCity("Washington");
        contactInfo.setAddressPostalCode("20001");
        contactInfo.setAddressCountry("United States");
        contactInfo.setAddressState("DC");
        contactInfo.setAddressDeliveryPoint("The White House");
        contactInfo.setContactEmail("info@whitehouse.gov");

        GeoServerInfo geoServerInfo = geoServer.getGlobal();
        SettingsInfo settingsInfo = geoServerInfo.getSettings();
        settingsInfo.setContact(contactInfo);
        geoServer.save(geoServerInfo);
    }

    @After
    public void reset() throws Exception {
        LocalWorkspace.remove();
    }

    @Test
    public void testGetContactAsJSON() throws Exception {
        initContact();
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/settings/contact.json");
        print(json);
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject contactInfo = jsonObject.getJSONObject("contact");
        assertNotNull(contactInfo);
        assertEquals("United States", contactInfo.get("addressCountry"));
        assertEquals("1600 Pennsylvania Avenue", contactInfo.get("address"));
        assertEquals("Washington", contactInfo.get("addressCity"));
        assertEquals("DC", contactInfo.get("addressState"));
        assertEquals("20001", contactInfo.get("addressPostalCode").toString());
        assertEquals("The White House", contactInfo.get("addressDeliveryPoint").toString());
        assertEquals("info@whitehouse.gov", contactInfo.get("contactEmail").toString());
    }

    @Test
    public void testGetContactAsXML() throws Exception {
        initContact();
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/settings/contact.xml");
        assertEquals("contact", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("United States", "/contact/addressCountry", dom);
        assertXpathEvaluatesTo("Washington", "/contact/addressCity", dom);
        assertXpathEvaluatesTo("1600 Pennsylvania Avenue", "/contact/address", dom);
        assertXpathEvaluatesTo("DC", "/contact/addressState", dom);
        assertXpathEvaluatesTo("20001", "/contact/addressPostalCode", dom);
        assertXpathEvaluatesTo("The White House", "/contact/addressDeliveryPoint", dom);
        assertXpathEvaluatesTo("info@whitehouse.gov", "/contact/contactEmail", dom);
    }

    @Test
    public void testGetContactAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/settings/contact.html", 200);
    }

    @Test
    public void testPutContactAsJSON() throws Exception {
        initContact();
        String inputJson =
                "{'contact':{"
                        + "    'id':'contact',"
                        + "    'address':'500 Market Street',"
                        + "    'addressCity':'Philadelphia',"
                        + "    'addressCountry':'United States',"
                        + "    'addressPostalCode':'19106',"
                        + "    'addressState':'PA',"
                        + "    'addressDeliveryPoint':'The White House',"
                        + "    'addressElectronicMailAddress':'info@whitehouse.gov'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/settings/contact", inputJson, "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/settings/contact.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject contactInfo = jsonObject.getJSONObject("contact");
        assertEquals("United States", contactInfo.get("addressCountry"));
        assertEquals("500 Market Street", contactInfo.get("address"));
        assertEquals("Philadelphia", contactInfo.get("addressCity"));
        assertEquals("PA", contactInfo.get("addressState"));
        assertEquals("19106", contactInfo.get("addressPostalCode").toString());
        assertEquals("The White House", contactInfo.get("addressDeliveryPoint").toString());
        assertEquals("info@whitehouse.gov", contactInfo.get("contactEmail").toString());
    }

    @Test
    public void testPutContactAsXML() throws Exception {
        initContact();
        String xml =
                "<contact> <address>1600 Pennsylvania Avenue</address>"
                        + "<addressCity>Washington</addressCity>"
                        + "<addressCountry>United States</addressCountry>"
                        + "<addressPostalCode>20001</addressPostalCode>"
                        + "<addressDeliveryPoint>The White House</addressDeliveryPoint>"
                        + "<addressElectronicMailAddress>info@whitehouse.gov</addressElectronicMailAddress>"
                        + "<addressState>DC</addressState>"
                        + "<addressType>Avenue</addressType>"
                        + "<contactEmail>chief.geographer@mail.com</contactEmail>"
                        + "<contactOrganization>GeoServer</contactOrganization>"
                        + "<contactPerson>ContactPerson</contactPerson>"
                        + "<contactPosition>Chief Geographer</contactPosition> </contact>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/settings/contact", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/settings/contact.xml");
        assertEquals("contact", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("United States", "/contact/addressCountry", dom);
        assertXpathEvaluatesTo("1600 Pennsylvania Avenue", "/contact/address", dom);
        assertXpathEvaluatesTo("Washington", "/contact/addressCity", dom);
        assertXpathEvaluatesTo("DC", "/contact/addressState", dom);
        assertXpathEvaluatesTo("20001", "/contact/addressPostalCode", dom);
        assertXpathEvaluatesTo("Chief Geographer", "/contact/contactPosition", dom);
        assertXpathEvaluatesTo("ContactPerson", "/contact/contactPerson", dom);
        assertXpathEvaluatesTo("The White House", "/contact/addressDeliveryPoint", dom);
        assertXpathEvaluatesTo("chief.geographer@mail.com", "/contact/contactEmail", dom);
    }

    @Test
    public void testGetGlobalAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/settings.json");
        print(json);
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject global = jsonObject.getJSONObject("global");
        assertNotNull(global);

        JSONObject settings = global.getJSONObject("settings");
        JSONObject contact = settings.getJSONObject("contact");
        assertNotNull(contact);
        assertEquals("Andrea Aime", contact.get("contactPerson"));

        assertEquals("UTF-8", settings.get("charset"));
        assertEquals("8", settings.get("numDecimals").toString().trim());
        assertEquals("http://geoserver.org", settings.get("onlineResource"));

        JSONObject jaiInfo = global.getJSONObject("jai");
        assertNotNull(jaiInfo);
        assertEquals("false", jaiInfo.get("allowInterpolation").toString().trim());
        assertEquals("0.75", jaiInfo.get("memoryThreshold").toString().trim());
        assertEquals("5", jaiInfo.get("tilePriority").toString().trim());

        JSONObject covInfo = global.getJSONObject("coverageAccess");
        assertEquals("UNBOUNDED", covInfo.get("queueType"));
    }

    @Test
    public void testGetGlobalAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/settings.xml");
        assertEquals("global", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("UTF-8", "/global/settings/charset", dom);
        assertXpathEvaluatesTo("8", "/global/settings/numDecimals", dom);
        assertXpathEvaluatesTo("http://geoserver.org", "/global/settings/onlineResource", dom);
        assertXpathEvaluatesTo("Andrea Aime", "/global/settings/contact/contactPerson", dom);
        assertXpathEvaluatesTo("false", "/global/jai/allowInterpolation", dom);
        assertXpathEvaluatesTo("0.75", "/global/jai/memoryThreshold", dom);
        assertXpathEvaluatesTo("UNBOUNDED", "/global/coverageAccess/queueType", dom);
    }

    @Test
    public void testPutGlobalAsJSON() throws Exception {
        String inputJson =
                "{'global': {"
                        + "'settings':   {"
                        + "'contact':     {"
                        + "'contactPerson': 'Claudius Ptolomaeus'"
                        + "},"
                        + "'charset': 'UTF-8',"
                        + "'numDecimals': '10',"
                        + "'onlineResource': 'http://geoserver2.org',"
                        + "'verbose': 'false',"
                        + "'verboseExceptions': 'false'"
                        + "},"
                        + "'jai':   {"
                        + "'allowInterpolation': 'false',"
                        + "'recycling': 'true',"
                        + "'tilePriority': '5',"
                        + "'tileThreads': '7',"
                        + "'memoryCapacity': '0.5',"
                        + "'memoryThreshold': '0.75',"
                        + "'imageIOCache': 'false',"
                        + "'pngAcceleration': 'true',"
                        + "'jpegAcceleration': 'true',"
                        + "'allowNativeMosaic': 'false'"
                        + "},"
                        + "'coverageAccess':   {"
                        + "'maxPoolSize': '5',"
                        + "'corePoolSize': '5',"
                        + "'keepAliveTime': '30000',"
                        + "'queueType': 'UNBOUNDED',"
                        + "'imageIOCacheThreshold': '10240'"
                        + "},"
                        + "'updateSequence': '0',"
                        + "'featureTypeCacheSize': '0',"
                        + "'globalServices': 'true',"
                        + "'xmlPostRequestLogBufferSize': '2048'"
                        + "}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/settings/", inputJson, "text/json");
        assertEquals(200, response.getStatus());
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject global = jsonObject.getJSONObject("global");
        assertNotNull(global);
        assertEquals("true", global.get("globalServices").toString().trim());
        assertEquals("2048", global.get("xmlPostRequestLogBufferSize").toString().trim());

        JSONObject settings = global.getJSONObject("settings");
        assertNotNull(settings);
        assertEquals("UTF-8", settings.get("charset"));
        assertEquals("10", settings.get("numDecimals").toString().trim());
        assertEquals("http://geoserver2.org", settings.get("onlineResource"));

        JSONObject contact = settings.getJSONObject("contact");
        assertNotNull(contact);
        assertEquals("Claudius Ptolomaeus", contact.get("contactPerson"));

        JSONObject jaiInfo = global.getJSONObject("jai");
        assertNotNull(jaiInfo);
        assertEquals("false", jaiInfo.get("allowInterpolation").toString().trim());
        assertEquals("0.75", jaiInfo.get("memoryThreshold").toString().trim());

        JSONObject covInfo = global.getJSONObject("coverageAccess");
        assertEquals("UNBOUNDED", covInfo.get("queueType"));
    }

    @Test
    public void testGetGlobalAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/settings.html", 200);
    }

    @Test
    public void testPutGlobalAsXML() throws Exception {
        String xml =
                "<global><settings>"
                        + "<charset>UTF-8</charset>"
                        + "<numDecimals>10</numDecimals>"
                        + "<onlineResource>http://geoserver.org</onlineResource>"
                        + "<verbose>false</verbose>"
                        + "<verboseExceptions>false</verboseExceptions>"
                        + "<contact><contactPerson>Justin Deoliveira</contactPerson></contact></settings>"
                        + "<jai>"
                        + "<allowInterpolation>true</allowInterpolation>"
                        + "<recycling>false</recycling>"
                        + "<tilePriority>5</tilePriority>"
                        + "<tileThreads>7</tileThreads>"
                        + "<memoryCapacity>0.5</memoryCapacity>"
                        + "<memoryThreshold>0.85</memoryThreshold>"
                        + "<imageIOCache>false</imageIOCache>"
                        + "<pngAcceleration>true</pngAcceleration>"
                        + "<jpegAcceleration>true</jpegAcceleration>"
                        + "<allowNativeMosaic>false</allowNativeMosaic>"
                        + "</jai>"
                        + "<coverageAccess>"
                        + "<maxPoolSize>10</maxPoolSize>"
                        + "<corePoolSize>5</corePoolSize>"
                        + "<keepAliveTime>30000</keepAliveTime>"
                        + "<queueType>UNBOUNDED</queueType>"
                        + "<imageIOCacheThreshold>10240</imageIOCacheThreshold>"
                        + "</coverageAccess>"
                        + "<updateSequence>97</updateSequence>"
                        + "<featureTypeCacheSize>0</featureTypeCacheSize>"
                        + "<globalServices>false</globalServices>"
                        + "<xmlPostRequestLogBufferSize>2048</xmlPostRequestLogBufferSize>"
                        + "</global>";

        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + "/settings/", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/settings.xml");
        assertEquals("global", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/global/globalServices", dom);
        assertXpathEvaluatesTo("2048", "/global/xmlPostRequestLogBufferSize", dom);
        assertXpathEvaluatesTo("UTF-8", "/global/settings/charset", dom);
        assertXpathEvaluatesTo("10", "/global/settings/numDecimals", dom);
        assertXpathEvaluatesTo("http://geoserver.org", "/global/settings/onlineResource", dom);
        assertXpathEvaluatesTo("Justin Deoliveira", "/global/settings/contact/contactPerson", dom);
        assertXpathEvaluatesTo("true", "/global/jai/allowInterpolation", dom);
        assertXpathEvaluatesTo("0.85", "/global/jai/memoryThreshold", dom);
        assertXpathEvaluatesTo("UNBOUNDED", "/global/coverageAccess/queueType", dom);
    }

    @Test
    public void testGetLocalAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.json");
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
    public void testGetLocalAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("sf", "/settings/workspace/name", dom);
        assertXpathEvaluatesTo("UTF-8", "/settings/charset", dom);
        assertXpathEvaluatesTo("8", "/settings/numDecimals", dom);
        assertXpathEvaluatesTo("false", "/settings/verbose", dom);
        assertXpathEvaluatesTo("false", "/settings/verboseExceptions", dom);
        assertXpathEvaluatesTo("Andrea Aime", "/settings/contact/contactPerson", dom);
    }

    @Test
    public void testGetLocalAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.html", 200);
    }

    @Test
    public void testCreateLocalAsJSON() throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.remove(geoServer.getSettings(geoServer.getCatalog().getWorkspaceByName("sf")));
        String json =
                "{'settings':{'workspace':{'name':'sf'},"
                        + "'contact':{'addressCity':'Alexandria','addressCountry':'Egypt','addressType':'Work',"
                        + "'contactEmail':'claudius.ptolomaeus@gmail.com','contactOrganization':'The ancient geographes INC',"
                        + "'contactPerson':'Claudius Ptolomaeus','contactPosition':'Chief geographer'},"
                        + "'charset':'UTF-8','numDecimals':10,'onlineResource':'http://geoserver.org',"
                        + "'proxyBaseUrl':'http://proxy.url','verbose':false,'verboseExceptions':'true'}}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/settings",
                        json,
                        "text/json");
        assertEquals(201, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.json");
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
    public void testCreateLocalAsXML() throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.remove(geoServer.getSettings(geoServer.getCatalog().getWorkspaceByName("sf")));
        String xml =
                "<settings>"
                        + "<workspace><name>sf</name></workspace>"
                        + "<contact>"
                        + "<addressCity>Alexandria</addressCity>"
                        + "<addressCountry>Egypt</addressCountry>"
                        + "<addressType>Work</addressType>"
                        + "<contactEmail>claudius.ptolomaeus@gmail.com</contactEmail>"
                        + "<contactOrganization>The ancient geographes INC</contactOrganization>"
                        + "<contactPerson>Claudius Ptolomaeus</contactPerson>"
                        + "<contactPosition>Chief geographer</contactPosition>"
                        + "</contact>"
                        + "<charset>UTF-8</charset>"
                        + "<numDecimals>8</numDecimals>"
                        + "<onlineResource>http://geoserver.org</onlineResource>"
                        + "<proxyBaseUrl>http://proxy.url</proxyBaseUrl>"
                        + "<verbose>false</verbose>"
                        + "<verboseExceptions>false</verboseExceptions>"
                        + "</settings>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/settings", xml, "text/xml");
        assertEquals(201, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("sf", "/settings/workspace/name", dom);
        assertXpathEvaluatesTo("false", "/settings/verbose", dom);
        assertXpathEvaluatesTo("false", "/settings/verboseExceptions", dom);
        assertXpathEvaluatesTo("http://geoserver.org", "/settings/onlineResource", dom);
        assertXpathEvaluatesTo("http://proxy.url", "/settings/proxyBaseUrl", dom);
        assertXpathEvaluatesTo("Claudius Ptolomaeus", "/settings/contact/contactPerson", dom);
        assertXpathEvaluatesTo(
                "claudius.ptolomaeus@gmail.com", "/settings/contact/contactEmail", dom);
        assertXpathEvaluatesTo("Chief geographer", "/settings/contact/contactPosition", dom);
        assertXpathEvaluatesTo(
                "The ancient geographes INC", "/settings/contact/contactOrganization", dom);
        assertXpathEvaluatesTo("Egypt", "/settings/contact/addressCountry", dom);
    }

    @Test
    public void testCreateLocalAlreadyExists() throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.remove(geoServer.getSettings(geoServer.getCatalog().getWorkspaceByName("sf")));
        String xml =
                "<settings>"
                        + "<workspace><name>sf</name></workspace>"
                        + "<contact>"
                        + "<addressCity>Alexandria</addressCity>"
                        + "<addressCountry>Egypt</addressCountry>"
                        + "<addressType>Work</addressType>"
                        + "<contactEmail>claudius.ptolomaeus@gmail.com</contactEmail>"
                        + "<contactOrganization>The ancient geographes INC</contactOrganization>"
                        + "<contactPerson>Claudius Ptolomaeus</contactPerson>"
                        + "<contactPosition>Chief geographer</contactPosition>"
                        + "</contact>"
                        + "<charset>UTF-8</charset>"
                        + "<numDecimals>8</numDecimals>"
                        + "<onlineResource>http://geoserver.org</onlineResource>"
                        + "<proxyBaseUrl>http://proxy.url</proxyBaseUrl>"
                        + "<verbose>false</verbose>"
                        + "<verboseExceptions>false</verboseExceptions>"
                        + "</settings>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/settings", xml, "text/xml");
        assertEquals(201, response.getStatus());

        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/settings", xml, "text/xml");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testPutLocalAsJSON() throws Exception {
        String inputJson =
                "{'settings':{'workspace':{'name':'sf'},"
                        + "'contact':{'addressCity':'Cairo','addressCountry':'Egypt','addressType':'Work',"
                        + "'contactEmail':'claudius.ptolomaeus@gmail.com','contactOrganization':'The ancient geographes INC',"
                        + "'contactPerson':'Claudius Ptolomaeus','contactPosition':'Chief geographer'},"
                        + "'charset':'UTF-8','numDecimals':8,'onlineResource':'http://geoserver2.org',"
                        + "'proxyBaseUrl':'http://proxy2.url','verbose':true,'verboseExceptions':'true'}}";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/settings",
                        inputJson,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.json");
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
    public void testPutLocalAsXML() throws Exception {
        String xml =
                "<settings>"
                        + "<workspace><name>sf</name></workspace>"
                        + "<contact>"
                        + "<addressCity>Cairo</addressCity>"
                        + "<addressCountry>Egypt</addressCountry>"
                        + "<addressType>Work</addressType>"
                        + "<contactEmail>claudius.ptolomaeus@gmail.com</contactEmail>"
                        + "<contactOrganization>The ancient geographes INC</contactOrganization>"
                        + "<contactPerson>Claudius Ptolomaeus</contactPerson>"
                        + "<contactPosition>Chief geographer</contactPosition>"
                        + "</contact>"
                        + "<charset>UTF-8</charset>"
                        + "<numDecimals>10</numDecimals>"
                        + "<onlineResource>http://geoserver2.org</onlineResource>"
                        + "<proxyBaseUrl>http://proxy2.url</proxyBaseUrl>"
                        + "<verbose>true</verbose>"
                        + "<verboseExceptions>true</verboseExceptions>"
                        + "</settings>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/settings", xml, "text/xml");
        assertEquals(200, response.getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.xml");
        assertEquals("settings", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("sf", "/settings/workspace/name", dom);
        assertXpathEvaluatesTo("true", "/settings/verbose", dom);
        assertXpathEvaluatesTo("true", "/settings/verboseExceptions", dom);
        assertXpathEvaluatesTo("http://geoserver2.org", "/settings/onlineResource", dom);
        assertXpathEvaluatesTo("http://proxy2.url", "/settings/proxyBaseUrl", dom);
        assertXpathEvaluatesTo("Claudius Ptolomaeus", "/settings/contact/contactPerson", dom);
        assertXpathEvaluatesTo(
                "claudius.ptolomaeus@gmail.com", "/settings/contact/contactEmail", dom);
        assertXpathEvaluatesTo("Chief geographer", "/settings/contact/contactPosition", dom);
        assertXpathEvaluatesTo(
                "The ancient geographes INC", "/settings/contact/contactOrganization", dom);
        assertXpathEvaluatesTo("Cairo", "/settings/contact/addressCity", dom);
    }

    @Test
    public void testDeleteLocal() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        assertEquals(
                200,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/settings")
                        .getStatus());
        json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/settings.json");
        JSONObject deletedJson = (JSONObject) json;
        assertNull(deletedJson.get("workspace"));
    }
}
