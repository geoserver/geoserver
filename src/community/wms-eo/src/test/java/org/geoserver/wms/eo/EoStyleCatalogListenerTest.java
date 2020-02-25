/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/** @author Davide Savazzi - geo-solutions.it */
public class EoStyleCatalogListenerTest extends GeoServerSystemTestSupport {

    private String[] getStyleNames() {
        return EoStyleCatalogListener.EO_STYLE_NAMES;
    }

    @Test
    public void testStylesExist() {
        Catalog catalog = getCatalog();
        for (String styleName : getStyleNames()) {
            assertNotNull(catalog.getStyleByName(styleName));
        }
    }

    @Test
    public void testDelete() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(getStyleNames()[0]);
        assertNotNull(style);
        catalog.remove(style);
        // style should have been recreated
        assertNotNull(catalog.getStyleByName(getStyleNames()[0]));
    }
}
