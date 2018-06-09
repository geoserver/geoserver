/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.solr;

import java.io.InputStream;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class SOLRXStreamInitializerTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Test
    public void testLoadWatermark() throws Exception {
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        try (InputStream is = getClass().getResourceAsStream("xstream-featuretype.xml")) {
            xp.load(is, FeatureTypeInfo.class);
        }
    }
}
