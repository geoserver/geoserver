/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import com.google.common.base.Predicates;
import com.google.common.collect.Streams;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.OpenAPIBuilder;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;

/** Builds the OpenAPI definition for the tiles service */
public class TilesAPIBuilder extends OpenAPIBuilder<TilesServiceInfo> {

    private static final Logger LOGGER = Logging.getLogger(TilesAPIBuilder.class);

    private final GWC gwc;

    public TilesAPIBuilder(GWC gwc) {
        super(TilesServiceInfo.class, "openapi.yaml", "Tiles API", TilesService.class);
        this.gwc = gwc;
    }

    @Override
    @SuppressWarnings("unchecked") // getSchema not generified
    public OpenAPI build(TilesServiceInfo service) throws IOException {
        OpenAPI api = super.build(service);

        // adjust path output formats
        declareGetResponseFormats(api, "/collections", TiledCollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", TiledCollectionsDocument.class);

        // the external documentation
        api.externalDocs(
                new ExternalDocumentation().description("Tiles specification").url("https://ogcapi.ogc.org/tiles/"));

        // adapt the potential formats for raster and vector tiles
        Set<String> formats = Arrays.stream(PublishedType.values())
                .map(t -> GWC.getAdvertisedCachedFormats(t))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        Set<String> rasterFormats =
                formats.stream().filter(f -> isRasterFormat(f)).collect(Collectors.toSet());
        Set<String> vectorFormats =
                formats.stream().filter(f -> isVectorFormat(f)).collect(Collectors.toSet());

        // map tile formats
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        Parameter mapTileFormats = parameters.get("f-map-tile");
        List mapTileFormatsEnum = mapTileFormats.getSchema().getEnum();
        mapTileFormatsEnum.clear();
        mapTileFormatsEnum.addAll(rasterFormats);

        // vector tile formats
        Parameter vectorTileFormats = parameters.get("f-vector-tile");
        List vectorTileFormatsEnum = vectorTileFormats.getSchema().getEnum();
        vectorTileFormatsEnum.clear();
        vectorTileFormatsEnum.addAll(vectorFormats);

        // provide a list of valid values for collectionId
        parameters.get("collectionId").getSchema().setEnum(getCollectionIds(Predicates.alwaysTrue()));

        // now a list only for raster tile layers
        parameters.get("mapCollectionId").getSchema().setEnum(getCollectionIds(getPredicateForFormats(rasterFormats)));

        // now a list only for vector tile layers
        parameters
                .get("vectorCollectionId")
                .getSchema()
                .setEnum(getCollectionIds(getPredicateForFormats(vectorFormats)));

        return api;
    }

    private static Predicate<TileLayer> getPredicateForFormats(Set<String> rasterFormats) {
        return t -> t.getMimeTypes().stream().map(m -> m.getMimeType()).anyMatch(m -> rasterFormats.contains(m));
    }

    private List<String> getCollectionIds(Predicate<TileLayer> filter) {
        return Streams.stream(gwc.getTileLayers())
                .filter(filter)
                .collect(Collectors.toList());
    }

    private static boolean isRasterFormat(String f) {
        try {
            return !MimeType.createFromFormat(f).isVector();
        } catch (MimeException e) {
            LOGGER.log(Level.FINE, "Error checking if format is raster", e);
            return false;
        }
    }

    private static boolean isVectorFormat(String f) {
        try {
            return MimeType.createFromFormat(f).isVector();
        } catch (MimeException e) {
            LOGGER.log(Level.FINE, "Error checking if format is raster", e);
            return false;
        }
    }
}
