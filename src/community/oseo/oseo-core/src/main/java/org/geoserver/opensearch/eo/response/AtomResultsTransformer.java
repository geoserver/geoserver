/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.MetadataRequest;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.OWS20Exception;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.Translator;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Polygon;
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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Transforms results into ATOM documents
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AtomResultsTransformer extends LambdaTransformerBase {

    static final String QUICKLOOK_URL_KEY = "${QUICKLOOK_URL}";

    static final String THUMB_URL_KEY = "${THUMB_URL}";

    static final String ATOM_URL_KEY = "${ATOM_URL}";

    static final String OM_METADATA_KEY = "${OM_METADATA_URL}";

    static final String ISO_METADATA_KEY = "${ISO_METADATA_LINK}";

    static final String BASE_URL_KEY = "${BASE_URL}";

    static final GMLConfiguration GML_CONFIGURATION = new GMLConfiguration();

    private OSEOInfo info;

    private GeoServerInfo gs;

    public AtomResultsTransformer(GeoServerInfo gs, OSEOInfo info) {
        this.info = info;
        this.gs = gs;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ResultsTranslator(handler);
    }

    class ResultsTranslator extends LambdaTranslatorSupport {

        public ResultsTranslator(ContentHandler contentHandler) {
            super(contentHandler);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            SearchResults results = (SearchResults) o;

            // xmlns:ical="http://www.w3.org/2002/12/cal/ical#"
            // xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            // xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/"
            // xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/"
            mapNamespacePrefix("", "http://www.w3.org/2005/Atom");
            mapNamespacePrefix("gml", "http://www.opengis.net/gml");
            mapNamespacePrefix("dc", "http://purl.org/dc/elements/1.1/");
            mapNamespacePrefix("dct", "http://purl.org/dc/terms/");
            mapNamespacePrefix("geo", "http://a9.com/-/opensearch/extensions/geo/1.0/");
            mapNamespacePrefix("time", "http://a9.com/-/opensearch/extensions/time/1.0");
            mapNamespacePrefix("eo", "http://a9.com/-/opensearch/extensions/eo/1.0/");
            mapNamespacePrefix("os", "http://a9.com/-/spec/opensearch/1.1/");
            mapNamespacePrefix("georss", "http://www.georss.org/georss");
            mapNamespacePrefix("xlink", "http://www.w3.org/1999/xlink");
            mapNamespacePrefix("xs", "http://www.w3.org/2001/XMLSchema");
            mapNamespacePrefix("sch", "http://www.ascc.net/xml/schematron");
            mapNamespacePrefix("owc", "http://www.opengis.net/owc/1.0");
            mapNamespacePrefix("media", "http://search.yahoo.com/mrss/");
            for (ProductClass pc : info.getProductClasses()) {
                mapNamespacePrefix(pc.getPrefix(), pc.getNamespace());
            }

            element("feed", () -> feedContents(results));
        }

        private void mapNamespacePrefix(String prefix, String namespaceURI) {
            try {
                contentHandler.startPrefixMapping(prefix, namespaceURI);
                contentHandler.endPrefixMapping(prefix);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }

        private void feedContents(SearchResults results) {
            final SearchRequest request = results.getRequest();

            element("os:totalResults", "" + results.getTotalResults());
            Integer startIndex = getQueryStartIndex(results) + 1;
            element("os:startIndex", "" + startIndex);
            element("os:itemsPerPage", "" + request.getQuery().getMaxFeatures());
            element("os:Query", NO_CONTENTS, getQueryAttributes(request));
            String organization = gs.getSettings().getContact().getContactOrganization();
            if (organization != null) {
                element(
                        "author",
                        () -> {
                            element("name", organization);
                        });
            }
            String title = info.getTitle();
            if (title != null) {
                element("title", title);
            }
            String updated = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            element("updated", updated);
            buildPaginationLinks(results);
            buildSearchLink(results.getRequest());
            encodeEntries(results.getResults(), results.getRequest());
        }

        private void buildSearchLink(SearchRequest request) {
            Map<String, String> kvp = null;
            if (request.getParentId() != null) {
                kvp = Collections.singletonMap("parentId", request.getParentId());
            }
            String href =
                    ResponseUtils.buildURL(
                            request.getBaseUrl(), "oseo/search/description", kvp, URLType.SERVICE);
            element(
                    "link",
                    NO_CONTENTS,
                    attributes(
                            "rel",
                            "search",
                            "href",
                            href,
                            "type",
                            DescriptionResponse.OS_DESCRIPTION_MIME));
        }

        private int getQueryStartIndex(SearchResults results) {
            Integer startIndex = results.getRequest().getQuery().getStartIndex();
            if (startIndex == null) {
                startIndex = 0;
            }
            return startIndex;
        }

        private void buildPaginationLinks(SearchResults results) {
            final SearchRequest request = results.getRequest();
            int total = results.getTotalResults();
            int startIndex = getQueryStartIndex(results) + 1;
            int itemsPerPage = request.getQuery().getMaxFeatures();

            // warning, opensearch is 1-based, geotools is 0 based
            encodePaginationLink("self", startIndex, itemsPerPage, request);
            encodePaginationLink("first", 1, itemsPerPage, request);
            if (startIndex > 1) {
                encodePaginationLink(
                        "previous", Math.max(startIndex - itemsPerPage, 1), itemsPerPage, request);
            }
            if (startIndex + itemsPerPage <= total) {
                encodePaginationLink("next", startIndex + itemsPerPage, itemsPerPage, request);
            }
            encodePaginationLink(
                    "last", getLastPageStart(total, itemsPerPage), itemsPerPage, request);
        }

        private void encodeEntries(FeatureCollection results, SearchRequest request) {
            FeatureType schema = results.getSchema();
            final String schemaName = schema.getName().getLocalPart();
            BiConsumer<Feature, SearchRequest> entryEncoder;
            if (JDBCOpenSearchAccess.COLLECTION.equals(schemaName)) {
                entryEncoder = this::encodeCollectionEntry;
            } else if (JDBCOpenSearchAccess.PRODUCT.equals(schemaName)) {
                entryEncoder = this::encodeProductEntry;
            } else {
                throw new IllegalArgumentException("Unrecognized feature type " + schemaName);
            }

            try (FeatureIterator<Feature> fi = results.features()) {
                while (fi.hasNext()) {
                    Feature feature = fi.next();
                    element("entry", () -> entryEncoder.accept(feature, request));
                }
            }
        }

        private void encodeCollectionEntry(Feature feature, SearchRequest request) {
            final String identifier = (String) value(feature, EO_NAMESPACE, "identifier");

            // build links and description replacement variables
            String identifierLink = buildCollectionIdentifierLink(identifier, request);
            String osddLink = buildOsddLink(identifier, request);
            String metadataLink =
                    buildMetadataLink(null, identifier, MetadataRequest.ISO_METADATA, request);
            Map<String, String> descriptionVariables = new HashMap<>();
            descriptionVariables.put(ISO_METADATA_KEY, metadataLink);
            descriptionVariables.put(ATOM_URL_KEY, identifierLink);

            // generic contents
            encodeGenericEntryContents(
                    feature, identifier, identifierLink, descriptionVariables, request);

            // build links to the metadata
            element(
                    "link",
                    NO_CONTENTS,
                    attributes(
                            "rel",
                            "alternate",
                            "href",
                            metadataLink,
                            "type",
                            MetadataRequest.ISO_METADATA,
                            "title",
                            "ISO metadata"));

            // build link to the collection specific OSDD
            element(
                    "link",
                    NO_CONTENTS,
                    attributes(
                            "rel",
                            "search",
                            "href",
                            osddLink,
                            "type",
                            DescriptionResponse.OS_DESCRIPTION_MIME,
                            "title",
                            "Collection OSDD"));

            // OGC links
            encodeOgcLinksFromFeature(feature, request);
        }

        private void mediaContent(String quicklookLink) {
            element(
                    "media:content",
                    () -> {
                        element(
                                "media:category",
                                "THUMBNAIL",
                                attributes("scheme", "http://www.opengis.net/spec/EOMPOM/1.0"));
                    },
                    attributes("medium", "image", "type", "image/jpeg", "url", quicklookLink));
        }

        private void encodeOgcLinksFromFeature(Feature feature, SearchRequest request) {
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
                encodeOgcLinks(linksByOffering, hrefBase);
            }
        }

        private String getHRefBase(SearchRequest request) {
            String baseURL = request.getBaseUrl();
            String hrefBase = ResponseUtils.buildURL(baseURL, null, null, URLType.SERVICE);
            if (hrefBase.endsWith("/")) {
                hrefBase = hrefBase.substring(0, hrefBase.length() - 1);
            }
            return hrefBase;
        }

        private void encodeOgcLinks(
                Map<String, List<SimpleFeature>> linksByOffering, String hrefBase) {
            linksByOffering.forEach(
                    (offering, links) -> {
                        element(
                                "owc:offering",
                                () -> {
                                    for (SimpleFeature link : links) {
                                        encodeOgcLink(link, hrefBase);
                                    }
                                },
                                attributes("code", offering));
                    });
        }

        private void encodeOgcLink(SimpleFeature link, String hrefBase) {
            String method = (String) link.getAttribute("method");
            String code = (String) link.getAttribute("code");
            String type = (String) link.getAttribute("type");
            String href = (String) link.getAttribute("href");
            String hrefExpanded =
                    QuickTemplate.replaceVariables(
                            href, Collections.singletonMap(BASE_URL_KEY, hrefBase));
            element(
                    "owc:operation",
                    NO_CONTENTS,
                    attributes("method", method, "code", code, "href", hrefExpanded, "type", type));
        }

        private void encodeProductEntry(Feature feature, SearchRequest request) {
            final String identifier =
                    (String) value(feature, ProductClass.GENERIC.getNamespace(), "identifier");

            // encode the generic contents
            String productIdentifierLink = buildProductIdentifierLink(identifier, request);
            String metadataLink =
                    buildMetadataLink(
                            request.getParentId(),
                            identifier,
                            MetadataRequest.OM_METADATA,
                            request);
            String quicklookLink = buildQuicklookLink(identifier, request);
            Map<String, String> descriptionVariables = new HashMap<>();
            descriptionVariables.put(QUICKLOOK_URL_KEY, quicklookLink);
            descriptionVariables.put(THUMB_URL_KEY, quicklookLink);
            descriptionVariables.put(ATOM_URL_KEY, productIdentifierLink);
            descriptionVariables.put(OM_METADATA_KEY, metadataLink);
            encodeGenericEntryContents(
                    feature, identifier, productIdentifierLink, descriptionVariables, request);

            // build links to the metadata
            element(
                    "link",
                    NO_CONTENTS,
                    attributes(
                            "rel",
                            "alternate",
                            "href",
                            metadataLink,
                            "type",
                            MetadataRequest.OM_METADATA,
                            "title",
                            "O&M metadata"));

            // and a quicklook as a link and as media
            if (quicklookLink != null) {
                element(
                        "link",
                        NO_CONTENTS,
                        attributes(
                                "rel",
                                "icon",
                                "href",
                                quicklookLink,
                                "type",
                                "image/jpeg",
                                "title",
                                "Quicklook"));
                element("media:group", () -> mediaContent(quicklookLink));
            }

            encodeOgcLinksFromFeature(feature, request);

            encodeDownloadLink(feature, request);
        }

        private void encodeDownloadLink(Feature feature, SearchRequest request) {
            String location =
                    (String) value(feature, null, OpenSearchAccess.ORIGINAL_PACKAGE_LOCATION);
            if (location != null) {
                String type = (String) value(feature, null, OpenSearchAccess.ORIGINAL_PACKAGE_TYPE);
                if (type == null) {
                    type = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                String hrefBase = getHRefBase(request);
                String locationExpanded =
                        QuickTemplate.replaceVariables(
                                location, Collections.singletonMap(BASE_URL_KEY, hrefBase));
                element(
                        "link",
                        NO_CONTENTS,
                        attributes(
                                "rel",
                                "enclosure",
                                "href",
                                locationExpanded,
                                "type",
                                type,
                                "title",
                                "Source package download"));
            }
        }

        private void encodeGenericEntryContents(
                Feature feature,
                String name,
                final String identifierLink,
                Map<String, String> descriptionVariables,
                SearchRequest request) {
            element("id", identifierLink);
            element("title", name);
            element("dc:identifier", name);
            Date start = (Date) value(feature, "timeStart");
            Date end = (Date) value(feature, "timeEnd");
            if (start != null || end != null) {
                // TODO: need an actual update column
                Date updated = end == null ? start : end;
                String formattedUpdated = DateTimeFormatter.ISO_INSTANT.format(updated.toInstant());
                element("updated", formattedUpdated);

                // dc:date, can be a range
                String spec;
                if (start != null && end != null && start.equals(end)) {
                    spec = DateTimeFormatter.ISO_INSTANT.format(start.toInstant());
                } else {
                    spec =
                            start != null
                                    ? DateTimeFormatter.ISO_INSTANT.format(start.toInstant())
                                    : "";
                    spec += "/";
                    spec +=
                            end != null
                                    ? DateTimeFormatter.ISO_INSTANT.format(end.toInstant())
                                    : "";
                }
                element("dc:date", spec);
            }
            Geometry footprint = (Geometry) value(feature, "footprint");
            if (footprint != null) {
                // geometry is already in lat/lon order here
                Envelope envelope = footprint.getEnvelopeInternal();
                element("georss:where", () -> encodeGmlRssGeometry(footprint));
                element(
                        "georss:box",
                        envelope.getMinX()
                                + " "
                                + envelope.getMinY()
                                + " "
                                + envelope.getMaxX()
                                + " "
                                + envelope.getMaxY());
            }
            String htmlDescription = (String) value(feature, "htmlDescription");
            if (htmlDescription != null) {
                String expanded =
                        QuickTemplate.replaceVariables(htmlDescription, descriptionVariables);
                String expandedWithLinks = expanded + "\n" + encodeOGCLinksAsHTML(feature, request);
                element("summary", () -> cdata(expandedWithLinks), attributes("type", "html"));
            }
            // self link
            element(
                    "link",
                    NO_CONTENTS,
                    attributes(
                            "rel",
                            "self",
                            "href",
                            identifierLink,
                            "type",
                            AtomSearchResponse.MIME,
                            "title",
                            "self"));
        }

        private String encodeOGCLinksAsHTML(Feature feature, SearchRequest request) {
            Collection<Property> linkProperties =
                    feature.getProperties(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME);
            StringBuilder sb = new StringBuilder();
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
                if (linkProperties.size() > 0) {
                    sb.append("<h3>OGC cross links</h3>\n<ul>\n");
                    for (Map.Entry<String, List<SimpleFeature>> entry :
                            linksByOffering.entrySet()) {
                        final String key = entry.getKey();
                        int idx = key.lastIndexOf('/');
                        String service = key;
                        if (idx > 0 && idx < key.length() - 1) {
                            service = key.substring(idx + 1).toUpperCase();
                        }
                        sb.append("  <li><b>").append(service).append("</b>\n<ul>");
                        for (SimpleFeature link : entry.getValue()) {
                            String code = (String) link.getAttribute("code");
                            String href = (String) link.getAttribute("href");
                            String hrefExpanded =
                                    QuickTemplate.replaceVariables(
                                            href, Collections.singletonMap(BASE_URL_KEY, hrefBase));
                            sb.append("\n    <li><a href=\"")
                                    .append(hrefExpanded)
                                    .append("\">")
                                    .append(code)
                                    .append("</a></li>");
                        }
                        sb.append("</ul></li>\n");
                    }
                    sb.append("</ul>");
                }
            }

            return sb.toString();
        }

        private void encodeGmlRssGeometry(Geometry g) {
            try {
                // get the proper element name
                QName elementName = null;
                if (g instanceof Polygon) {
                    elementName = org.geotools.gml2.GML.Polygon;
                } else if (g instanceof MultiPoint) {
                    elementName = org.geotools.gml2.GML.MultiPoint;
                } else {
                    elementName = org.geotools.gml2.GML._Geometry;
                }

                // encode in GML3
                Encoder encoder = new Encoder(GML_CONFIGURATION);
                encoder.setInline(true);
                encoder.setIndenting(true);
                encoder.encode(g, elementName, contentHandler);
            } catch (Exception e) {
                throw new RuntimeException("Cannot transform the specified geometry in GML", e);
            }
        }

        private String buildCollectionIdentifierLink(Object identifier, SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            kvp.put("uid", String.valueOf(identifier));
            kvp.put("httpAccept", AtomSearchResponse.MIME);
            String href = ResponseUtils.buildURL(baseURL, "oseo/search", kvp, URLType.SERVICE);
            return href;
        }

        private String buildProductIdentifierLink(Object identifier, SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            kvp.put("parentId", request.getParentId());
            kvp.put("uid", String.valueOf(identifier));
            kvp.put("httpAccept", AtomSearchResponse.MIME);
            String href = ResponseUtils.buildURL(baseURL, "oseo/search", kvp, URLType.SERVICE);
            return href;
        }

        private String buildQuicklookLink(String identifier, SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            kvp.put("parentId", request.getParentId());
            kvp.put("uid", String.valueOf(identifier));
            String href = ResponseUtils.buildURL(baseURL, "oseo/quicklook", kvp, URLType.SERVICE);
            return href;
        }

        private String buildMetadataLink(
                String parentIdentifier,
                Object identifier,
                String mimeType,
                SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            if (parentIdentifier != null) {
                kvp.put("parentId", String.valueOf(parentIdentifier));
            }
            kvp.put("uid", String.valueOf(identifier));
            if (mimeType != null) {
                kvp.put("httpAccept", mimeType);
            }
            String href = ResponseUtils.buildURL(baseURL, "oseo/metadata", kvp, URLType.SERVICE);
            return href;
        }

        private String buildOsddLink(String parentIdentifier, SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            if (parentIdentifier != null) {
                kvp.put("parentId", String.valueOf(parentIdentifier));
            }
            String href = ResponseUtils.buildURL(baseURL, "oseo/description", kvp, URLType.SERVICE);
            return href;
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
                            && !CRS.equalsIgnoreMetadata(
                                    nativeCRS, OpenSearchParameters.OUTPUT_CRS)) {
                        Geometry g = (Geometry) value;
                        try {
                            return JTS.transform(
                                    g,
                                    CRS.findMathTransform(
                                            nativeCRS, OpenSearchParameters.OUTPUT_CRS));
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

        private int getLastPageStart(int total, int itemsPerPage) {
            // all in one page?
            if (total <= itemsPerPage || itemsPerPage == 0) {
                return 1;
            }
            // check how many items in the last page, is the last page partial or full?
            int lastPageItems = total % itemsPerPage;
            if (lastPageItems == 0) {
                lastPageItems = itemsPerPage;
            }
            return total - lastPageItems + 1;
        }

        private void encodePaginationLink(
                String rel, int startIndex, int itemsPerPage, SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            for (Map.Entry<Parameter, String> entry : request.getSearchParameters().entrySet()) {
                Parameter parameter = entry.getKey();
                String value = entry.getValue();
                String key = OpenSearchParameters.getQualifiedParamName(info, parameter, false);
                kvp.put(key, value);
            }
            kvp.put("startIndex", "" + startIndex);
            kvp.put("count", "" + itemsPerPage);
            kvp.put("httpAccept", AtomSearchResponse.MIME);
            String href = ResponseUtils.buildURL(baseURL, "oseo/search", kvp, URLType.SERVICE);
            element(
                    "link",
                    NO_CONTENTS,
                    attributes("rel", rel, "href", href, "type", AtomSearchResponse.MIME));
        }

        public Attributes getQueryAttributes(SearchRequest request) {
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
            return attributes(parameters);
        }
    }
}
