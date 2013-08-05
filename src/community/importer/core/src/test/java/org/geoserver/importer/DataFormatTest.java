/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class DataFormatTest extends ImporterTestSupport {

    public void testLookupShapefile() {
        DataFormat format = DataFormat.lookup(new File("foo.shp"));
        assertNotNull("No format found for shape files", format);
        String name = format.getName();
        assertEquals("Shapefile format not found", "Shapefile", name);
    }

    public void testLookupTiff() throws Exception {
        File dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        File tif = new File(dir, "EmissiveCampania.tif");
        DataFormat format = DataFormat.lookup(tif);
        assertNotNull("No format found for tif", format);
        String name = format.getName();
        assertEquals("Tif format not found", "GeoTIFF", name);
    }

    public void testLookupCSV() throws Exception {
        DataFormat format = DataFormat.lookup(new File("foo.csv"));
        assertNotNull("No format found for csv files", format);
        String name = format.getName();
        assertEquals("CSV format not found", "CSV", name);
    }

    public void testLookupKML() throws Exception {
        File kmlFile = new File(tmpDir(), "foo.kml");
        FileUtils.touch(kmlFile);

        DataFormat format = DataFormat.lookup(kmlFile);
        assertNotNull("No format found for kml files", format);
        String name = format.getName();
        assertEquals("KML format not found", "KML", name);
    }
}
