/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.netcdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class NetCDFCRSOverridingAuthorityFactoryTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        String netcdfProjectionsDefinition = "netcdf.projections.properties";
        File projectionFileDir = new File(testData.getDataDirectoryRoot(), "user_projections");
        if (!projectionFileDir.mkdir()) {
            FileUtils.deleteDirectory(projectionFileDir);
            assertTrue(
                    "Unable to create projection dir: " + projectionFileDir,
                    projectionFileDir.mkdir());
        }
        testData.copyTo(
                getClass().getResourceAsStream(netcdfProjectionsDefinition),
                "user_projections/" + netcdfProjectionsDefinition);
    }

    @Test
    public void testCRSOverridingFactory() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:971801");
        assertNotNull(crs);
        Integer epsgCode = CRS.lookupEpsgCode(crs, false);
        assertEquals(971801, epsgCode.intValue());
    }
}
