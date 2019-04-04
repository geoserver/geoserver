/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.grib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.spi.ImageReaderSpi;
import junit.framework.TestCase;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.io.grib.GRIBFormat;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.imageio.netcdf.NetCDFImageReaderSpi;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.Test;

/** Simple test class for checking if grib data are supported. */
public class GribDataTest extends TestCase {

    @Test
    public void testFormatSupported() throws FileNotFoundException, IOException {
        // Check if the Grib Library is available by calling the NetCDFUtilities.isGribAvailable()
        // method
        Assert.assertTrue(NetCDFUtilities.isGribAvailable());
        // Selection of the input file
        File file = TestData.file(this, "sampleGrib.grb2");
        // Check if the grib file is accepted by the NetCDF driver
        AbstractGridFormat format = new GRIBFormat();
        Assert.assertTrue(format.accepts(file));
        // Check if the netcdf reader spi object can read the input file
        ImageReaderSpi spi = new NetCDFImageReaderSpi();
        Assert.assertTrue(spi.canDecodeInput(file));
    }

    @Test
    public void testImage() throws FileNotFoundException, IOException {
        // Selection of the input file
        File file = TestData.file(this, "sampleGrib.grb2");
        // Creation of a NetCDF reader for the grib data
        NetCDFReader reader = new NetCDFReader(file, null);
        Assert.assertNotNull(reader);

        try {
            // Selection of the coverage names
            String[] coverageNames = reader.getGridCoverageNames();
            // Check if almost one coverage is present
            Assert.assertNotNull(coverageNames);
            Assert.assertTrue(coverageNames.length > 0);
            // Reading of one coverage
            GridCoverage2D coverage = reader.read(coverageNames[0], null);
            // Check if the coverage exists
            Assert.assertNotNull(coverage);
        } finally {
            // Reader disposal
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // nothing
                }
            }
        }
    }
}
