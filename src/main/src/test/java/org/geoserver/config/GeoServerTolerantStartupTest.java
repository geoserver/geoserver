package org.geoserver.config;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class GeoServerTolerantStartupTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testReloadWithRuinedCoverageStore() throws Exception {
        // ruin one coverage description
        File root = getDataDirectory().getRoot().dir();
        File targetCoverage = new File(root, "workspaces/wcs/BlueMarble/coveragestore.xml");
        FileUtils.writeStringToFile(targetCoverage, "boom!", "UTF-8");

        // reload and check it does not go belly up
        getGeoServer().reload();

        // check the coverage in question is no more
        getCatalog().getCoverageByName(getLayerId(MockData.TASMANIA_BM));
    }
}
