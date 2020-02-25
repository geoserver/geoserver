/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.junit.Test;
import org.w3c.dom.Document;

public class MapPPIOTest {

    @Test
    public void testXML() throws Exception {
        Map map = new HashMap();
        map.put("name", "bomb");
        map.put("price", 12.99);

        MapXMLPPIO ppio = new MapXMLPPIO();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ppio.encode(map, bout);

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Document dom = dom(bin);

        assertEquals("map", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("bomb", "/map/name", dom);
        assertXpathEvaluatesTo("12.99", "/map/price", dom);
    }

    @Test
    public void testJSON() throws Exception {
        Map map = new HashMap();
        map.put("name", "bomb");
        map.put("price", 12.99);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new MapJSONPPIO().encode(map, bout);

        JSON json = JSONSerializer.toJSON(new String(bout.toByteArray()));
        JSONObject obj = (JSONObject) json;

        assertEquals("bomb", obj.getString("name"));
        assertEquals(12.99, obj.getDouble("price"), 0.1);
    }

    Document dom(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(input);
    }

    void print(Document document, OutputStream output) {
        try {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.INDENT, "yes");
            tx.transform(new DOMSource(document), new StreamResult(output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
