/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.h2;

import static org.junit.Assert.*;

import java.util.HashMap;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.h2.H2DataStoreFactory;
import org.junit.Test;
import org.vfny.geoserver.util.DataStoreUtils;

public class H2DataStoreInitializerTest extends GeoServerSystemTestSupport {

    @Test
    public void testDataStoreFactoryInitialized() {
        HashMap params = new HashMap();
        params.put(H2DataStoreFactory.DBTYPE.key, "h2");
        params.put(H2DataStoreFactory.DATABASE.key, "test");

        DataAccessFactory f = DataStoreUtils.aquireFactory(params);
        assertNotNull(f);
        assertTrue(f instanceof H2DataStoreFactory);

        assertEquals(testData.getDataDirectoryRoot(), ((H2DataStoreFactory) f).getBaseDirectory());
    }
}
