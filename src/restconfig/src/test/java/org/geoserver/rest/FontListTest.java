package org.geoserver.rest;

import org.geoserver.test.GeoServerTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

/**
 * Class with FontListResource tests
 *
 * @author Jose Garc’a
 */
public class FontListTest extends GeoServerTestSupport {
    public void testGetAsXML() throws Exception {
        //make the request, parsing the result as a dom
        Document dom = getAsDOM("/rest/fonts.xml");

        //print out the result
        print(dom);

        //make assertions
        Node fonts = getFirstElementByTagName(dom, "fonts");
        assertNotNull(fonts);
        assertTrue( ((Element) fonts).getElementsByTagName("entry").getLength()  > 0);
    }

    public void testGetAsJSON() throws Exception {
        //make the request, parsing the result into a json object
        JSON json = getAsJSON("/rest/fonts.json");

        //print out the result
        print(json);

        //make assertions
        assertTrue(json instanceof JSONObject);
        assertTrue(((JSONObject) json).get("fonts") instanceof JSONArray);
    }
}
