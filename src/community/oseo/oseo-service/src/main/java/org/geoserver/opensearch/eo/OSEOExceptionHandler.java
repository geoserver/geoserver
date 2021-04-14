/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
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

    private GeoServer geoServer;

    public OSEOExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        HttpServletResponse response = request.getHttpResponse();
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
            String format = (String) request.getKvp().get("httpAccept");
            if (format != null && format.contains("json")) writeJSONResponse(exception, request);
            else writeXMLResponse(exception, request);
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        }
    }

    private void writeJSONResponse(ServiceException se, Request request) throws IOException {
        ExceptionReport.Exception exception =
                new ExceptionReport.Exception(se.getCode(), getExceptionText(se), se.getLocator());
        ExceptionReport report = new ExceptionReport(exception);
        try (ServletOutputStream os = request.getHttpResponse().getOutputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(os, report);
            os.flush();
        }
    }

    private String getExceptionText(ServiceException exception) {
        List<String> text = exception.getExceptionText();
        if (text != null && !text.isEmpty()) return text.stream().collect(Collectors.joining("\n"));
        return exception.getMessage();
    }

    private void writeXMLResponse(ServiceException exception, Request request)
            throws IOException, TransformerException {
        HttpServletResponse response = request.getHttpResponse();
        response.setContentType("application/xml");

        try (OutputStream os = request.getHttpResponse().getOutputStream()) {
            new RSSExceptionTransformer(geoServer.getGlobal(), request).transform(exception, os);
            os.flush();
        }
    }
}
