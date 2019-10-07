/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;

public class ImagesTestSupport extends OGCApiTestSupport {

    protected static QName WATER_TEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static QName WATER_TEMP_DEFAULT =
            new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);

    public static final String WATER_TEMP_TITLE = "Water temperature";
    public static final String WATER_TEMP_DESCRIPTION = "I love my water warm and cosy!";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // these will be ignored, not structured coverages
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();

        // a raster layer with time and elevation
        testData.addRasterLayer(
                WATER_TEMP, "watertemp.zip", null, null, SystemTestData.class, catalog);

        // another copy in different workspace, to check ws specific services
        testData.addRasterLayer(
                WATER_TEMP_DEFAULT, "watertemp.zip", null, null, SystemTestData.class, catalog);

        // add a description for the water temp layer
        CoverageInfo waterTemp = getCatalog().getCoverageByName(getLayerId(WATER_TEMP));
        waterTemp.setTitle(WATER_TEMP_TITLE);
        waterTemp.setAbstract(WATER_TEMP_DESCRIPTION);
        getCatalog().save(waterTemp);
    }

    /**
     * Returns all {@link CoverageInfo} available in the catalog that are backed by a {@link
     * StructuredGridCoverage2DReader}
     */
    protected Stream<CoverageInfo> getStructuredCoverages() {
        return Streams.stream(getCatalog().getCoverages())
                .filter(
                        c -> {
                            try {
                                return c.getGridCoverageReader(null, null)
                                        instanceof StructuredGridCoverage2DReader;
                            } catch (IOException e) {
                                return false;
                            }
                        });
    }
}
