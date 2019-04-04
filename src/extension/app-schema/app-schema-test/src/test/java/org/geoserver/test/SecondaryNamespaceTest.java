/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test encoding of secondary (transitively imported) namespace.
 *
 * @author Jacqui Githaiga, Curtin University of Technology
 */
public class SecondaryNamespaceTest extends AbstractAppSchemaTestSupport {

    @Override
    protected SecondaryNamespaceMockData createTestData() {
        return new SecondaryNamespaceMockData();
    }

    /** Test encoding of sa namespace. */
    @Test
    public void testNamespaces() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=ex:ShapeContent");
        LOGGER.info("Secondary Namespace Response:\n" + prettyString(doc));
        assertXpathCount(2, "//sa:shape", doc);
        assertXpathEvaluatesTo(
                "420502.00109583 399739.99000583 420502.00109583 399739.99000583",
                "//ex:ShapeContent[@gml:id='sa.1']/sa:shape/gml:Curve/gml:segments/gml:LineStringSegment/gml:posList",
                doc);
    }
}
