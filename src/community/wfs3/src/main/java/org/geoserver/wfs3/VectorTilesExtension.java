/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.InputStream;
import java.util.Map;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;

/** WFS3 extension adding support for the vector tiling OpenAPI paths */
public class VectorTilesExtension implements WFS3Extension {

    private static final String TILING_SPECIFICATION;
    static final String TILING_SCHEMES_PATH = "/tilingSchemes";
    static final String TILING_SCHEME_PATH = "/tilingSchemes/{tilingSchemeId}";
    static final String TILES_PATH =
            "/collections/{collectionId}/tiles/{tilingSchemeId}/{zoomLevel}/{row}/{column}";

    static {
        try (InputStream is = VectorTilesExtension.class.getResourceAsStream("tiling.yml")) {
            TILING_SPECIFICATION = IOUtils.toString(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the tiling.yaml template", e);
        }
    }

    @Override
    public void extendAPI(OpenAPI api) {
        // load the pre-cooked building blocks
        OpenAPI tileAPITemplate = readTemplate();

        // customize paths
        Paths paths = api.getPaths();
        paths.addPathItem(TILING_SCHEMES_PATH, tileAPITemplate.getPaths().get(TILING_SCHEMES_PATH));
        paths.addPathItem(
                TILING_SCHEME_PATH,
                tileAPITemplate.getPaths().get("/tilingSchemes/{tilingSchemeId}"));
        paths.addPathItem(
                TILING_SCHEME_PATH,
                tileAPITemplate.getPaths().get("/tilingSchemes/{tilingSchemeId}"));
        paths.addPathItem(TILES_PATH, tileAPITemplate.getPaths().get(TILES_PATH));

        // and add all schemas and parameters
        Components apiComponents = api.getComponents();
        Components tileComponents = tileAPITemplate.getComponents();
        Map<String, Schema> tileSchemas = tileComponents.getSchemas();
        apiComponents.getSchemas().putAll(tileSchemas);
        Map<String, Parameter> tileParameters = tileComponents.getParameters();
        apiComponents.getParameters().putAll(tileParameters);
    }

    /**
     * Reads the template to customize (each time, as the object tree is not thread safe nor
     * cloneable not serializable)
     */
    private OpenAPI readTemplate() {
        try {
            return Yaml.mapper().readValue(TILING_SPECIFICATION, OpenAPI.class);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
