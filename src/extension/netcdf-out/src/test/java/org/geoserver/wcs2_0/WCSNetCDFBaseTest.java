/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.kvp.WCSKVPTestSupport;
import org.geotools.coverage.io.netcdf.crs.NetCDFCRSAuthorityFactory;

public class WCSNetCDFBaseTest extends WCSKVPTestSupport {

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
        final File projectionFile = new File(projectionFileDir, netcdfProjectionsDefinition);
        System.setProperty(
                NetCDFCRSAuthorityFactory.SYSTEM_DEFAULT_USER_PROJ_FILE,
                projectionFile.getCanonicalPath());
        testData.copyTo(
                getClass().getResourceAsStream("reduced-cf-standard-name-table.xml"),
                "cf-standard-name-table.xml");
    }
}
