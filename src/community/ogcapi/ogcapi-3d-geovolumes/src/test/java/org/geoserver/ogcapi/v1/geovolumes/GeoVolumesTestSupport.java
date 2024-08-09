/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import java.io.File;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.platform.resource.Resource;

public class GeoVolumesTestSupport extends OGCApiTestSupport {

    public static final double[] NY_BBOX = {
        -74.01900887327089,
        40.700475291581974,
        -11.892070104139751,
        -73.9068954348699,
        40.880256294183646,
        547.7591871983744
    };
    public static final double[] STG_BBOX = {9.161434, 48.771841, -10, 9.183426, 48.786318, 550};

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerDataDirectory dd = getDataDirectory();
        Resource geovolumesResource = dd.get(GeoVolumesProvider.GEOVOLUMES_DEFAULT_DIR);
        File geovolumesDir = geovolumesResource.dir();
        FileUtils.copyFileToDirectory(
                new File("./src/test/resources/collections.json"), geovolumesDir);
    }

    /**
     * Parses a bbox in the format "xmin, ymin, zmin, xmax, ymax, zmax." (with a trailing period)
     */
    protected double[] parseBBOXText(String bbox) {
        // Remove the trailing period
        bbox = bbox.substring(0, bbox.length() - 1);

        // Split the string by commas, trim spaces, and parse to doubles
        return Arrays.stream(bbox.split("\\s*,\\s*")).mapToDouble(Double::parseDouble).toArray();
    }
}
