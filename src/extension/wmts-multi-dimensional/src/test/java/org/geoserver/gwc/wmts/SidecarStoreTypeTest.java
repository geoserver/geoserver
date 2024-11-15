/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.SIDECAR_STORE;

import java.io.IOException;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;

/**
 * Same as SidecarTypeTest, but with a setup wit two different stores, one for the main type, one
 * for the sidecar type. Since property data store returns all properties found in the same
 * directory, we'll use two separate directories here
 */
public class SidecarStoreTypeTest extends SidecarTypeTest {

    public static String MAINSTORE_PREFIX = "mainstore";

    public static String MAINSTORE_URI = "http://www.opengis.net/mainstore";

    public static String SIDESTORE_PREFIX = "sidestore";

    public static String SIDESTORE_URI = "http://www.opengis.net/sidestore";

    public static final QName VECTOR_ELEVATION_TIME_SS =
            new QName(MAINSTORE_URI, "ElevationWithStartEnd", MAINSTORE_PREFIX);

    public static final QName SIDECAR_VECTOR_ET_SS =
            new QName(SIDESTORE_URI, "SidecarTimeElevationWithStartEnd", SIDESTORE_PREFIX);

    @Override
    protected void afterSetup(SystemTestData testData) throws IOException {

        testData.addVectorLayer(
                VECTOR_ELEVATION_TIME_SS,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        testData.addVectorLayer(
                SIDECAR_VECTOR_ET_SS,
                Collections.emptyMap(),
                "/SidecarTimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());

        // registering elevation and time dimensions for a vector
        FeatureTypeInfo vectorInfo =
                getCatalog()
                        .getFeatureTypeByName(
                                MAINSTORE_URI, VECTOR_ELEVATION_TIME_SS.getLocalPart());
        registerLayerDimension(
                vectorInfo,
                ResourceInfo.ELEVATION,
                "startElevation",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());
        registerLayerDimension(
                vectorInfo,
                ResourceInfo.TIME,
                "startTime",
                DimensionPresentation.LIST,
                minimumValue());
    }

    @Override
    protected String getTestLayerId() {
        return getLayerId(VECTOR_ELEVATION_TIME_SS);
    }

    @Override
    protected void setupVectorSidecar() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo vector = catalog.getFeatureTypeByName(getTestLayerId());
        vector.getMetadata()
                .put(MultiDimensionalExtension.SIDECAR_TYPE, SIDECAR_VECTOR_ET_SS.getLocalPart());
        vector.getMetadata().put(SIDECAR_STORE, SIDESTORE_PREFIX + ":" + SIDESTORE_PREFIX);
        catalog.save(vector);
    }
}
