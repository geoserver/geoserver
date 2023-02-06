/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class FunctionsTest extends FeaturesTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Test
    public void testCapabilities() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/functions", 200);

        // test one random function
        DocumentContext function = readSingleContext(json, "functions[?(@.name=='strSubstring')]");
        assertEquals("substring", function.read("returns.title"));
        assertEquals("string", readSingle(function, "returns.type"));
        assertEquals("string", function.read("arguments[0].title"));
        assertEquals("string", readSingle(function, "arguments[0].type"));
        assertEquals("beginIndex", function.read("arguments[1].title"));
        assertEquals("integer", readSingle(function, "arguments[1].type"));
        assertEquals("endIndex", function.read("arguments[2].title"));
        assertEquals("integer", readSingle(function, "arguments[2].type"));
    }
}
