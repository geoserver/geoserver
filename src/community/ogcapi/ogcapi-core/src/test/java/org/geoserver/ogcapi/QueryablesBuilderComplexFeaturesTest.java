/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertTrue;

import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.junit.Test;

public class QueryablesBuilderComplexFeaturesTest extends AbstractAppSchemaTestSupport {
    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Test
    public void testQueryablesBuilder() throws Exception {
        QueryablesBuilder qb =
                new QueryablesBuilder("id")
                        .forType(getCatalog().getFeatureTypeByName("MappedFeature"));
        assertTrue(qb.queryables.getProperties().containsKey("observationMethod"));
    }
}
