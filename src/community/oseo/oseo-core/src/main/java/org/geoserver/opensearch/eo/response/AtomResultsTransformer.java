/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.geoserver.opensearch.eo.SearchResults;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

/**
 * Transforms results into ATOM documents
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AtomResultsTransformer extends LambdaTransformerBase {

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
            element("os:totalResults", "" + results.getTotalResults());
            Integer startIndex = results.getRequest().getQuery().getStartIndex();
            if (startIndex == null) {
                startIndex = 1;
            }
            element("os:startIndex", "" + startIndex);
        }

//        
//
//        private String buildSelfUrl() {
//            String baseURL = baseURL(request.getHttpRequest());
//            return buildURL(baseURL, "oseo/description", null, URLType.SERVICE);
//        }

    }

}
