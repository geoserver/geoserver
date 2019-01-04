/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class DataFormatTest extends ImporterTestSupport {

    @Test
    public void testLookupShapefile() throws Exception {
        DataFormat format = DataFormat.lookup(new File(tmpDir(), "foo.shp"));
        assertNotNull("No format found for shape files", format);
        String name = format.getName();
        assertEquals("Shapefile format not found", "Shapefile", name);
    }

    @Test
    public void testLookupTiff() throws Exception {
        File dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        File tif = new File(dir, "EmissiveCampania.tif");
        DataFormat format = DataFormat.lookup(tif);
        assertNotNull("No format found for tif", format);
        String name = format.getName();
        assertEquals("Tif format not found", "GeoTIFF", name);
    }

    @Test
    public void testLookupCSV() throws Exception {
        DataFormat format = DataFormat.lookup(new File(tmpDir(), "foo.csv"));
        assertNotNull("No format found for csv files", format);
        String name = format.getName();
        assertEquals("CSV format not found", "CSV", name);
    }

    @Test
    public void testLookupKML() throws Exception {
        File kmlFile = new File(tmpDir(), "foo.kml");
        FileUtils.touch(kmlFile);

        DataFormat format = DataFormat.lookup(kmlFile);
        assertNotNull("No format found for kml files", format);
        String name = format.getName();
        assertEquals("KML format not found", "KML", name);
    }
}
