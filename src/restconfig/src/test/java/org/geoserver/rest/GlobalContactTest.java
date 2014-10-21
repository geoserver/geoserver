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
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
public class GlobalContactTest extends CatalogRESTTestSupport {

    protected GeoServer geoServer;

    @Before
    public void init() {
        geoServer = getGeoServer();

        ContactInfo contactInfo = new ContactInfoImpl();
        contactInfo.setAddress("1600 Pennsylvania Avenue");
        contactInfo.setAddressCity("Washington");
        contactInfo.setAddressPostalCode("20001");
        contactInfo.setAddressCountry("United States");
        contactInfo.setAddressState("DC");
        
        GeoServerInfo geoServerInfo = geoServer.getGlobal();
        SettingsInfo settingsInfo = geoServerInfo.getSettings();
        settingsInfo.setContact(contactInfo);
        geoServer.save(geoServerInfo);        
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON("/rest/settings/contact.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject contactInfo = jsonObject.getJSONObject("contact");
        assertNotNull(contactInfo);
        assertEquals("United States", contactInfo.get("addressCountry"));
        assertEquals("1600 Pennsylvania Avenue", contactInfo.get("address"));
        assertEquals("Washington", contactInfo.get("addressCity"));
        assertEquals("DC", contactInfo.get("addressState"));
        assertEquals("20001", contactInfo.get("addressPostalCode").toString());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/settings/contact.xml");
        assertEquals("contact", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("United States", "/contact/addressCountry", dom);
        assertXpathEvaluatesTo("Washington", "/contact/addressCity", dom);
        assertXpathEvaluatesTo("1600 Pennsylvania Avenue", "/contact/address", dom);
        assertXpathEvaluatesTo("DC", "/contact/addressState", dom);
        assertXpathEvaluatesTo("20001", "/contact/addressPostalCode", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String inputJson = "{'contact':" + "{'id': 'contact',"
                + "'address': '500 Market Street'," + "'addressCity': 'Philadelphia',"
                + "'addressCountry': 'United States'," + "'addressPostalCode': '19106',"
                + "'addressState': 'PA'" + "}" + "}";
        MockHttpServletResponse response = putAsServletResponse("/rest/settings/contact",
                inputJson, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/settings/contact.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject contactInfo = jsonObject.getJSONObject("contact");
        assertEquals("United States", contactInfo.get("addressCountry"));
        assertEquals("500 Market Street", contactInfo.get("address"));
        assertEquals("Philadelphia", contactInfo.get("addressCity"));
        assertEquals("PA", contactInfo.get("addressState"));
        assertEquals("19106", contactInfo.get("addressPostalCode").toString());
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml = "<contact>" + "<address>1600 Pennsylvania Avenue</address>"
                + "<addressCity>Washington</addressCity>"
                + "<addressCountry>United States</addressCountry>"
                + "<addressPostalCode>20001</addressPostalCode>"
                + "<addressState>DC</addressState>" + "<addressType>Avenue</addressType>"
                + "<contactEmail>chief.geographer@mail.com</contactEmail>"
                + "<contactOrganization>GeoServer</contactOrganization>"
                + "<contactPerson>ContactPerson</contactPerson>"
                + "<contactPosition>Chief Geographer</contactPosition>" + "</contact>";
        MockHttpServletResponse response = putAsServletResponse("/rest/settings/contact", xml,
                "text/xml");
        assertEquals(200, response.getStatusCode());
        JSON json = getAsJSON("/rest/settings/contact.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject contactInfo = jsonObject.getJSONObject("contact");
        assertEquals("United States", contactInfo.get("addressCountry"));
        assertEquals("1600 Pennsylvania Avenue", contactInfo.get("address"));
        assertEquals("Washington", contactInfo.get("addressCity"));
        assertEquals("DC", contactInfo.get("addressState"));
        assertEquals("20001", contactInfo.get("addressPostalCode").toString());
        assertEquals("Chief Geographer", contactInfo.get("contactPosition").toString());
        assertEquals("ContactPerson", contactInfo.get("contactPerson").toString());
    }

}
