/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.FreemarkerTemplateSupport;
import org.geoserver.opensearch.eo.MetadataRequest;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml3.v3_2.GML;
import org.geotools.gml3.v3_2.GMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.MediaType;

/**
 * Loads, caches and processes Freemarker templates against a stream of features. It's meant to be
 * used for a single request, as it caches the template and won't react to on disk template changes.
 */
public class TemplatesProcessor {

    private static final Logger LOGGER = Logging.getLogger(TemplatesProcessor.class);
    static final String BASE_URL_KEY = "${BASE_URL}";

    private FreemarkerTemplateSupport support;
    private Map<String, Template> templateCache = new HashMap<>();
    private boolean isHeaderWritten = false;
    private GeoServerInfo gs;
    private OSEOInfo info;

    public TemplatesProcessor(FreemarkerTemplateSupport support) {
        this.support = support;
    }

    public TemplatesProcessor(FreemarkerTemplateSupport support, GeoServerInfo gs, OSEOInfo info) {
        this.support = support;
        this.gs = gs;
        this.info = info;
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param collection The collection name used to lookup templates in the data dir
     * @param templateName The template name (with no extension)
     * @param feature The feature to be applied
     */
    public String processTemplate(String collection, String templateName, Feature feature)
            throws IOException {
        Template template = getTemplate(collection, templateName);

        StringWriter sw = new StringWriter();
        HashMap<String, Object> model = setupModel(feature);
        try {
            template.process(model, sw);
        } catch (TemplateException e) {
            throw new IOException("Error occurred processing template " + templateName, e);
        }
        return sw.toString();
    }

    public String processTemplate(SearchResults searchResults)
            throws IOException, TemplateException {
        StringWriter sw = new StringWriter();
        Template footer;
        Template header = null;
        HashMap<String, Object> model = null;
        FeatureCollection results = searchResults.getResults();

        try (FeatureIterator<Feature> featureIterator = results.features()) {
            while (featureIterator.hasNext()) {
                String templateName = "";
                String collectionName = "";
                String identifier = "";

                Feature feature = featureIterator.next();
                FeatureType schema = results.getSchema();
                final String schemaName = schema.getName().getLocalPart();

                if (JDBCOpenSearchAccess.COLLECTION.equals(schemaName)) {
                    templateName = "collection";
                } else if (JDBCOpenSearchAccess.PRODUCT.equals(schemaName)) {
                    templateName = "product";
                }

                if (templateName.equals("product")) {
                    collectionName =
                            (String)
                                    value(
                                            feature,
                                            ProductClass.GENERIC.getNamespace(),
                                            "parentIdentifier");
                    identifier =
                            (String)
                                    value(
                                            feature,
                                            ProductClass.GENERIC.getNamespace(),
                                            "identifier");
                } else if (templateName.equals("collection")) {
                    collectionName = (String) value(feature, EO_NAMESPACE, "identifier");
                    identifier = collectionName;
                }

                Template template = getTemplate(collectionName, templateName);
                Template contentStart = getTemplate("", "generic" + "-content-start");
                Template contentEnd = getTemplate(collectionName, templateName + "-content-end");

                if (!isHeaderWritten) {
                    header = getTemplate(collectionName, "generic" + "-header");
                    model = setupHeaderModel(feature, searchResults);
                    header.process(model, sw);
                    isHeaderWritten = true;
                }

                model = setupContentModel(feature, searchResults, templateName, identifier);
                contentStart.process(model, sw);
                template.process(model, sw);
                contentEnd.process(model, sw);
            }
        }

        if (header == null) {
            Template noFeatureHeader = getTemplate("", "generic" + "-header");
            model = setupHeaderModel(searchResults);
            noFeatureHeader.process(model, sw);
        }

        footer = getTemplate("", "generic" + "-footer");
        footer.process(model, sw);

        return sw.toString();
    }

    private Template getTemplate(String collection, String templateName) throws IOException {
        String key = templateName;
        if (collection != null) key = collection + "/" + templateName;
        Template t = templateCache.get(key);
        if (t == null) {
            t = support.getTemplate(collection, templateName, TemplatesProcessor.class);
            templateCache.put(key, t);
        }
        return t;
    }

    protected HashMap<String, Object> setupModel(Feature feature) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("model", feature);
        if (Dispatcher.REQUEST.get() != null) {
            final String baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            model.put("baseURL", baseURL);
            addUtilityFunctions(baseURL, model);
        }

        return model;
    }

    protected HashMap<String, Object> setupContentModel(
            Feature feature, SearchResults searchResults, String templateName, String identifier) {
        HashMap<String, Object> model = new HashMap<>();
        SearchRequest request = searchResults.getRequest();

        String identifierLink = "";
        if (templateName.equals("product")) {
            identifierLink = buildProductIdentifierLink(identifier, request);
        } else if (templateName.equals("collection")) {
            identifierLink = buildCollectionIdentifierLink(identifier, request);
        }

        model.put("id", identifierLink.replaceAll("&", "&amp;"));
        model.put("title", identifier);
        putDatesToContentModel(feature, model);

        Geometry footprint = (Geometry) value(feature, "footprint");
        if (footprint != null) {
            // geometry is already in lat/lon order here
            Envelope envelope = footprint.getEnvelopeInternal();
            model.put("georssGeom", encodeGmlRssGeometry(footprint));
            model.put(
                    "georssBox",
                    envelope.getMinX()
                            + " "
                            + envelope.getMinY()
                            + " "
                            + envelope.getMaxX()
                            + " "
                            + envelope.getMaxY());
        }

        if (templateName.equals("product")) {
            fillProductContentModel(feature, identifier, model, request, identifierLink);
        } else if (templateName.equals("collection")) {
            String metadataLink =
                    buildMetadataLink(null, identifier, MetadataRequest.ISO_METADATA, request);
            String osddLink = buildOsddLink(identifier, request);

            model.put(
                    "identifierLink",
                    encodeLink("self", identifierLink, AtomSearchResponse.MIME, "self"));
            model.put(
                    "metadataLink",
                    encodeLink(
                            "alternate",
                            metadataLink,
                            MetadataRequest.ISO_METADATA,
                            "ISO metadata"));
            model.put(
                    "osddLink",
                    encodeLink(
                            "search",
                            osddLink,
                            DescriptionResponse.OS_DESCRIPTION_MIME,
                            "Collection OSDD"));
            encodeOgcLinksFromFeature(feature, request, model);
        }

        model.put("model", feature);
        if (Dispatcher.REQUEST.get() != null) {
            final String baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            model.put("baseURL", baseURL);
            addUtilityFunctions(baseURL, model);
        }

        return model;
    }

    private void fillProductContentModel(
            Feature feature,
            String identifier,
            HashMap<String, Object> model,
            SearchRequest request,
            String identifierLink) {
        String metadataLink =
                buildMetadataLink(
                        request.getParentIdentifier(),
                        identifier,
                        MetadataRequest.OM_METADATA,
                        request);

        model.put(
                "identifierLink",
                encodeLink("self", identifierLink, AtomSearchResponse.MIME, "self"));
        model.put(
                "metadataLink",
                encodeLink("alternate", metadataLink, MetadataRequest.OM_METADATA, "OM metadata"));

        String quicklookLink = buildQuicklookLink(identifier, request);

        // and a quicklook as a link and as media
        if (quicklookLink != null) {
            model.put(
                    "quicklookLink", encodeLink("icon", quicklookLink, "image/jpeg", "Quicklook"));
            model.put("mediaContent", encodeMedia("image", "image/jpeg", quicklookLink));
        }

        encodeOgcLinksFromFeature(feature, request, model);
        encodeDownloadLink(feature, request, model);
    }

    private void putDatesToContentModel(Feature feature, HashMap<String, Object> model) {
        Date start = (Date) value(feature, "timeStart");
        Date end = (Date) value(feature, "timeEnd");
        if (start != null || end != null) {
            Date updated = end == null ? start : end;
            String formattedUpdated = DateTimeFormatter.ISO_INSTANT.format(updated.toInstant());
            model.put("updated", formattedUpdated);

            // dc:date, can be a range
            String spec;
            if (start != null && end != null && start.equals(end)) {
                spec = DateTimeFormatter.ISO_INSTANT.format(start.toInstant());
            } else {
                spec = start != null ? DateTimeFormatter.ISO_INSTANT.format(start.toInstant()) : "";
                spec += "/";
                spec += end != null ? DateTimeFormatter.ISO_INSTANT.format(end.toInstant()) : "";
            }
            model.put("dcDate", spec);
        }
    }

    protected HashMap<String, Object> setupHeaderModel(
            Feature feature, SearchResults searchResults) {
        HashMap<String, Object> model = new HashMap<>();
        putHeaderContentToModel(searchResults, model);

        model.put("model", feature);
        if (Dispatcher.REQUEST.get() != null) {
            final String baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            model.put("baseURL", baseURL);
            addUtilityFunctions(baseURL, model);
        }

        return model;
    }

    protected HashMap<String, Object> setupHeaderModel(SearchResults searchResults) {
        HashMap<String, Object> model = new HashMap<>();
        putHeaderContentToModel(searchResults, model);

        if (Dispatcher.REQUEST.get() != null) {
            final String baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            model.put("baseURL", baseURL);
            addUtilityFunctions(baseURL, model);
        }

        return model;
    }

    private void putHeaderContentToModel(
            SearchResults searchResults, HashMap<String, Object> model) {
        Integer startIndex = getQueryStartIndex(searchResults) + 1;
        model.put("startIndex", startIndex);
        model.put("itemsPerPage", "" + searchResults.getRequest().getQuery().getMaxFeatures());
        model.put("Query", getQueryAttributes(searchResults.getRequest()));
        String organization = gs.getSettings().getContact().getContactOrganization();
        if (organization != null) {
            model.put("organization", organization);
        }
        String title = info.getTitle();
        if (title != null) {
            model.put("title", title);
        }
        String updated = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        model.put("updated", updated);
        model.put("totalResults", searchResults.getTotalResults());
        buildPaginationLinks(searchResults, model);
        buildSearchLink(searchResults.getRequest(), model);
    }

    /**
     * Adds the <code>oseoLink</code> and <code>oseoLink</code> functions to the model, for usage in
     * the template, and the bbox extraction ones.
     */
    protected void addUtilityFunctions(String baseURL, Map<String, Object> model) {
        model.put(
                "oseoLink",
                (TemplateMethodModelEx)
                        arguments -> {
                            Map<String, String> kvp = new LinkedHashMap<>();
                            if (arguments.size() > 1 && (arguments.size() % 2) != 1) {
                                throw new IllegalArgumentException(
                                        "Expected a path argument, followed by an optional list of keys and values. Found a key that is not matched to a value: "
                                                + arguments);
                            }
                            int i = 1;
                            while (i < arguments.size()) {
                                kvp.put(toString(arguments.get(i++)), toString(arguments.get(i++)));
                            }
                            return ResponseUtils.buildURL(
                                    baseURL,
                                    ResponseUtils.appendPath("oseo", toString(arguments.get(0))),
                                    kvp,
                                    URLMangler.URLType.SERVICE);
                        });
        model.put(
                "resourceLink",
                (TemplateMethodModelEx)
                        arguments ->
                                ResponseUtils.buildURL(
                                        baseURL,
                                        toString(arguments.get(0)),
                                        null,
                                        URLMangler.URLType.RESOURCE));
        // bbox extraction functions
        model.put(
                "minx",
                (TemplateMethodModelEx)
                        arguments -> {
                            Geometry g = toGeometry(arguments.get(0));
                            return g.getEnvelopeInternal().getMinX();
                        });
        model.put(
                "miny",
                (TemplateMethodModelEx)
                        arguments -> {
                            Geometry g = toGeometry(arguments.get(0));
                            return g.getEnvelopeInternal().getMinY();
                        });
        model.put(
                "maxx",
                (TemplateMethodModelEx)
                        arguments -> {
                            Geometry g = toGeometry(arguments.get(0));
                            return g.getEnvelopeInternal().getMaxX();
                        });
        model.put(
                "maxy",
                (TemplateMethodModelEx)
                        arguments -> {
                            Geometry g = toGeometry(arguments.get(0));
                            return g.getEnvelopeInternal().getMaxY();
                        });
        model.put(
                "gml",
                (TemplateMethodModelEx)
                        arguments -> {
                            try {
                                Geometry g = toGeometry(arguments.get(0));
                                return encodeToGML(g);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to encode geometry", e);
                            }
                        });
        model.put("loadJSON", parseJSON());
    }

    private TemplateMethodModelEx parseJSON() {
        return arguments -> loadJSON(arguments.get(0).toString());
    }

    private String loadJSON(String filePath) {
        try {
            GeoServerDataDirectory geoServerDataDirectory =
                    GeoServerExtensions.bean(GeoServerDataDirectory.class);

            File file = geoServerDataDirectory.findFile(filePath);
            if (file == null) {
                LOGGER.warning("File is outside of data directory");
                throw new RuntimeException(
                        "File " + filePath + " is outside of the data directory");
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(file).toString();
        } catch (Exception e) {
            LOGGER.warning("Failed to parse JSON file " + e.getLocalizedMessage());
        }
        LOGGER.warning("Failed to create a JSON object");
        return "Failed to create a JSON object";
    }

    private String encodeToGML(Geometry g) throws IOException {
        Encoder encoder = new Encoder(new GMLConfiguration());
        encoder.setOmitXMLDeclaration(true);
        encoder.setIndenting(true);
        String gml = encoder.encodeAsString(g, GML.Polygon);

        // has extra prefix declarations that we don't want, sanitize
        gml = gml.replace("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"", "");
        gml = gml.replace("xmlns:gml=\"http://www.opengis.net/gml/3.2\"", "");

        return gml;
    }

    private String toString(Object argument) throws TemplateModelException {
        if (argument instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) argument).getAsString();
        }
        // in case it's an attribute, unwrap the raw value and convert
        if (argument instanceof SimpleMapModel) {
            argument = ((SimpleMapModel) argument).get("rawValue");
        }
        return Converters.convert(argument, String.class);
    }

    private Geometry toGeometry(Object argument) throws TemplateModelException {
        // in case it's an attribute, unwrap the raw value and convert
        if (argument instanceof SimpleMapModel) {
            argument = ((SimpleMapModel) argument).get("rawValue");
        } else if (argument instanceof BeanModel) {
            argument = ((BeanModel) argument).getWrappedObject();
        }
        return Converters.convert(argument, Geometry.class);
    }

    private Object value(Feature feature, String attribute) {
        String prefix = feature.getType().getName().getNamespaceURI();
        return value(feature, prefix, attribute);
    }

    private Object value(Feature feature, String prefix, String attribute) {
        Property property;
        if (prefix != null) {
            property = feature.getProperty(new NameImpl(prefix, attribute));
        } else {
            property = feature.getProperty(attribute);
        }
        if (property == null) {
            return null;
        } else {
            Object value = property.getValue();
            if (value instanceof Geometry) {
                // cheap reprojection support since there is no reprojecting collection
                // wrapper for complex features
                CoordinateReferenceSystem nativeCRS =
                        ((GeometryDescriptor) property.getDescriptor())
                                .getCoordinateReferenceSystem();
                if (nativeCRS != null
                        && !CRS.equalsIgnoreMetadata(nativeCRS, OpenSearchParameters.OUTPUT_CRS)) {
                    Geometry g = (Geometry) value;
                    try {
                        return JTS.transform(
                                g,
                                CRS.findMathTransform(nativeCRS, OpenSearchParameters.OUTPUT_CRS));
                    } catch (MismatchedDimensionException
                            | TransformException
                            | FactoryException e) {
                        throw new OWS20Exception(
                                "Failed to reproject geometry to EPSG:4326 lat/lon", e);
                    }
                }
            }
            return value;
        }
    }

    private int getQueryStartIndex(SearchResults results) {
        Integer startIndex = results.getRequest().getQuery().getStartIndex();
        if (startIndex == null) {
            startIndex = 0;
        }
        return startIndex;
    }

    public String getQueryAttributes(SearchRequest request) {
        // turn each request parameter into an attribute for os:Query
        Map<String, String> parameters = new LinkedHashMap<>();
        for (Map.Entry<Parameter, String> entry : request.getSearchParameters().entrySet()) {
            Parameter parameter = entry.getKey();
            String value = entry.getValue();
            String key = OpenSearchParameters.getQualifiedParamName(info, parameter, false);
            parameters.put(key, value);
        }
        // fill in defaults
        final Query query = request.getQuery();
        if (parameters.get("count") == null) {
            parameters.put("count", "" + query.getMaxFeatures());
        }
        if (parameters.get("startIndex") == null) {
            Integer startIndex = query.getStartIndex();
            if (startIndex == null) {
                startIndex = 1;
            }
            parameters.put("startIndex", "" + startIndex);
        }
        parameters.put("role", "request");

        String result = "";
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result += key + "=\"" + value + "\" ";
        }
        return result;
    }

    private void buildPaginationLinks(SearchResults results, HashMap<String, Object> model) {
        PaginationLinkBuilder builder =
                new PaginationLinkBuilder(results, info, AtomSearchResponse.MIME);

        model.put("self", encodeLink("self", builder.getSelf(), AtomSearchResponse.MIME));
        model.put("first", encodeLink("first", builder.getFirst(), AtomSearchResponse.MIME));
        if (builder.getPrevious() != null)
            model.put(
                    "previous",
                    encodeLink("previous", builder.getPrevious(), AtomSearchResponse.MIME));
        if (builder.getNext() != null)
            model.put("next", encodeLink("next", builder.getNext(), AtomSearchResponse.MIME));
        model.put("last", encodeLink("last", builder.getLast(), AtomSearchResponse.MIME));
    }

    private void buildSearchLink(SearchRequest request, HashMap<String, Object> model) {
        Map<String, String> kvp = null;
        if (request.getParentIdentifier() != null) {
            kvp = Collections.singletonMap("parentId", request.getParentIdentifier());
        }
        String href =
                ResponseUtils.buildURL(
                        request.getBaseUrl(),
                        "oseo/search/description",
                        kvp,
                        URLMangler.URLType.SERVICE);
        model.put("search", encodeLink("search", href, DescriptionResponse.OS_DESCRIPTION_MIME));
    }

    private String buildCollectionIdentifierLink(Object identifier, SearchRequest request) {
        String baseURL = request.getBaseUrl();
        Map<String, String> kvp = new LinkedHashMap<>();
        kvp.put("uid", String.valueOf(identifier));
        kvp.put("httpAccept", AtomSearchResponse.MIME);
        return ResponseUtils.buildURL(baseURL, "oseo/search", kvp, URLMangler.URLType.SERVICE);
    }

    private String buildProductIdentifierLink(Object identifier, SearchRequest request) {
        String baseURL = request.getBaseUrl();
        Map<String, String> kvp = new LinkedHashMap<>();
        kvp.put("parentId", request.getParentIdentifier());
        kvp.put("uid", String.valueOf(identifier));
        kvp.put("httpAccept", AtomSearchResponse.MIME);
        return ResponseUtils.buildURL(baseURL, "oseo/search", kvp, URLMangler.URLType.SERVICE);
    }

    private String buildQuicklookLink(String identifier, SearchRequest request) {
        String baseURL = request.getBaseUrl();
        Map<String, String> kvp = new LinkedHashMap<>();
        kvp.put("parentId", request.getParentIdentifier());
        kvp.put("uid", String.valueOf(identifier));
        return ResponseUtils.buildURL(baseURL, "oseo/quicklook", kvp, URLMangler.URLType.SERVICE);
    }

    private String buildMetadataLink(
            String parentIdentifier, Object identifier, String mimeType, SearchRequest request) {
        String baseURL = request.getBaseUrl();
        Map<String, String> kvp = new LinkedHashMap<>();
        if (parentIdentifier != null) {
            kvp.put("parentId", parentIdentifier);
        }
        kvp.put("uid", String.valueOf(identifier));
        if (mimeType != null) {
            kvp.put("httpAccept", mimeType);
        }
        return ResponseUtils.buildURL(baseURL, "oseo/metadata", kvp, URLMangler.URLType.SERVICE);
    }

    private String buildOsddLink(String parentIdentifier, SearchRequest request) {
        String baseURL = request.getBaseUrl();
        Map<String, String> kvp = new LinkedHashMap<>();
        if (parentIdentifier != null) {
            kvp.put("parentId", parentIdentifier);
        }
        return ResponseUtils.buildURL(baseURL, "oseo/description", kvp, URLMangler.URLType.SERVICE);
    }

    private String encodeLink(String rel, String builder, String type) {
        return "href=\""
                + builder.replaceAll("&", "&amp;")
                + "\" rel=\""
                + rel
                + "\" type=\""
                + type
                + "\"";
    }

    private String encodeLink(String rel, String builder, String type, String title) {
        return "href=\""
                + builder.replaceAll("&", "&amp;")
                + "\" rel=\""
                + rel
                + "\" title=\""
                + title.replaceAll("&", "&amp;")
                + "\" type=\""
                + type
                + "\"";
    }

    private String encodeMedia(String medium, String type, String url) {
        return "medium=\""
                + medium
                + "\" type=\""
                + type
                + "\" url=\""
                + url.replaceAll("&", "&amp;")
                + "\"";
    }

    private String encodeOffering(String code, String href, String method, String type) {
        return "code=\""
                + code
                + "\" href=\""
                + href.replaceAll("&", "&amp;")
                + "\" method=\""
                + method
                + "\" type=\""
                + type
                + "\"";
    }

    private void encodeDownloadLink(
            Feature feature, SearchRequest request, HashMap<String, Object> model) {
        String location = (String) value(feature, null, OpenSearchAccess.ORIGINAL_PACKAGE_LOCATION);
        if (location != null) {
            String type = (String) value(feature, null, OpenSearchAccess.ORIGINAL_PACKAGE_TYPE);
            if (type == null) {
                type = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String hrefBase = getHRefBase(request);
            String locationExpanded =
                    QuickTemplate.replaceVariables(
                            location, Collections.singletonMap(BASE_URL_KEY, hrefBase));

            model.put(
                    "downloadLink",
                    encodeLink("enclosure", locationExpanded, type, "Source package download"));
        }
    }

    private void encodeOgcLinksFromFeature(
            Feature feature, SearchRequest request, HashMap<String, Object> model) {
        // build ogc links if available
        Collection<Property> linkProperties =
                feature.getProperties(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME);
        if (linkProperties != null) {
            Map<String, List<SimpleFeature>> linksByOffering =
                    linkProperties
                            .stream()
                            .map(p -> (SimpleFeature) p)
                            .sorted(LinkFeatureComparator.INSTANCE)
                            .collect(
                                    Collectors.groupingBy(
                                            f -> (String) f.getAttribute("offering")));
            String hrefBase = getHRefBase(request);
            encodeOgcLinks(linksByOffering, hrefBase, model);
        }
    }

    private String getHRefBase(SearchRequest request) {
        String baseURL = request.getBaseUrl();
        String hrefBase = ResponseUtils.buildURL(baseURL, null, null, URLMangler.URLType.SERVICE);
        if (hrefBase.endsWith("/")) {
            hrefBase = hrefBase.substring(0, hrefBase.length() - 1);
        }
        return hrefBase;
    }

    private void encodeOgcLinks(
            Map<String, List<SimpleFeature>> linksByOffering,
            String hrefBase,
            HashMap<String, Object> model) {
        ArrayList<Offering> offerings = new ArrayList<>();

        linksByOffering.forEach(
                (offering, links) -> {
                    ArrayList<String> offeringDetailList = new ArrayList<>();

                    for (SimpleFeature link : links) {
                        offeringDetailList.add(encodeOgcLink(link, hrefBase));
                    }
                    offerings.add(new Offering("code=\"" + offering + "\"", offeringDetailList));
                });
        model.put("offerings", offerings);
    }

    private String encodeOgcLink(SimpleFeature link, String hrefBase) {
        String method = (String) link.getAttribute("method");
        String code = (String) link.getAttribute("code");
        String type = (String) link.getAttribute("type");
        String href = (String) link.getAttribute("href");
        String hrefExpanded =
                QuickTemplate.replaceVariables(
                        href, Collections.singletonMap(BASE_URL_KEY, hrefBase));
        return encodeOffering(code, hrefExpanded, method, type);
    }

    private String encodeGmlRssGeometry(Geometry g) {
        try {
            Encoder encoder = new Encoder(new GMLConfiguration());
            encoder.setOmitXMLDeclaration(true);
            encoder.setIndenting(true);
            String gml = encoder.encodeAsString(g, GML.Polygon);

            // has extra prefix declarations that we don't want, sanitize
            gml = gml.replace("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"", "");
            gml = gml.replace("xmlns:gml=\"http://www.opengis.net/gml/3.2\"", "");

            return gml;
        } catch (Exception e) {
            throw new RuntimeException("Cannot transform the specified geometry in GML", e);
        }
    }
}
