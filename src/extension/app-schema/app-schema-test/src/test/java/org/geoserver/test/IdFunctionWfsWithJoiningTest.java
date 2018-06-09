/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test GEOS-5618: using functions in idExpression with joining. If the function
 * is not translatable to SQL, it is not supported with joining. However, it should work without
 * joining.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class IdFunctionWfsWithJoiningTest extends AbstractAppSchemaTestSupport {

    @Override
    protected IdFunctionMockData createTestData() {
        return new IdFunctionMockData();
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
        String expected =
                "java.lang.UnsupportedOperationException: idExpression 'getID()' for targetElement 'gsml:MappedFeature' cannot be translated into SQL, "
                        + "therefore is not supported with joining!\nPlease make sure idExpression is mapped into existing database fields, "
                        + "and only use functions that are supported by your database."
                        + "\nIf this cannot be helped, you can turn off joining in app-schema.properties file.";
        String exceptionText = evaluate("//ows:ExceptionText", doc);
        assertTrue(exceptionText.startsWith(expected));
    }
}
