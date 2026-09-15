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
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.OpenAPIBuilder;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;

/** Builds the OGC API - Maps 1.0.0 OpenAPI document, pruned to the conformance classes enabled on this server. */
public class MapsAPIBuilder extends OpenAPIBuilder<WMSInfo> {

    private static final List<String> MAP_PATHS = List.of(
            "/map",
            "/map/info",
            "/collections/{collectionId}/map",
            "/collections/{collectionId}/styles/{styleId}/map",
            "/collections/{collectionId}/map/info",
            "/collections/{collectionId}/styles/{styleId}/map/info");

    private static final List<String> FILTER_PARAMETERS = List.of("filter", "filter-lang", "filter-crs");

    public MapsAPIBuilder() {
        super(MapsAPIBuilder.class, "openapi.yaml", "Maps 1.0 server", MapsService.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public OpenAPI build(WMSInfo wms) throws IOException {
        OpenAPI api = super.build(wms);
        MapsConformance maps = MapsConformance.configuration(wms);

        // the dataset map resources exist only when their conformance class is enabled
        if (!maps.datasetMap(wms)) {
            api.getPaths().remove("/map");
            api.getPaths().remove("/map/info");
        }

        // the collections parameter belongs to a class of its own, and only the dataset map takes it
        if (!maps.collectionsSelection(wms)) {
            api.getComponents().getParameters().remove("collections");
            removeParameter(api, "collections");
        }

        // GetFeatureInfo is a GeoServer extension, drop its paths when disabled
        if (!maps.featureInfo(wms)) {
            api.getPaths().remove("/collections/{collectionId}/map/info");
            api.getPaths().remove("/collections/{collectionId}/styles/{styleId}/map/info");
            api.getPaths().remove("/map/info");
        }

        // GetLegendGraphic is a GeoServer extension, drop its paths when disabled
        if (!maps.legend(wms)) {
            api.getPaths().remove("/collections/{collectionId}/legend");
            api.getPaths().remove("/collections/{collectionId}/styles/{styleId}/legend");
        }

        // the queryables resource exists only when its conformance class is enabled, and filtering with it
        if (!maps.queryablesAvailable(wms)) {
            api.getPaths().remove("/collections/{collectionId}/queryables");
        } else {
            declareGetResponseFormats(api, "/collections/{collectionId}/queryables", Queryables.class);
        }

        // filtering already accounts for the languages, none enabled means no filtering at all
        List<String> filterLanguages = APIFilterParser.enabledLanguages(wms);
        boolean filtering = maps.filtering(wms);

        // prune optional map parameters that map to disabled conformance classes
        pruneMapParameters(api, maps, wms, filtering);
        if (filtering) {
            declareFilterLanguages(api, filterLanguages);
        } else {
            FILTER_PARAMETERS.forEach(
                    name -> api.getComponents().getParameters().remove(name));
        }

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
        declareFormats(api, "/map", "a rendered map", mapFormats);
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
            declareFormats(api, "/map/info", infoDescription, infoFormats);
            declareFormats(api, "/collections/{collectionId}/map/info", infoDescription, infoFormats);
            declareFormats(api, "/collections/{collectionId}/styles/{styleId}/map/info", infoDescription, infoFormats);
            setParameterEnum(api, "f-info", infoFormats);
        }

        // valid collection identifiers: the ones the collections resource lists, streamed from the catalog
        Parameter collectionId = api.getComponents().getParameters().get("collectionId");
        Catalog catalog = wms.getGeoServer().getCatalog();
        List<String> validCollectionIds = new ArrayList<>();
        try (CloseableIterator<PublishedInfo> it =
                catalog.list(PublishedInfo.class, DatasetCollections.catalogFilter(PublishedInfo.class))) {
            while (it.hasNext()) {
                PublishedInfo published = it.next();
                if (DatasetCollections.isMappable(published)) validCollectionIds.add(published.prefixedName());
            }
        }
        collectionId.getSchema().setEnum(validCollectionIds);

        return api;
    }

    /**
     * Removes the parameters of every disabled conformance class from all the map operations, the info ones included:
     * they take the same query parameters and {@link MapsService} ignores them there too.
     */
    private void pruneMapParameters(OpenAPI api, MapsConformance maps, WMSInfo wms, boolean filtering) {
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
        if (!filtering) disabled.addAll(FILTER_PARAMETERS);
        if (disabled.isEmpty()) return;

        disabled.forEach(name -> removeParameter(api, name));
    }

    /** Removes one parameter reference from every map operation that declares it. */
    private void removeParameter(OpenAPI api, String name) {
        for (String path : MAP_PATHS) {
            PathItem item = api.getPaths().get(path);
            if (item == null) continue;
            item.getGet().getParameters().removeIf(p -> ("#/components/parameters/" + name).equals(p.get$ref()));
        }
    }

    /**
     * Declares the {@code enum} and the {@code default} of the {@code filter-lang} parameter, both required by OGC API
     * - Features - Part 3 {@code /req/filter/filter-lang-param}, from the enabled language conformance classes.
     */
    @SuppressWarnings("unchecked")
    private void declareFilterLanguages(OpenAPI api, List<String> languages) {
        Parameter filterLang = api.getComponents().getParameters().get("filter-lang");
        if (filterLang == null) return;
        Schema<String> schema = (Schema<String>) filterLang.getSchema();
        schema.setEnum(languages);
        // cql2-text is the spec default, any other one only when it is not available
        schema.setDefault(languages.contains(APIFilterParser.CQL2_TEXT) ? APIFilterParser.CQL2_TEXT : languages.get(0));
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
