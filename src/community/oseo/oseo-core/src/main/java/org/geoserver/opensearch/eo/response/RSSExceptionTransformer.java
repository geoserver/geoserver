/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.response;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

/**
 * Based on the indications at
 * http://www.opensearch.org/Documentation/Developer_how_to_guide#How_to_indicate_errors encodes the
 * exception into a RSS document, e.g.
 *
 * <pre>{@code
 * <rss version="2.0" xmlns:openSearch="http://a9.com/-/spec/opensearch/1.1/">
 * <channel>
 * <title>title</title>
 * <link>link</link>
 * <description>description</description>
 * <openSearch:totalResults>1</openSearch:totalResults>
 * <openSearch:startIndex>1</openSearch:startIndex>
 * <openSearch:itemsPerPage>1</openSearch:itemsPerPage>
 * <item>
 * <title>Error</title>
 * <description>error message</description>
 * </item>
 * </channel>
 * }</pre>
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RSSExceptionTransformer extends LambdaTransformerBase {

    Request request;

    GeoServerInfo geoServer;

    public RSSExceptionTransformer(GeoServerInfo geoServer, Request request) {
        this.request = request;
        this.geoServer = geoServer;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ExceptionTranslator(handler);
    }

    public static String getDescription(GeoServerInfo geoServer, ServiceException e) {
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(e, sb, true);

        if (geoServer.getSettings().isVerboseExceptions()) {
            ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(stackTrace));

            sb.append("\nDetails:\n");
            sb.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
        }

        return sb.toString();
    }

    class ExceptionTranslator extends LambdaTranslatorSupport {

        public ExceptionTranslator(ContentHandler contentHandler) {
            super(contentHandler);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            ServiceException e = (ServiceException) o;
            element(
                    "rss",
                    () -> channel(e), //
                    attributes("xmlns:opensearch", "http://a9.com/-/spec/opensearch/1.1/"));
        }

        private void channel(ServiceException e) {
            element(
                    "channel",
                    () -> {
                        element("title", "OpenSearch for EO Error report");
                        element("link", buildSelfUrl());
                        element("opensearch:totalResults", "1");
                        element("opensearch:startIndex", "1");
                        element("opensearch:itemsPerPage", "1");
                        element("item", () -> itemContents(e));
                    });
        }

        private void itemContents(ServiceException e) {
            element("title", e.getMessage());
            element("description", getDescription(geoServer, e));
        }

        private String buildSelfUrl() {
            String baseURL = baseURL(request.getHttpRequest());
            return buildURL(baseURL, "oseo/description", null, URLType.SERVICE);
        }
    }
}
