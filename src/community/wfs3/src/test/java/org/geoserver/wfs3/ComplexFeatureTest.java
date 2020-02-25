/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.StationsMockData;
import org.junit.Test;

public class ComplexFeatureTest extends WFS3TestSupport {

    @Override
    protected SystemTestData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    /** Helper class that will setup custom complex feature types using the stations data set. */
    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            putNamespace(MEASUREMENTS_PREFIX_GML31, MEASUREMENTS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.2 feature types
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML32,
                    "gml32",
                    "Station_gml32",
                    "/test-data/stations/geoJson/stations.xml",
                    getGml32StandardParamaters(),
                    "/test-data/stations/geoJson/measurements.xml",
                    "/test-data/stations/geoJson/stations.xsd",
                    "/test-data/stations/geoJson/stations.properties",
                    "/test-data/stations/geoJson/measurements.properties");
        }
    }

    @Test
    public void testSingleComplexFeature() throws Exception {
        // get a single feature (used not to generate anything valid)
        DocumentContext response =
                getAsJSONPath("wfs3/collections/st_gml32__Station_gml32/items/st.1", 200);
        assertEquals("Feature", response.read("$.type"));
        assertEquals("st.1", response.read("$.id"));
        // links are the extra bit that normally is not there, check that they exist
        assertNotNull(response.read("$.links"));
    }
}
