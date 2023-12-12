/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.process.vector.GraticuleLabelTestSupport;
import org.junit.Test;

public class GraticuleDataStoreTest extends GraticuleLabelTestSupport {

    @Test
    public void testCreation() throws Exception {

        String[] names = store.getTypeNames();
        assertEquals(1, names.length);
        SimpleFeatureType schema = store.getSchema("10_0");
        assertNotNull(schema);
    }
}
