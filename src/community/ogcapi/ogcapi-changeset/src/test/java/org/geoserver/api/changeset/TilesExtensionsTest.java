/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.api.OGCApiTestSupport;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class TilesExtensionsTest extends OGCApiTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Test
    public void testApiExtension() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/tiles/api", 200);
        assertThat(response.getContentType(), startsWith("application/openapi+json;version=3.0"));
        String json = response.getContentAsString();
        LOGGER.log(Level.INFO, json);

        ObjectMapper mapper = Json.mapper();
        OpenAPI api = mapper.readValue(json, OpenAPI.class);

        // check the multitile path
        PathItem multiTile =
                api.getPaths()
                        .get("/collections/{collectionId}/map/{styleId}/tiles/{tileMatrixSetId}");
        assertThat(multiTile, notNullValue());
        // check the extra params are in there
        List<String> parameterNames =
                multiTile
                        .getGet()
                        .getParameters()
                        .stream()
                        .map(p -> p.get$ref())
                        .collect(Collectors.toList());
        assertThat(parameterNames, hasItem("#/components/parameters/f-tile"));
        assertThat(parameterNames, hasItem("#/components/parameters/f-json-zip"));

        // check the param definitions are in components
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        assertThat(parameters.get("f-tile"), notNullValue());
        assertThat(parameters.get("f-json-zip"), notNullValue());
    }

    @Test
    public void testMultiTileExtension() throws Exception {}
}
