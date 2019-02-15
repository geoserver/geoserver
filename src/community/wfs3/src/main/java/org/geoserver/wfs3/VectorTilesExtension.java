/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import java.io.InputStream;
import java.util.Collections;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs3.response.CollectionDocument;
import org.geoserver.wfs3.response.Link;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;

/** WFS3 extension adding support for the vector tiling OpenAPI paths */
public class VectorTilesExtension extends AbstractWFS3Extension {

    private static final String TILING_SPECIFICATION;
    static final String TILING_SCHEMES_PATH = "/tilingSchemes";
    static final String TILING_SCHEME_PATH = "/tilingSchemes/{tilingSchemeId}";
    static final String TILES_PATH =
            "/collections/{collectionId}/tiles/{tilingSchemeId}/{zoomLevel}/{row}/{column}";
    static final String COLLECTION_TILES_SCHEMES_PATH = "/collections/{collectionId}/tiles";

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
        OpenAPI tileAPITemplate = readTemplate(TILING_SPECIFICATION);

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
        paths.addPathItem(
                COLLECTION_TILES_SCHEMES_PATH,
                tileAPITemplate.getPaths().get(COLLECTION_TILES_SCHEMES_PATH));

        addSchemasAndParameters(api, tileAPITemplate);
    }

    @Override
    public void extendCollection(CollectionDocument collection, BaseRequest request) {
        String collectionId = collection.getName();

        // links
        String baseUrl = request.getBaseUrl();
        String tilingSchemeURL =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/" + collectionId + "/tiles/{tilingSchemeId}",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                tilingSchemeURL,
                                "tilingScheme",
                                MapBoxTileBuilderFactory.MIME_TYPE,
                                collectionId
                                        + " associated tiling schemes. The link is a URI template \"\n"
                                        + "                                        + \"where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' resource",
                                "items"));

        String tilesURL =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/"
                                + collectionId
                                + "/tiles/{tilingSchemeId}/{level}/{row}/{col}",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                tilesURL,
                                "tiles",
                                MapBoxTileBuilderFactory.MIME_TYPE,
                                collectionId
                                        + " as Mapbox vector tiles. The link is a URI template "
                                        + "where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' resource, and {level}/{row}/{col} the tile based on the tiling scheme.",
                                "items"));

        String tilingSchemesURL =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/" + collectionId + "/tiles",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                tilingSchemesURL,
                                "tilingSchemes",
                                "application/json",
                                collectionId
                                        + " associated tiling schemes. The link is a URI template \"\n"
                                        + "                                        + \"where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' resource",
                                "items"));
    }
}
