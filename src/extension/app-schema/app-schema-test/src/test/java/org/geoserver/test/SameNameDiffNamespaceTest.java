/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.NullEntityResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests whether two 'name' properties with different namespace are properly encoded
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class SameNameDiffNamespaceTest extends AbstractAppSchemaTestSupport {

    @BeforeClass
    public static void setUpClass() throws Exception {
        EntityResolverProvider.setEntityResolver(NullEntityResolver.INSTANCE);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        EntityResolverProvider.setEntityResolver(DevModeEntityResolver.INSTANCE);
    }

    @Override
    protected SameNameDiffNamespaceMockData createTestData() {
        return new SameNameDiffNamespaceMockData();
    }

    /** Test GetFeature with two 'name' properties with different namespace. */
    @Test
    public void testSameNameDiffNamespace3() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=ex:MyTestFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/gml:name", doc);
        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/ex:name", doc);
    }
}
