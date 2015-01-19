/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.Query;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

public class ImportProcessTest extends WPSTestSupport {
    
    @After 
    public void removeNewLayers() {
        removeLayer(SystemTestData.CITE_PREFIX, "Buildings2");
    }

    /**
     * Try to re-import buildings as another layer (different name, different projection)
     */
    @Test
    public void testImportBuildings() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource = (SimpleFeatureCollection) ti.getFeatureSource(null,
                null).getFeatures();
        ForceCoordinateSystemFeatureResults forced = new ForceCoordinateSystemFeatureResults(
                rawSource, CRS.decode("EPSG:4326"));

        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(forced, null, SystemTestData.CITE_PREFIX, SystemTestData.CITE_PREFIX,
                "Buildings2", null, null, null);

        checkBuildings(result,"Buildings2");
    }
    
    /**
     * Try to re-import buildings as another layer (different name, different projection)
     */
    @Test
    public void testImportBuildingsForceCRS() throws Exception {
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));
        SimpleFeatureCollection rawSource = (SimpleFeatureCollection) ti.getFeatureSource(null,
                null).getFeatures();

        ImportProcess importer = new ImportProcess(getCatalog());
        String result = importer.execute(rawSource, null, SystemTestData.CITE_PREFIX, SystemTestData.CITE_PREFIX,
                "Buildings3", CRS.decode("EPSG:4326"), null, null);

        checkBuildings(result,"Buildings3");
    }


	private void checkBuildings(String result,String expected) throws IOException {
		assertEquals(SystemTestData.CITE_PREFIX + ":" + expected, result);

            // check the layer
            LayerInfo layer = getCatalog().getLayerByName(result);
            assertNotNull(layer);
            assertEquals("polygon", layer.getDefaultStyle().getName());
    
            // check the feature type info
            FeatureTypeInfo fti = (FeatureTypeInfo) layer.getResource();
            assertEquals("EPSG:4326", fti.getSRS());
            SimpleFeatureSource fs = (SimpleFeatureSource) fti.getFeatureSource(null, null);
            assertEquals(2, fs.getCount(Query.ALL));
    
            // _=the_geom:MultiPolygon,FID:String,ADDRESS:String
            // Buildings.1107531701010=MULTIPOLYGON (((0.0008 0.0005, 0.0008 0.0007,
            // 0.0012 0.0007, 0.0012 0.0005, 0.0008 0.0005)))|113|123 Main Street
            // Buildings.1107531701011=MULTIPOLYGON (((0.002 0.0008, 0.002 0.001,
            // 0.0024 0.001, 0.0024 0.0008, 0.002 0.0008)))|114|215 Main Street
    
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
    
            SimpleFeatureIterator fi = fs.getFeatures(ff.equals(ff.property("FID"), ff.literal("113")))
                    .features();
            SimpleFeature f = fi.next();
            fi.close();
            assertEquals("113", f.getAttribute("FID"));
            assertEquals("123 Main Street", f.getAttribute("ADDRESS"));
    
            fi = fs.getFeatures(ff.equals(ff.property("FID"), ff.literal("114"))).features();
            f = fi.next();
            fi.close();
            assertEquals("114", f.getAttribute("FID"));
            assertEquals("215 Main Street", f.getAttribute("ADDRESS"));
	}
}
