/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class ExceptionHandlingTest extends STACTestSupport {

    @Test
    public void testCollectionNotFound() throws Exception {
        String path = "ogc/stac/v1/collections/NonExistentCollection";
        DocumentContext json = getAsJSONPath(path, 404);
        assertEquals("NotFound", json.read("code"));
        assertEquals("Collection not found: NonExistentCollection", json.read("description"));
    }

    @Test
    public void testInvalidResource() throws Exception {
        // the Features exception handler might have failed here, the service is not yet determined
        String path = "ogc/stac/v1/notThere";
        DocumentContext json = getAsJSONPath(path, 404);
        assertEquals("ResourceNotFound", json.read("code"));
        assertEquals(
                "No mapping for GET /geoserver/ogc/stac/v1/notThere", json.read("description"));
    }
}
