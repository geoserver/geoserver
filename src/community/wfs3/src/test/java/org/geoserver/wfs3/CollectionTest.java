/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.sf.json.JSON;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class CollectionTest extends WFS3TestSupport {

    @Test
    public void testCollectionJson() throws Exception {
        JSON json = getAsJSON("wfs3/collections/" + getEncodedName(MockData.ROAD_SEGMENTS));
        print(json);
    }

    @Test
    public void testCollectionsXML() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs3/collections/"
                                + getEncodedName(MockData.ROAD_SEGMENTS)
                                + "?f=text/xml");
        // print(dom);
        String expected =
                "http://localhost:8080/geoserver/wfs3/cite__RoadSegments/items?f=application%2Fjson";
        XMLAssert.assertXpathEvaluatesTo(
                expected,
                "//wfs:Collection[wfs:Name='cite__RoadSegments']/atom:link[@atom:type='application/json']/@atom:href",
                dom);
    }

    @Test
    public void testCollectionYaml() throws Exception {
        String yaml =
                getAsString(
                        "wfs3/collections/"
                                + getEncodedName(MockData.ROAD_SEGMENTS)
                                + "?f=application/x-yaml");
        System.out.println(yaml);
    }
}
