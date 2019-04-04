/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.response.RSSExceptionTransformer;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;

/**
 * Returns exceptions as a RSS feed, as suggested in the OpenSearch EO developer guide at
 * http://www.opensearch.org/Documentation/Developer_how_to_guide
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOExceptionHandler extends ServiceExceptionHandler {

    static final String RSS_MIME = "application/rss+xml";

    private GeoServer geoServer;

    public OSEOExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        HttpServletResponse response = request.getHttpResponse();
        response.setContentType("application/xml");

        if (exception instanceof OWS20Exception) {
            OWS20Exception ex = (OWS20Exception) exception;
            if (ex.getHttpCode() != null) {
                response.setStatus(ex.getHttpCode());
            } else {
                response.setStatus(500);
            }
        } else {
            response.setStatus(500);
        }

        try {
            new RSSExceptionTransformer(geoServer.getGlobal(), request)
                    .transform(exception, response.getOutputStream());
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        } finally {
            try {
                request.getHttpResponse().getOutputStream().flush();
            } catch (IOException ioe) {
            }
        }
    }
}
