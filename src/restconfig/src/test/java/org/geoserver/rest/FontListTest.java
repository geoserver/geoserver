/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import static org.junit.Assert.*;

/**
 * Class with FontListResource tests
 *
 * @author Jose Garca
 */
public class FontListTest extends GeoServerSystemTestSupport {

    @Test
    public void testGetAsXML() throws Exception {
        //make the request, parsing the result as a dom
        Document dom = getAsDOM("/rest/fonts.xml");

        //make assertions
        Node fonts = getFirstElementByTagName(dom, "fonts");
        assertNotNull(fonts);
        assertTrue( ((Element) fonts).getElementsByTagName("entry").getLength()  > 0);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        //make the request, parsing the result into a json object
        JSON json = getAsJSON("/rest/fonts.json");

        //make assertions
        assertTrue(json instanceof JSONObject);
        assertTrue(((JSONObject) json).get("fonts") instanceof JSONArray);
    }
}
