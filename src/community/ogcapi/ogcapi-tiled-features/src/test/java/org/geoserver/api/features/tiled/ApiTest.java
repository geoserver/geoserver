/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features.tiled;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.api.OpenAPIMessageConverter;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiTest extends TiledFeaturesTestSupport {

    @Test
    public void testApiJson() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/features/api", 200);
        assertThat(
                response.getContentType(),
                CoreMatchers.startsWith(OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE));
        String json = response.getContentAsString();
        LOGGER.log(Level.INFO, json);

        ObjectMapper mapper = Json.mapper();
        OpenAPI api = mapper.readValue(json, OpenAPI.class);
        validateApi(api);
    }

    private void validateApi(OpenAPI api) {
        // only one server
        List<Server> servers = api.getServers();
        assertThat(servers, hasSize(1));
        assertThat(
                servers.get(0).getUrl(), equalTo("http://localhost:8080/geoserver/ogc/features"));

        // paths
        Paths paths = api.getPaths();

        // ... tile matrix sets
        PathItem tileMatrixSets = paths.get("/tileMatrixSets");
        assertNotNull(tileMatrixSets);
        assertThat(tileMatrixSets.getGet().getOperationId(), equalTo("getTileMatrixSets"));

        // ... the single tile matrix
        PathItem tileMatrixSet = paths.get("/tileMatrixSets/{tileMatrixSetId}");
        assertNotNull(tileMatrixSet);
        assertThat(tileMatrixSet.getGet().getOperationId(), equalTo("getTileMatrixSetDescription"));

        // ... the tiles themselves
        PathItem tiles =
                paths.get(
                        "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}");
        assertNotNull(tiles);
        assertThat(tiles.getGet().getOperationId(), equalTo("getTileOfCollectionId"));
    }
}
