/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.netcdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.spi.ImageReaderSpi;
import junit.framework.TestCase;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.io.netcdf.NetCDFFormat;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.imageio.netcdf.NetCDFImageReaderSpi;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.Test;

/** Simple test class for checking if netcdf data are supported. */
public class NetCDFDataTest extends TestCase {

    @Test
    public void testFormatSupported() throws FileNotFoundException, IOException {

        // Selection of the input file
        File file = TestData.file(this, "2DLatLonCoverage.nc");
        // Check if the grib file is accepted by the NetCDF driver
        AbstractGridFormat format = new NetCDFFormat();
        Assert.assertTrue(format.accepts(file));
        // Check if the netcdf reader spi object can read the input file
        ImageReaderSpi spi = new NetCDFImageReaderSpi();
        Assert.assertTrue(spi.canDecodeInput(file));
    }

    @Test
    public void testImage() throws FileNotFoundException, IOException {
        // Selection of the input file
        File file = TestData.file(this, "2DLatLonCoverage.nc");
        // Creation of a NetCDF reader for the data
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
