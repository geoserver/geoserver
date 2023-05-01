/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import static org.geoserver.ows.URLMangler.URLType.SERVICE;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.opengis.wps10.ExecuteType;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgMetadata;
import org.geotools.geopkg.GeoPkgMetadata.Scope;
import org.geotools.geopkg.GeoPkgMetadataExtension;
import org.geotools.geopkg.GeoPkgMetadataReference;
import org.geotools.geopkg.wps.GeoPackageProcessRequest;
import org.geotools.util.logging.Logging;
import org.geotools.wps.WPS;
import org.geotools.xsd.Encoder;

/** Adds the OWS Context elements into the GeoPackage using freemarker templates */
public class OWSContextWriter {

    public static final String PROVENANCE_SA_URI =
            "https://gitlab.com/imagemattersllc/ogc-vtp2/-/blob/master/extensions/22"
                    + "-metadata-dataset-provenance.adoc/example.geojson";
    public static final String PROVENANCE_SA_TYPE = "im_metadata_dp_owc_geojson";
    public static final String STYLESHEET_SA_URI =
            "https://gitlab.ogc.org/ogc/t16-d010-geopackage-er/-/blob/master/ER/annex-samples.adoc";
    public static final String STYLESHEET_SA_TYPE = "im_metadata_cop_owc_geojson";

    private static final Logger LOGGER = Logging.getLogger(OWSContextWriter.class);

    private static final String OWS_CONTEXT_JSON_URI =
            "https://portal.opengeospatial.org/files/?artifact_id=68826";

    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final String GEO_JSON_MIME = "application/geo+json";

    private static Configuration templateConfig = TemplateUtils.getSafeConfiguration();
    private static Template REQUEST_TEMPLATE;
    private static Template FEATURE_WFS_TEMPLATE;
    private static Template STYLESHEETS_TEMPLATE;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        try {
            ClassTemplateLoader loader = new ClassTemplateLoader(GeoPackageProcess.class, "");
            templateConfig.setTemplateLoader(loader);
            REQUEST_TEMPLATE = templateConfig.getTemplate("owsContextRequest.ftl");
            FEATURE_WFS_TEMPLATE = templateConfig.getTemplate("owsContextFeature.ftl");
            STYLESHEETS_TEMPLATE = templateConfig.getTemplate("owsContextStylesheet.ftl");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load freemarker templates for OWS contexts", e);
        }
    }

    private final GeoPackage gpkg;
    private final GeoPkgMetadataExtension metadatas;
    private final SemanticAnnotationsExtension annotations;
    private final GeoServer gs;
    private final StyleWorker styleWorker;
    private Map<String, LayerInfo> layerStyles = new HashMap<>();
    private GeoPkgMetadata requestMetadata;
    private GeoPkgSemanticAnnotation provenanceAnnotation;

    public OWSContextWriter(GeoServer gs, GeoPackage gpkg, StyleWorker styleWorker) {
        this.gs = gs;
        this.gpkg = gpkg;
        this.metadatas = gpkg.getExtension(GeoPkgMetadataExtension.class);
        this.annotations = gpkg.getExtension(SemanticAnnotationsExtension.class);
        this.styleWorker = styleWorker;
    }

    /**
     * Adds the WPS request context. Should be called first, as the per layer metadata references
     * depend on it.
     *
     * @throws IOException
     */
    public void addRequestContext() throws IOException {
        // setup the template model
        HashMap<String, Object> model = new HashMap<>();
        model.put("geometry", "{}");
        model.put("contact", gs.getGlobal().getSettings().getContact());
        model.put("now", getCurrentISOTimestamp());
        model.put("uuid", UUID.randomUUID().toString());
        Request request = Dispatcher.REQUEST.get();
        String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        String capsURL =
                buildURL(
                        baseURL,
                        "wps",
                        ImmutableMap.of(
                                "service", "WPS", "version", "1.0", "request", "GetCapabilities"),
                        SERVICE);
        model.put("getCapabilitiesURL", capsURL);
        String describeURL =
                buildURL(
                        baseURL,
                        "wps",
                        ImmutableMap.of(
                                "service",
                                "WPS",
                                "version",
                                "1.0",
                                "request",
                                "DescribeProcess",
                                "identifier",
                                "gs:GeoPackageProcess"),
                        SERVICE);
        model.put("describeProcessURL", describeURL);
        String executeURL = buildURL(baseURL, "wps", Collections.emptyMap(), SERVICE);
        model.put("executeURL", executeURL);
        String requestXML = getRequestXML(request);
        String escapedBody = MAPPER.writer().writeValueAsString(requestXML);
        model.put("executeBody", escapedBody);
        String requestContext = process(REQUEST_TEMPLATE, model);

        try {
            GeoPkgMetadata metadata =
                    new GeoPkgMetadata(
                            Scope.Undefined, OWS_CONTEXT_JSON_URI, GEO_JSON_MIME, requestContext);
            metadatas.addMetadata(metadata);

            GeoPkgMetadataReference reference =
                    new GeoPkgMetadataReference(
                            GeoPkgMetadataReference.Scope.GeoPackage,
                            null,
                            null,
                            null,
                            new Date(),
                            metadata,
                            null);
            metadatas.addReference(reference);
            this.requestMetadata = metadata;

            GeoPkgSemanticAnnotation annotation =
                    new GeoPkgSemanticAnnotation(
                            PROVENANCE_SA_TYPE, "Dataset provenance", PROVENANCE_SA_URI);
            annotations.addAnnotation(annotation);
            this.provenanceAnnotation = annotation;

            GeoPkgAnnotationReference ar =
                    new GeoPkgAnnotationReference(
                            "gpkg_metadata", "id", metadata.getId(), annotation);
            annotations.addReference(ar);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    /**
     * Adds the OWS context for a given feature type, assuming WFS is enabled. Should be called
     * after {@link #addRequestContext()}
     *
     * @throws IOException
     */
    public void addFeatureTypeContext(FeatureTypeInfo ft, String layerName) throws IOException {
        WFSInfo service = gs.getService(WFSInfo.class);
        if (service != null && !service.isEnabled()) {
            return;
        }

        // setup the template model
        HashMap<String, Object> model = new HashMap<>();
        model.put("featureType", ft.prefixedName());
        model.put("title", Optional.ofNullable(ft.getTitle()).orElse(ft.prefixedName()));
        model.put("abstract", ft.getAbstract());
        model.put("now", getCurrentISOTimestamp());
        Request request = Dispatcher.REQUEST.get();
        String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        String capsURL =
                buildURL(
                        baseURL,
                        "wfs",
                        ImmutableMap.of(
                                "service", "WFS", "version", "2.0", "request", "GetCapabilities"),
                        SERVICE);
        model.put("getCapabilitiesURL", capsURL);
        String getFeatureURL =
                buildURL(
                        baseURL,
                        "wfs",
                        ImmutableMap.of(
                                "service",
                                "WFS",
                                "version",
                                "2.0",
                                "request",
                                "GetFeature",
                                "typeNames",
                                ft.prefixedName()),
                        SERVICE);
        model.put("getFeatureURL", getFeatureURL);
        String requestContext = process(FEATURE_WFS_TEMPLATE, model);

        try {
            GeoPkgMetadata metadata =
                    new GeoPkgMetadata(
                            Scope.Dataset, OWS_CONTEXT_JSON_URI, GEO_JSON_MIME, requestContext);
            metadatas.addMetadata(metadata);

            GeoPkgMetadataReference reference =
                    new GeoPkgMetadataReference(
                            GeoPkgMetadataReference.Scope.Table,
                            layerName,
                            null,
                            null,
                            new Date(),
                            metadata,
                            requestMetadata);
            metadatas.addReference(reference);

            GeoPkgAnnotationReference ar =
                    new GeoPkgAnnotationReference(
                            "gpkg_metadata", "id", metadata.getId(), provenanceAnnotation);
            annotations.addReference(ar);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private String pretty(String json) {
        try {
            Object parsedJson = MAPPER.readValue(json, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING, "Could not pretty print JSON, returning as is", e);
            return json;
        }
    }

    /** Encodes the request back to XML */
    private String getRequestXML(Request request) throws IOException {
        ExecuteType execute = (ExecuteType) request.getOperation().getParameters()[0];
        WPSConfiguration config = new WPSConfiguration();
        Encoder encoder = new Encoder(config);
        encoder.setIndenting(true);
        encoder.setIndentSize(4);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encoder.encode(execute, WPS.Execute, bos);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    public String getCurrentISOTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_STRING);
        format.setTimeZone(UTC);
        return format.format(new Date());
    }

    public String process(Template template, Object model) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    public void addStyleGroupInformations(GeoPackageProcessRequest contents) throws IOException {
        // if we don't have at least two layers, there is nothing interesting to add
        if (layerStyles.size() < 2) {
            return;
        }

        // get all the layer groups, find the ones containing the layers and styles
        // that we have dumped
        List<LayerGroupInfo> layerGroups = getGroupsByLayerStyles();
        for (LayerGroupInfo layerGroup : layerGroups) {
            writeStyleContext(layerGroup, layerStyles, contents.getName());
        }
    }

    private void writeStyleContext(
            LayerGroupInfo layerGroup, Map<String, LayerInfo> layerStyles, String packageName)
            throws IOException {
        // setup the template model
        HashMap<String, Object> model = new HashMap<>();
        model.put("groupId", layerGroup.prefixedName());
        String groupTitle =
                Optional.ofNullable(layerGroup.getTitle()).orElse(layerGroup.prefixedName());
        model.put("groupTitle", groupTitle);
        model.put("now", getCurrentISOTimestamp());
        model.put("contact", gs.getGlobal().getSettings().getContact());
        model.put("layers", getLayersModel(layerGroup, layerStyles));
        model.put("packageName", packageName);

        String stylesheetContext = process(STYLESHEETS_TEMPLATE, model);

        try {
            GeoPkgMetadata metadata =
                    new GeoPkgMetadata(
                            Scope.Undefined,
                            OWS_CONTEXT_JSON_URI,
                            GEO_JSON_MIME,
                            stylesheetContext);
            metadatas.addMetadata(metadata);

            GeoPkgMetadataReference reference =
                    new GeoPkgMetadataReference(
                            GeoPkgMetadataReference.Scope.GeoPackage,
                            null,
                            null,
                            null,
                            new Date(),
                            metadata,
                            null);
            metadatas.addReference(reference);

            GeoPkgSemanticAnnotation annotation =
                    new GeoPkgSemanticAnnotation(
                            STYLESHEET_SA_TYPE,
                            "OGC OWS Context GeoJSON for " + groupTitle,
                            STYLESHEET_SA_URI);
            annotations.addAnnotation(annotation);

            GeoPkgAnnotationReference ar =
                    new GeoPkgAnnotationReference(
                            "gpkg_metadata", "id", metadata.getId(), annotation);
            annotations.addReference(ar);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private List<Map<String, Object>> getLayersModel(
            LayerGroupInfo layerGroup, Map<String, LayerInfo> layerStyles) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        List<PublishedInfo> layers = layerGroup.getLayers();
        Map<LayerInfo, String> targetLayers =
                layerStyles.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey()));
        int size = layers.size();
        // OWS context lists the layers top to bottom, the opposite of painter's order
        // used in the layer group definition, and WMS. So, iterate backwards.
        for (int i = size - 1; i >= 0; i--) {
            PublishedInfo publishedInfo = layers.get(i);
            String tableName = targetLayers.get(publishedInfo);
            if (tableName != null) {
                LayerInfo layer = (LayerInfo) publishedInfo;
                StyleInfo style = layerGroup.getStyles().get(i);
                if (style == null) {
                    style = layer.getDefaultStyle();
                }

                Map<String, Object> model = new HashMap<>();
                model.put("type", "Feature");
                model.put("id", layer.prefixedName());
                model.put("title", layer.getTitle());
                model.put("tableName", tableName);
                model.put("styleId", style.prefixedName());
                model.put("styleTitle", styleWorker.getTitle(style));
                model.put("styleAbstract", styleWorker.getAbstract(style));
                result.add(model);
            }
        }
        return result;
    }

    private List<LayerGroupInfo> getGroupsByLayerStyles() {

        Collection<LayerInfo> layers = layerStyles.values();
        return gs.getCatalog().getLayerGroups().stream()
                .filter(lg -> stylesMatch(lg, layers))
                .collect(Collectors.toList());
    }

    private boolean stylesMatch(LayerGroupInfo lg, Collection<LayerInfo> layers) {
        for (LayerInfo layer : layers) {
            // is the layer there?
            int idx = lg.getLayers().indexOf(layer);
            if (idx < 0) return false;

            StyleInfo styleInfo = lg.getStyles().get(idx);
            if (styleInfo != null
                    && !(styleInfo.equals(layer.getDefaultStyle())
                            || (layer.getStyles() != null
                                    && layer.getStyles().contains(styleInfo)))) return false;
        }

        return true;
    }

    public void trackLayerStyles(String name, LayerInfo layerInfo) {
        this.layerStyles.put(name, layerInfo);
    }
}
