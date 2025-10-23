/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assume.assumeTrue;

import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.w3c.dom.Document;

public class CogS3RemoteHarvestOnlineTest extends CogRemoteHarvestOnlineTest {

    @Override
    protected String getCogURL() {
        return "https://deafrica-landsat.s3.af-south-1.amazonaws.com/collection02/level-2/standard/tm/2010/200/040/LT05_L2SP_200040_20100123_20200825_02_T1/LT05_L2SP_200040_20100123_20200825_02_T1_QA_RADSAT.TIF";
    }

    @Test
    public void testHarvestRemoteURLInImageMosaic() throws Exception {
        assumeTrue(isOnline());
        byte[] zipData = prepareZipData("empty", "empty");

        // setup store
        createImageMosaicStore("empty", zipData);

        // Harvesting
        harvestGranule("empty", getCogURL());

        // Getting the list of available coverages
        checkExistingCoverages("empty", "empty");

        // Configuring the coverage
        configureCoverage("empty");

        checkGranuleExists("empty", "empty", getCogURL());

        // Harvesting another granule
        harvestGranule(
                "empty",
                "https://deafrica-landsat.s3.af-south-1.amazonaws.com/collection02/level-2/standard/tm/2010/200/040/LT05_L2SP_200040_20100208_20200824_02_T1/LT05_L2SP_200040_20100208_20200824_02_T1_QA_RADSAT.TIF");

        // Check we have 2 granules
        Document dom = getAsDOM(RestBaseController.ROOT_PATH
                + "/workspaces/gs/coveragestores/empty/coverages/empty/index/granules.xml");
        assertXpathEvaluatesTo("2", "count(//gf:empty)", dom);
    }
}
