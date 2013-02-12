/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
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
    public void testModify() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(getStyleNames()[0]);
        style.setName("wmseo-danger");
        try {
            catalog.save(style);
            fail("style is read-only");
        } catch (CatalogException e) {            
        }
        
        assertNotNull(catalog.getStyleByName(getStyleNames()[0]));
        assertNull(catalog.getStyleByName("wmseo-danger"));
    }
    
    @Test
    public void testDelete() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(getStyleNames()[0]);
        catalog.remove(style);
        // style should have been recreated
        assertNotNull(catalog.getStyleByName(getStyleNames()[0]));
    }    
}