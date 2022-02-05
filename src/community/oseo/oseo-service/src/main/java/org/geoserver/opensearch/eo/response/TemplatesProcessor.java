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
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Loads, caches and processes Freemarker templates against a stream of features. It's meant to be
 * used for a single request, as it caches the template and won't react to on disk template changes.
 */
public class TemplatesProcessor {

    private static final Logger LOGGER = Logging.getLogger(TemplatesProcessor.class);
    private static final String BASE_URL_KEY = "${BASE_URL}";

    private FreemarkerTemplateSupport support;
    private Map<String, Template> templateCache = new HashMap<>();
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
        HashMap<String, Object> model = setupGenericHeaderModel(searchResults);
        StringWriter sw = new StringWriter();
        Template header = getTemplate("", "generic" + "-header");
        header.process(model, sw);
        FeatureCollection results = searchResults.getResults();

        try (FeatureIterator<Feature> featureIterator = results.features()) {
            while (featureIterator.hasNext()) {
                String templateName = "";
                String collectionName = "";

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
                } else if (templateName.equals("collection")) {
                    collectionName = (String) value(feature, EO_NAMESPACE, "identifier");
                }

                Template template = getTemplate(collectionName, templateName);
                model = setupContentModel(feature, searchResults, templateName);
                template.process(model, sw);
            }
        }

        Template footer = getTemplate("", "generic" + "-footer");
        footer.process(model, sw);

        return sw.toString();
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

    protected HashMap<String, Object> setupGenericHeaderModel(SearchResults searchResults) {
        HashMap<String, Object> model = new HashMap<>();
        prepareGenericHeaderModel(searchResults, model);
        model.put("searchResults", searchResults);

        if (Dispatcher.REQUEST.get() != null) {
            final String baseURL = ResponseUtils.baseURL(Dispatcher.REQUEST.get().getHttpRequest());
            model.put("baseURL", baseURL);
            addUtilityFunctions(baseURL, model);
        }

        return model;
    }

    protected HashMap<String, Object> setupContentModel(
            Feature feature, SearchResults searchResults, String templateName) {
        HashMap<String, Object> model = new HashMap<>();
        SearchRequest request = searchResults.getRequest();

        putDatesToContentModel(feature, model);

        Geometry footprint = (Geometry) value(feature, "footprint");
        if (footprint != null) {
            // geometry is already in lat/lon order here
            GeometryAttribute defaultGeometryProperty = feature.getDefaultGeometryProperty();
            // reprojected the coordinates of the geometry
            defaultGeometryProperty.setValue(footprint);
            feature.setDefaultGeometryProperty(defaultGeometryProperty);
        }

        if (templateName.equals("product")) {
            encodeOgcLinksFromFeature(feature, request, model);
        } else if (templateName.equals("collection")) {
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

    private void prepareGenericHeaderModel(
            SearchResults searchResults, HashMap<String, Object> model) {
        model.put("Query", getQueryAttributes(searchResults.getRequest()));
        String organization = gs.getSettings().getContact().getContactOrganization();
        if (organization != null) {
            model.put("organization", organization);
        }
        String title = info.getTitle();
        if (title != null) {
            model.put("title", title);
        }
        model.put("updated", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        model.put(
                "builder", new PaginationLinkBuilder(searchResults, info, AtomSearchResponse.MIME));
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
                // cheap re-projection support since there is no re-projecting collection
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

    public Map<String, String> getQueryAttributes(SearchRequest request) {
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

        return parameters;
    }

    private void encodeOgcLinksFromFeature(
            Feature feature, SearchRequest request, HashMap<String, Object> model) {
        // build ogc links if available
        Collection<Property> linkProperties =
                feature.getProperties(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME);
        if (linkProperties != null) {
            Map<String, List<SimpleFeature>> linksByOffering =
                    linkProperties.stream()
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
                    ArrayList<OfferingDetail> offeringDetailList = new ArrayList<>();

                    for (SimpleFeature link : links) {
                        offeringDetailList.add(encodeOgcLink(link, hrefBase));
                    }
                    offerings.add(new Offering(offering, offeringDetailList));
                });
        model.put("offerings", offerings);
    }

    private OfferingDetail encodeOgcLink(SimpleFeature link, String hrefBase) {
        String method = (String) link.getAttribute("method");
        String code = (String) link.getAttribute("code");
        String type = (String) link.getAttribute("type");
        String href = (String) link.getAttribute("href");
        String hrefExpanded =
                QuickTemplate.replaceVariables(
                        href, Collections.singletonMap(BASE_URL_KEY, hrefBase));
        return new OfferingDetail(method, code, type, hrefExpanded);
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
}
