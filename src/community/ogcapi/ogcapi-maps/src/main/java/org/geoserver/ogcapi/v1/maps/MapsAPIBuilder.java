/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static java.util.stream.Collectors.toCollection;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.OpenAPIBuilder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geotools.api.filter.Filter;

/** Builds the OGC API - Maps 1.0.0 OpenAPI document, pruned to the conformance classes enabled on this server. */
public class MapsAPIBuilder extends OpenAPIBuilder<WMSInfo> {

    private static final List<String> MAP_PATHS = List.of(
            "/collections/{collectionId}/map",
            "/collections/{collectionId}/styles/{styleId}/map",
            "/collections/{collectionId}/map/info",
            "/collections/{collectionId}/styles/{styleId}/map/info");

    public MapsAPIBuilder() {
        super(MapsAPIBuilder.class, "openapi.yaml", "Maps 1.0 server", MapsService.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OpenAPI build(WMSInfo wms) throws IOException {
        OpenAPI api = super.build(wms);
        MapsConformance maps = MapsConformance.configuration(wms);

        // GetFeatureInfo is a GeoServer extension, drop its paths when disabled
        if (!maps.featureInfo(wms)) {
            api.getPaths().remove("/collections/{collectionId}/map/info");
            api.getPaths().remove("/collections/{collectionId}/styles/{styleId}/map/info");
        }

        // GetLegendGraphic is a GeoServer extension, drop its paths when disabled
        if (!maps.legend(wms)) {
            api.getPaths().remove("/collections/{collectionId}/legend");
            api.getPaths().remove("/collections/{collectionId}/styles/{styleId}/legend");
        }

        // prune optional map parameters that map to disabled conformance classes
        pruneMapParameters(api, maps, wms);

        // declare the negotiated response formats
        declareGetResponseFormats(api, "/", OpenAPI.class);
        declareGetResponseFormats(api, "/conformance", ConformanceDocument.class);
        declareGetResponseFormats(api, "/collections", CollectionsDocument.class);
        declareGetResponseFormats(api, "/collections/{collectionId}", CollectionDocument.class);

        WMS wmsFacade = GeoServerExtensions.bean(WMS.class);

        // map output formats: OGC API - Maps defines png/jpeg as baseline, tiff and svg as optional classes.
        // Also consider formats disabled in the mapping service configuration.
        List<String> mapFormats = new ArrayList<>(List.of("image/png", "image/jpeg"));
        if (maps.tiff(wms)) mapFormats.add("image/tiff");
        if (maps.svg(wms)) mapFormats.add("image/svg+xml");
        Set<String> allowedMapFormats = wmsFacade.getAllowedMapFormatNames();
        mapFormats.removeIf(f -> !allowedMapFormats.contains(f));
        declareFormats(api, "/collections/{collectionId}/map", "a rendered map", mapFormats);
        declareFormats(api, "/collections/{collectionId}/styles/{styleId}/map", "a rendered map", mapFormats);
        setParameterEnum(api, "f-map", mapFormats);

        // feature info output formats come from the producible media types (no format conformance class here);
        // drop the ones that are WMS GetFeatureInfo formats disabled in the configuration
        if (maps.featureInfo(wms)) {
            List<String> infoFormats = producibleMimeTypes(FeatureInfoResponse.class);
            List<String> availableInfo = wmsFacade.getAvailableFeatureInfoFormats();
            List<String> allowedInfo = wmsFacade.getAllowedFeatureInfoFormats();
            infoFormats.removeIf(f -> availableInfo.contains(f) && !allowedInfo.contains(f));
            String infoDescription = "the feature information at the queried pixel";
            declareFormats(api, "/collections/{collectionId}/map/info", infoDescription, infoFormats);
            declareFormats(api, "/collections/{collectionId}/styles/{styleId}/map/info", infoDescription, infoFormats);
            setParameterEnum(api, "f-info", infoFormats);
        }

        // valid collection identifiers: layers and layer groups, streamed from the catalog,
        // matching the PublishedInfo listing used by the collections resource
        Parameter collectionId = api.getComponents().getParameters().get("collectionId");
        Catalog catalog = wms.getGeoServer().getCatalog();
        List<String> validCollectionIds = new ArrayList<>();
        try (CloseableIterator<PublishedInfo> it = catalog.list(PublishedInfo.class, Filter.INCLUDE)) {
            while (it.hasNext()) {
                validCollectionIds.add(it.next().prefixedName());
            }
        }
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }

    /**
     * Removes the parameters of every disabled conformance class from all the map operations, the info ones included:
     * they take the same query parameters and {@link MapsService} ignores them there too.
     */
    private void pruneMapParameters(OpenAPI api, MapsConformance maps, WMSInfo wms) {
        List<String> disabled = new ArrayList<>();
        if (!maps.spatialSubsetting(wms)) {
            disabled.addAll(List.of("bbox", "bbox-crs", "subset", "subset-crs", "center", "center-crs"));
        }
        if (!maps.scaling(wms)) disabled.add("scale-denominator");
        // width/height are provided by the scaling class and, when it is off, by the spatial subsetting class
        // (OGC API - Maps, /req/spatial-subsetting/width-height), so they are gone only when both are disabled
        if (!maps.scaling(wms) && !maps.spatialSubsetting(wms)) disabled.addAll(List.of("width", "height"));
        if (!maps.displayResolution(wms)) disabled.add("mm-per-pixel");
        if (!maps.datetime(wms)) disabled.add("datetime");
        if (!maps.crs(wms)) disabled.add("crs");
        if (!maps.background(wms)) disabled.addAll(List.of("bgcolor", "transparent", "void-color", "void-transparent"));
        if (!maps.orientation(wms)) disabled.add("orientation");
        if (disabled.isEmpty()) return;

        for (String path : MAP_PATHS) {
            PathItem item = api.getPaths().get(path);
            if (item == null) continue;
            List<Parameter> parameters = item.getGet().getParameters();
            for (String name : disabled) {
                parameters.removeIf(p -> ("#/components/parameters/" + name).equals(p.get$ref()));
            }
        }
    }

    /** Replaces the 200 response of {@code path} with an inline one advertising the given media types. */
    private void declareFormats(OpenAPI api, String path, String description, List<String> formats) {
        PathItem pi = api.getPaths().get(path);
        if (pi == null) return;
        Content content = new Content();
        for (String format : formats) {
            Schema<?> schema = isTextual(format) ? new StringSchema() : new BinarySchema();
            content.addMediaType(format, new MediaType().schema(schema));
        }
        ApiResponse ok = new ApiResponse().description(description).content(content);
        pi.getGet().getResponses().addApiResponse("200", ok);
    }

    private boolean isTextual(String mimeType) {
        return mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("yaml");
    }

    @SuppressWarnings("unchecked")
    private void setParameterEnum(OpenAPI api, String parameterName, List<String> values) {
        Parameter parameter = api.getComponents().getParameters().get(parameterName);
        if (parameter != null) parameter.getSchema().setEnum(new ArrayList<>(values));
    }

    /** The media types a response class can be encoded in, as a mutable list the caller narrows down. */
    private List<String> producibleMimeTypes(Class<?> binding) {
        return APIRequestInfo.get().getProducibleMediaTypes(binding, true).stream()
                .map(mt -> mt.toString())
                .distinct()
                .collect(toCollection(ArrayList::new));
    }
}
