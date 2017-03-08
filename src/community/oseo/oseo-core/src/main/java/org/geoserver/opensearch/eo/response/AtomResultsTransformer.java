/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.xml.transform.Translator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * Transforms results into ATOM documents
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AtomResultsTransformer extends LambdaTransformerBase {

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
            // xmlns:georss="http://www.georss.org/georss"
            // xmlns:gml="http://www.opengis.net/gml"
            // xmlns:ical="http://www.w3.org/2002/12/cal/ical#"
            // xmlns:os="http://a9.com/-/spec/opensearch/1.1/"
            // xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            // xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/"
            // xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/"
            element("feed", () -> feedContents(results), //
                    attributes("xmlns", "http://www.w3.org/2005/Atom", //
                            "xmlns:dc", "http://purl.org/dc/elements/1.1/", //
                            "xmlns:dct", "http://purl.org/dc/terms/", //
                            "xmlns:geo", "http://a9.com/-/opensearch/extensions/geo/1.0/", //
                            "xmlns:time", "http://a9.com/-/opensearch/extensions/time/1.0", //
                            "xmlns:eo", "http://a9.com/-/opensearch/extensions/eo/1.0/", //
                            "xmlns:os", "http://a9.com/-/spec/opensearch/1.1/"));
        }

        private void feedContents(SearchResults results) {
            final SearchRequest request = results.getRequest();

            element("os:totalResults", "" + results.getTotalResults());
            Integer startIndex = getStartIndex(results);
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

        private int getStartIndex(SearchResults results) {
            Integer startIndex = results.getRequest().getQuery().getStartIndex();
            if (startIndex == null) {
                startIndex = 1;
            }
            return startIndex;
        }

        private void buildPaginationLinks(SearchResults results) {
            final SearchRequest request = results.getRequest();
            int total = results.getTotalResults();
            int startIndex = getStartIndex(results);
            int itemsPerPage = request.getQuery().getMaxFeatures();

            // warning, 1-based logic follows
            encodePaginationLink("self", startIndex, itemsPerPage, request);
            encodePaginationLink("first", 1, itemsPerPage, request);
            if (startIndex > 1) {
                encodePaginationLink("previous", Math.max(startIndex - itemsPerPage, 1),
                        itemsPerPage, request);
            }
            if (startIndex + itemsPerPage <= total) {
                encodePaginationLink("next", startIndex + itemsPerPage, itemsPerPage, request);
            }
            encodePaginationLink("last", getLastPageStart(total, itemsPerPage), itemsPerPage, request);
        }

        private int getLastPageStart(int total, int itemsPerPage) {
            // check how many items in the last page, is the last page partial or full?
            int lastPageItems = total % itemsPerPage;
            if(lastPageItems == 0) {
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

        //
        //
        // private String buildSelfUrl() {
        // String baseURL = baseURL(request.getHttpRequest());
        // return buildURL(baseURL, "oseo/description", null, URLType.SERVICE);
        // }

    }

}
