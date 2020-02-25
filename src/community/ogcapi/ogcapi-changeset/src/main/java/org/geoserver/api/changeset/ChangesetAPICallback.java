/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.io.InputStream;
import org.geoserver.api.GeoServerOpenAPI;
import org.geoserver.api.OpenAPICallback;
import org.geoserver.api.tiles.TilesService;
import org.geoserver.ows.Request;
import org.geoserver.util.IOUtils;
import org.springframework.stereotype.Component;

/** Extends the tiles API with the extra paths/params */
@Component
public class ChangesetAPICallback implements OpenAPICallback {

    private final GeoServerOpenAPI fragment;

    public ChangesetAPICallback() throws IOException {
        String location = "changeset.yml";
        try (InputStream is = ChangesetAPICallback.class.getResourceAsStream(location)) {
            if (is == null) {
                throw new RuntimeException(
                        "Could not find API definition at "
                                + location
                                + " from class "
                                + ChangesetAPICallback.class);
            }
            String specFragment = IOUtils.toString(is);
            this.fragment = Yaml.mapper().readValue(specFragment, GeoServerOpenAPI.class);
        }
    }

    @Override
    public void apply(Request dr, OpenAPI api) {
        if (dr.getServiceDescriptor().getService() instanceof TilesService) {
            // add the multitile
            // TODO: just customize it once added to the Tiles API
            String name = "/collections/{collectionId}/map/{styleId}/tiles/{tileMatrixSetId}";
            PathItem multiTileRenderedPath =
                    fragment.getPaths()
                            .get(
                                    "/collections/{collectionId}/map/{styleId}/tiles"
                                            + "/{tileMatrixSetId}");
            api.getPaths().addPathItem(name, multiTileRenderedPath);

            Parameter fTileParameter = fragment.getComponents().getParameters().get("f-tile");
            api.getComponents().getParameters().put("f-tile", fTileParameter);
            Parameter fParameter = fragment.getComponents().getParameters().get("f-json-zip");
            api.getComponents().getParameters().put("f-json-zip", fTileParameter);
        }
    }
}
