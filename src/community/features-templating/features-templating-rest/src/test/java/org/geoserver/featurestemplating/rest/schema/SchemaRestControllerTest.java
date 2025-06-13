/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

/** Test for the {@link SchemaRestController} */
public class SchemaRestControllerTest extends CatalogRESTTestSupport {

    private static final String JSON_SCHEMA = "{ \"schemaName\": \"foo\", \"template\": \"foo\" }";
    private static final String JSON_SCHEMA2 = "{ \"schemaName\": \"foo\", \"code\": \"foo\" }";

    @Test
    public void testPostGetPutGetDeleteJSON() throws Exception {
        try {
            MockHttpServletResponse response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/schemaoverrides?schemaName=foo",
                    JSON_SCHEMA,
                    MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH + "/schemaoverrides/foo");
            assertEquals(200, response.getStatus());
            assertEquals(JSON_SCHEMA.trim(), response.getContentAsString());
            response = putAsServletResponse(
                    RestBaseController.ROOT_PATH + "/schemaoverrides/foo",
                    JSON_SCHEMA2,
                    MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            response = getAsServletResponse(RestBaseController.ROOT_PATH + "/schemaoverrides/foo");
            assertEquals(200, response.getStatus());
            assertEquals(JSON_SCHEMA2.trim(), response.getContentAsString());
            // delete transaction validation
            response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/schemaoverrides/foo");
            assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
            assertNull(TemplateInfoDAO.get().findByFullName("cdf:foo"));
        } finally {
            SchemaInfoDAO.get().deleteAll();
        }
    }
}
