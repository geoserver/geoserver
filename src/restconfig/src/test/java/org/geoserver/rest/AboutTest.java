/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Carlo Cancellieri - GeoSolutions SAS
 */
public class AboutTest extends GeoServerSystemTestSupport {

    @Test
    public void testEmptyListHTMLTemplate() throws Exception {
        try {
            getAsDOM("/rest/about/version?manifest=NOTEXISTS.*");
        } catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testGetVersionsAsXML() throws Exception {
        // make the request, parsing the result as a dom
        Document dom = getAsDOM("/rest/about/version.xml");

        checkXMLModel(dom);
    }

    @Test
    public void testGetManifestsAsXML() throws Exception {
        // make the request, parsing the result as a dom
        Document dom = getAsDOM("/rest/about/manifest.xml");

        checkXMLModel(dom);
    }

    @Test
    public void testGetAsVersionsHTML() throws Exception {
        // make the request, parsing the result into a Dom object
        Document dom = getAsDOM("/rest/about/version");

        checkHTMLModel(dom);
    }

    @Test
    public void testGetAsManifestsHTML() throws Exception {
        // make the request, parsing the result into a Dom object
        Document dom = getAsDOM("/rest/about/manifest?manifest=freemarker.*");

        checkHTMLModel(dom);
    }

    @Test
    public void testGetAsVersionsJSON() throws Exception {
        // make the request, parsing the result into a json object
        JSONObject json = (JSONObject) getAsJSON("/rest/about/version.json");

        checkJSONModel(json);
    }

    @Test
    public void testGetAsManifestsJSON() throws Exception {
        // make the request, parsing the result into a json object
        JSONObject json = (JSONObject) getAsJSON("/rest/about/manifest.json");

        checkJSONModel(json);
    }

    private void checkHTMLModel(Document dom) {
        // make assertions
        Node resource = getFirstElementByTagName(dom, "h2");
        resource.getTextContent().equalsIgnoreCase("About:");
        assertNotNull(resource);
        try {
            serializeXML(dom);
        } catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }

    }

    private void checkJSONModel(JSONObject json) {
        // StringWriter sw = new StringWriter();
        // json.write(sw);
        // System.out.println(sw.toString());

        // make assertions
        assertTrue(json instanceof JSONObject);
        Object obj = json.get("about");

        assertTrue(obj instanceof JSONObject);
        JSONObject about = (JSONObject) obj;

        obj = about.get("resource");
        assertTrue(obj instanceof JSONArray);

        // JSONArray resources = (JSONArray) obj;
    }

    private void checkXMLModel(Document dom) {
        // make assertions
        Node resource = getFirstElementByTagName(dom, "resource");

        // serializeXML(dom);

        assertNotNull(resource);
        assertTrue(((Element) resource).getAttribute("name").length() > 0);
    }

    protected static void serializeXML(Document domDoc) throws TransformerException {

        DOMSource domSrc;
        Transformer txformer;
        StringWriter sw;
        StreamResult sr;

        try {
            domSrc = new DOMSource(domDoc);

            txformer = TransformerFactory.newInstance().newTransformer();
            txformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            txformer.setOutputProperty(OutputKeys.METHOD, "xml");
            txformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            txformer.setOutputProperty(OutputKeys.INDENT, "yes");
            txformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            txformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            sw = new StringWriter();
            sr = new StreamResult(sw);

            txformer.transform(domSrc, sr);

            System.out.println(sw.toString());
        } catch (TransformerConfigurationException ex) {
            ex.printStackTrace();
            throw ex;
        } catch (TransformerFactoryConfigurationError ex) {
            ex.printStackTrace();
            throw ex;
        } catch (TransformerException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
