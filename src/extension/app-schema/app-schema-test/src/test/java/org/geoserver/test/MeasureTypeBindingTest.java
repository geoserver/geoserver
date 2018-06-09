/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** @author Xiangtan Lin, CSIRO Information Management and Technology */
public class MeasureTypeBindingTest extends AbstractAppSchemaTestSupport {

    @Override
    protected MeasureTypeBindingTestMockData createTestData() {
        return new MeasureTypeBindingTestMockData();
    }

    /**
     * This is to test MeasureTypeBinding without 'uom' in app-schema. GeoServer should encode
     * output without error (https://osgeo-org.atlassian.net/browse/GEOT-1272)
     */
    @Test
    public void testMeasureTypeBindingWithoutUOM() {
        String path = "wfs?request=GetFeature&version=1.1.0&typename=ex:PolymorphicFeature";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "WFS GetFeature&typename=ex:PolymorphicFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//ex:PolymorphicFeature", doc);

        Node feature = doc.getElementsByTagName("ex:PolymorphicFeature").item(0);
        assertEquals("ex:PolymorphicFeature", feature.getNodeName());

        // gml:id
        assertXpathEvaluatesTo("f1", "//ex:PolymorphicFeature/@gml:id", doc);

        // firstValue
        Node firstValue = feature.getFirstChild();
        assertEquals("ex:firstValue", firstValue.getNodeName());
        Node cgi_numericValue = firstValue.getFirstChild();
        assertEquals("gsml:CGI_NumericValue", cgi_numericValue.getNodeName());
        assertEquals("1.0", cgi_numericValue.getFirstChild().getFirstChild().getNodeValue());

        // secondValue
        Node secondValue = firstValue.getNextSibling();
        assertEquals("ex:secondValue", secondValue.getNodeName());
        cgi_numericValue = secondValue.getFirstChild();
        assertEquals("gsml:CGI_NumericValue", cgi_numericValue.getNodeName());
        assertEquals("1.0", cgi_numericValue.getFirstChild().getFirstChild().getNodeValue());

        // thirdValue
        Node thirdValue = secondValue.getNextSibling();
        assertEquals("ex:thirdValue", thirdValue.getNodeName());
        cgi_numericValue = thirdValue.getFirstChild();
        assertEquals("gsml:CGI_NumericValue", cgi_numericValue.getNodeName());
        assertEquals("1.0", cgi_numericValue.getFirstChild().getFirstChild().getNodeValue());

        // fourthValue
        Node fourthValue = thirdValue.getNextSibling();
        assertEquals("ex:fourthValue", fourthValue.getNodeName());
        cgi_numericValue = fourthValue.getFirstChild();
        assertEquals("gsml:CGI_NumericValue", cgi_numericValue.getNodeName());
        assertEquals("1.0", cgi_numericValue.getFirstChild().getFirstChild().getNodeValue());

        // if 'uom' is not set, schema validation fails.
        // validateGet(path);
    }
}
