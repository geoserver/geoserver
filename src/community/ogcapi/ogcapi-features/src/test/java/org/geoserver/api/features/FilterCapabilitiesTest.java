/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class FilterCapabilitiesTest extends FeaturesTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Test
    public void testCapabilities() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/filter-capabilities", 200);
    }
}
