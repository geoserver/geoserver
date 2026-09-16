/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Iterators;
import java.util.Collections;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.filter.Filter;
import org.junit.Test;

/** The catalog facade must apply the same security filtering to counting as it does to listing. */
public class SecureCatalogFacadeTest extends GeoServerSystemTestSupport {

    /** Layers left once Forests is denied to the reader. */
    private static final int VISIBLE_TO_READER = 28;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        addUser("reader", "reader", null, Collections.singletonList("ROLE_READER"));
        // Forests is readable by ROLE_OWNER only, a plain reader must not see it
        addLayerAccessRule("cite", "Forests", AccessMode.READ, "ROLE_OWNER");
    }

    @Test
    public void testCountMatchesListing() {
        login("reader", "reader", "ROLE_READER");

        CatalogFacade facade = getCatalog().getFacade();
        int listed;
        try (CloseableIterator<LayerInfo> it = facade.list(LayerInfo.class, Filter.INCLUDE, null, null)) {
            listed = Iterators.size(it);
        }

        assertEquals(VISIBLE_TO_READER, listed);
        assertEquals(VISIBLE_TO_READER, facade.count(LayerInfo.class, Filter.INCLUDE));
    }
}
