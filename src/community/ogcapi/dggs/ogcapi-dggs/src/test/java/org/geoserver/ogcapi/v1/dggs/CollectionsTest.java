/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class CollectionsTest extends DGGSTestSupport {

    @Test
    public void testCollectionsJson() throws Exception {
        getAsJSONPath("ogc/dggs/v1/collections", 200);
    }

    @Test
    public void testCollectionJsonFormatOverridesBrowserAcceptHeader() throws Exception {
        String collectionId = getCatalog().getDefaultWorkspace().getName() + ":H3";
        MockHttpServletRequest request = createRequest("ogc/dggs/v1/collections/" + collectionId + "?f=json");
        request.setMethod("GET");
        request.addHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        MockHttpServletResponse response = dispatch(request);

        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), containsString("application/json"));
        assertEquals(collectionId, getAsJSONPath(response).read("id"));
    }

    @Test
    public void testCollectionHtmlFormat() throws Exception {
        String collectionId = getCatalog().getDefaultWorkspace().getName() + ":H3";
        MockHttpServletResponse response = getAsServletResponse("ogc/dggs/v1/collections/" + collectionId + "?f=html");

        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), containsString("text/html"));
    }

    @Test
    public void testByteResolutionConversion() {
        assertEquals(3, CollectionDocument.toResolution(Byte.valueOf((byte) 3)));
    }
}
