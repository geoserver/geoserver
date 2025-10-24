/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/** Similar to the {@link CogRemoteHarvestOnlineTest} but testing Google Storage */
public class CogGSRemoteHarvestOnlineTest extends CogRemoteHarvestOnlineTest {

    // Landsat image courtesy of the U.S. Geological Survey
    private static final String GS_COG_URI = "gs://gcp-public-data-landsat/LC08/01/044/034"
            + "/LC08_L1GT_044034_20130330_20170310_01_T2"
            + "/LC08_L1GT_044034_20130330_20170310_01_T2_B11.TIF";

    @Override
    protected String getCogURL() {
        // Landsat image courtesy of the U.S. Geological Survey
        return "https://storage.googleapis"
                + ".com/gcp-public-data-landsat/LC08/01/044/034"
                + "/LC08_L1GT_044034_20130330_20170310_01_T2"
                + "/LC08_L1GT_044034_20130330_20170310_01_T2_B11.TIF";
    }

    @Test
    public void testHarvestURI() throws Exception {
        assumeTrue(isOnline());
        byte[] zipData = prepareZipData("gs_uri_empty", "gsempty");

        // setup store
        createImageMosaicStore("empty", zipData);

        // Harvesting
        harvestGranule("empty", GS_COG_URI);

        // Getting the list of available coverages
        checkExistingCoverages("empty", "empty");

        // Configuring the coverage
        configureCoverage("empty");

        // Checking granules
        checkGranuleExists("empty", "empty", GS_COG_URI);
    }

    @Test
    public void testHarvestHTTPS() throws Exception {
        assumeTrue(isOnline());
        byte[] zipData = prepareZipData("gs_empty2", "gsempty2");

        // setup store
        createImageMosaicStore("empty2", zipData);

        // Harvesting
        harvestGranule("empty2", getCogURL());

        // Getting the list of available coverages
        checkExistingCoverages("empty2", "empty2");

        // Configuring the coverage
        configureCoverage("empty2");

        // Checking granules
        checkGranuleExists("empty2", "empty2", getCogURL());
    }
}
