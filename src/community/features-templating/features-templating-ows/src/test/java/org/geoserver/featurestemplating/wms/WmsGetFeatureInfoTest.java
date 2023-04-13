/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.wms;

import static org.junit.Assert.assertFalse;

import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.WmsSupportMockData;
import org.junit.Test;

/** GetFeatureInfo tests for feature templating module */
public class WmsGetFeatureInfoTest extends AbstractAppSchemaTestSupport {

    private static final String URL =
            "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100";

    @Override
    protected WmsSupportMockData createTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("positionalaccuracy21", "styles/positionalaccuracy21.sld");
        return mockData;
    }

    /**
     * Checks if submitting features templating request without format doesn't throw an exception
     *
     * @throws Exception
     */
    @Test
    public void testGetFeatureInfoEmptyFormat() throws Exception {
        String response = getAsString(URL);
        assertFalse(response.contains("NullPointerException"));
    }

    /**
     * Checks if submitting features templating request with not supported template format doesn't
     * throw an exception
     *
     * @throws Exception
     */
    @Test
    public void testGetFeatureInfoTextPlain() throws Exception {
        String response = getAsString(URL + "&INFO_FORMAT=text/plain");
        assertFalse(response.contains("NullPointerException"));
    }
}
