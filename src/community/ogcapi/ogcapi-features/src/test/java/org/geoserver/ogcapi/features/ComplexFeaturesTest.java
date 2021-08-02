/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features;

import java.io.IOException;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;
import org.w3c.dom.Document;

public class ComplexFeaturesTest extends AbstractAppSchemaTestSupport {
    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Test
    public void testHTMLMappedFeature() throws IOException {
        Document doc = getAsDOM("ogc/features/collections/gsml:MappedFeature/items?f=text/html");

        // all the five root feature are present
        assertXpathCount(5, "//ul[@id='rootUL']", doc);
        // nested features are present
        assertXpathCount(4, "//li[span='GeologicUnitType']", doc);
        assertXpathCount(6, "//li[span='CompositionPartType']", doc);
    }
}
