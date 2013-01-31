/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
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


public class EoStyleCatalogListenerTest extends GeoServerSystemTestSupport {

    private String[] getStyleNames() {
        return EoStyleCatalogListener.STYLE_NAMES;
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