/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServerInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GlobalSettingsTest extends CatalogRESTTestSupport {

    @Before
    public void revertSettings() {
        GeoServerInfo global = getGeoServer().getGlobal();       
        global.getJAI().setAllowInterpolation(false);
        global.getJAI().setMemoryThreshold(0.75d);
        global.getJAI().setTilePriority(5);
        global.getCoverageAccess().setQueueType(QueueType.UNBOUNDED);
        getGeoServer().save(global);
        
        revertSettings(null);
    }
    
    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON("/rest/settings.json");
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
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/settings.xml");
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
    public void testPutAsJSON() throws Exception {
        String inputJson = "{'global': {" + "'settings':   {" + "'contact':     {"
                + "'contactPerson': 'Claudius Ptolomaeus'" + "}," + "'charset': 'UTF-8',"
                + "'numDecimals': '10'," + "'onlineResource': 'http://geoserver2.org',"
                + "'verbose': 'false'," + "'verboseExceptions': 'false'" + "}," + "'jai':   {"
                + "'allowInterpolation': 'false'," + "'recycling': 'true',"
                + "'tilePriority': '5'," + "'tileThreads': '7'," + "'memoryCapacity': '0.5',"
                + "'memoryThreshold': '0.75'," + "'imageIOCache': 'false',"
                + "'pngAcceleration': 'true'," + "'jpegAcceleration': 'true',"
                + "'allowNativeMosaic': 'false'" + "}," + "'coverageAccess':   {"
                + "'maxPoolSize': '5'," + "'corePoolSize': '5'," + "'keepAliveTime': '30000',"
                + "'queueType': 'UNBOUNDED'," + "'imageIOCacheThreshold': '10240'" + "},"
                + "'updateSequence': '0'," + "'featureTypeCacheSize': '0',"
                + "'globalServices': 'true'," + "'xmlPostRequestLogBufferSize': '2048'" + "}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/settings/", inputJson,
                "text/json");
        assertEquals(200, response.getStatusCode());
        JSON json = getAsJSON("/rest/settings.json");
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
    public void testPutAsXML() throws Exception {
        String xml = "<global><settings>" + "<charset>UTF-8</charset>"
                + "<numDecimals>10</numDecimals>"
                + "<onlineResource>http://geoserver.org</onlineResource>"
                + "<verbose>false</verbose>" + "<verboseExceptions>false</verboseExceptions>"
                + "<contact><contactPerson>Justin Deoliveira</contactPerson></contact></settings>"
                + "<jai>" + "<allowInterpolation>true</allowInterpolation>"
                + "<recycling>false</recycling>" + "<tilePriority>5</tilePriority>"
                + "<tileThreads>7</tileThreads>" + "<memoryCapacity>0.5</memoryCapacity>"
                + "<memoryThreshold>0.85</memoryThreshold>" + "<imageIOCache>false</imageIOCache>"
                + "<pngAcceleration>true</pngAcceleration>"
                + "<jpegAcceleration>true</jpegAcceleration>"
                + "<allowNativeMosaic>false</allowNativeMosaic>" + "</jai>" + "<coverageAccess>"
                + "<maxPoolSize>10</maxPoolSize>" + "<corePoolSize>5</corePoolSize>"
                + "<keepAliveTime>30000</keepAliveTime>" + "<queueType>UNBOUNDED</queueType>"
                + "<imageIOCacheThreshold>10240</imageIOCacheThreshold>" + "</coverageAccess>"
                + "<updateSequence>97</updateSequence>"
                + "<featureTypeCacheSize>0</featureTypeCacheSize>"
                + "<globalServices>false</globalServices>"
                + "<xmlPostRequestLogBufferSize>2048</xmlPostRequestLogBufferSize>" + "</global>";

        MockHttpServletResponse response = putAsServletResponse("/rest/settings/", xml, "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/settings.xml");
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

}
