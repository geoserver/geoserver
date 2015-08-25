/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StyleGeneratorTest extends CatalogRESTTestSupport {

    @Test
    public void testGetDefault() throws Exception {
        String sld = getAsString( "/rest/stylegenerator" );
        
        assertTrue(sld.contains("StyledLayerDescriptor"));
        assertTrue(sld.contains("<Name>Default style</Name>"));
        
        assertTrue(sld.contains("<PointSymbolizer>"));
        assertTrue(sld.contains("<LineSymbolizer>"));
        assertTrue(sld.contains("<PolygonSymbolizer>"));
        assertTrue(sld.contains("<RasterSymbolizer>"));
    }
    
    @Test
    public void testGetAsSld() throws Exception {
        String sld = getAsString( "/rest/stylegenerator/point.sld" );
        
        assertTrue(sld.contains("StyledLayerDescriptor"));
        assertTrue(sld.contains("<Name>Default style</Name>"));
        
        assertTrue(sld.contains("<PointSymbolizer>"));
        assertFalse(sld.contains("<LineSymbolizer>"));
        assertFalse(sld.contains("<PolygonSymbolizer>"));
        assertFalse(sld.contains("<RasterSymbolizer>"));
    }

}
