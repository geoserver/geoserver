/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Class with FontListResource tests
 *
 * @author Jose Garca
 */
public class FontListTest extends GeoServerSystemTestSupport {

    @Test
    public void testGetAsXML() throws Exception {
        // make the request, parsing the result as a dom
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/fonts.xml");

        /*TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(dom), new StreamResult(writer));
        String output = writer.toString();
        System.out.println(output);*/
        // make assertions
        Node fonts = getFirstElementByTagName(dom, "fonts");
        assertNotNull(fonts);
        assertTrue(((Element) fonts).getElementsByTagName("entry").getLength() > 0);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        // make the request, parsing the result into a json object
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/fonts.json");

        // make assertions
        assertTrue(json instanceof JSONObject);
        assertTrue(((JSONObject) json).get("fonts") instanceof JSONArray);
    }
}
