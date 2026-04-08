/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/** Similar to the {@link CogRemoteHarvestOnlineTest} but testing Azure Blobs */
public class CogAzureRemoteHarvestOnlineTest extends CogRemoteHarvestOnlineTest {

    @Override
    protected String getCogURL() {
        return "https://cogtestdata.blob.core.windows.net/cogtestdata/land_topo_cog_jpeg_1024.tif";
    }

    @Test
    public void testHarvestHTTPS() throws Exception {
        assumeTrue(isOnline());
        byte[] zipData = prepareZipData("azure_https_empty", "azureempty");

        // setup store
        createImageMosaicStore("empty", zipData);

        // Harvesting
        harvestGranule("empty", getCogURL());

        // Getting the list of available coverages
        checkExistingCoverages("empty", "empty");

        // Configuring the coverage
        configureCoverage("empty");

        // Checking granules
        checkGranuleExists("empty", "empty", getCogURL());
    }
}
