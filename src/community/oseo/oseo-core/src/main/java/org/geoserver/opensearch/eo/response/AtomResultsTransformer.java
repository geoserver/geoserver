/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.xml.namespace.QName;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
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
import org.geotools.xml.Encoder;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Transforms results into ATOM documents
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AtomResultsTransformer extends LambdaTransformerBase {

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
                element("author", () -> {
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
                encodePaginationLink("previous", Math.max(startIndex - itemsPerPage, 1),
                        itemsPerPage, request);
            }
            if (startIndex + itemsPerPage <= total) {
                encodePaginationLink("next", startIndex + itemsPerPage, itemsPerPage, request);
            }
            encodePaginationLink("last", getLastPageStart(total, itemsPerPage), itemsPerPage,
                    request);
            encodeEntries(results.getResults(), results.getRequest());
        }

        private void encodeEntries(FeatureCollection results, SearchRequest request) {
            FeatureType schema = results.getSchema();
            final String schemaName = schema.getName().getLocalPart();
            BiConsumer<Feature, SearchRequest> entryEncoder;
            if (JDBCOpenSearchAccess.COLLECTION.equals(schemaName)) {
                entryEncoder = this::encodeCollectionEntry;
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
            final String buildCollectionIdentifierLink = buildCollectionIdentifierLink(value(feature, EO_NAMESPACE, "identifier"),
                    request);
            element("id", buildCollectionIdentifierLink);
            element("title", (String) value(feature, "name"));
            // TODO: need an actual update column
            Date updated = (Date) value(feature, "timeStart");
            if (updated != null) {
                String formattedUpdated = DateTimeFormatter.ISO_INSTANT.format(updated.toInstant());
                element("updated", formattedUpdated);
            }
            Geometry footprint = (Geometry) value(feature, "footprint");
            if (footprint != null) {
                element("georss:where", () -> encodeGmlRssGeometry(footprint));
            }
            String htmlDescription = (String) value(feature, "htmlDescription");
            if (htmlDescription != null) {
                element("summary", () -> cdata(htmlDescription), attributes("type", "html"));
            }
            // self link
            element("link", NO_CONTENTS,
                    attributes("rel", "alternate", "href", buildCollectionIdentifierLink, "type", AtomSearchResponse.MIME));

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

        private Object value(Feature feature, String attribute) {
            String prefix = feature.getType().getName().getNamespaceURI();
            return value(feature, prefix, attribute);
        }

        private Object value(Feature feature, String prefix, String attribute) {
            Property property = feature.getProperty(new NameImpl(prefix, attribute));
            if (property == null) {
                return null;
            } else {
                Object value = property.getValue();
                if(value instanceof Geometry) {
                    // cheap reprojection support since there is no reprojecting collection
                    // wrapper for complex features
                    CoordinateReferenceSystem nativeCRS = ((GeometryDescriptor) property.getDescriptor()).getCoordinateReferenceSystem();
                    if(nativeCRS != null && !CRS.equalsIgnoreMetadata(nativeCRS, OpenSearchParameters.OUTPUT_CRS)) {
                        Geometry g = (Geometry) value;
                        try {
                            return JTS.transform(g, CRS.findMathTransform(nativeCRS, OpenSearchParameters.OUTPUT_CRS));
                        } catch (MismatchedDimensionException | TransformException
                                | FactoryException e) {
                            throw new OWS20Exception("Failed to reproject geometry to EPSG:4326 lat/lon", e);
                        }
                    }
                }
                return value;
            }
        }

        private int getLastPageStart(int total, int itemsPerPage) {
            // check how many items in the last page, is the last page partial or full?
            int lastPageItems = total % itemsPerPage;
            if (lastPageItems == 0) {
                lastPageItems = itemsPerPage;
            }
            return total - lastPageItems + 1;
        }

        private void encodePaginationLink(String rel, int startIndex, int itemsPerPage,
                SearchRequest request) {
            String baseURL = request.getBaseUrl();
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            for (Map.Entry<Parameter, String> entry : request.getSearchParameters().entrySet()) {
                Parameter parameter = entry.getKey();
                String value = entry.getValue();
                String key = OpenSearchParameters.getQualifiedParamName(parameter, false);
                kvp.put(key, value);
            }
            kvp.put("startIndex", "" + startIndex);
            kvp.put("count", "" + itemsPerPage);
            kvp.put("httpAccept", AtomSearchResponse.MIME);
            String href = ResponseUtils.buildURL(baseURL, "oseo/search", kvp, URLType.SERVICE);
            element("link", NO_CONTENTS,
                    attributes("rel", rel, "href", href, "type", AtomSearchResponse.MIME));
        }

        public Attributes getQueryAttributes(SearchRequest request) {
            // turn each request parameter into an attribute for os:Query
            Map<String, String> parameters = new LinkedHashMap<>();
            for (Map.Entry<Parameter, String> entry : request.getSearchParameters().entrySet()) {
                Parameter parameter = entry.getKey();
                String value = entry.getValue();
                String key = OpenSearchParameters.getQualifiedParamName(parameter, false);
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
