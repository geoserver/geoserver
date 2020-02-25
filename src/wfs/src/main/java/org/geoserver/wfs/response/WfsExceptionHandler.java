/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.OWS10ServiceExceptionHandler;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.json.JSONType;

/**
 * Handles a wfs service exception by producing an exception report.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Carlo Cancellieri - GeoSolutions
 */
public class WfsExceptionHandler extends OWS10ServiceExceptionHandler {

    GeoServer gs;

    /** @param services The wfs service descriptors. */
    public WfsExceptionHandler(List services, GeoServer gs) {
        super(services);
        this.gs = gs;
    }

    public WFSInfo getInfo() {
        return gs.getService(WFSInfo.class);
    }

    /** Encodes a ogc:ServiceExceptionReport to output. */
    public void handleServiceException(ServiceException exception, Request request) {

        boolean verbose = gs.getSettings().isVerboseExceptions();
        String charset = gs.getSettings().getCharset();
        // first of all check what kind of exception handling we must perform
        final String exceptions;
        try {
            exceptions = (String) request.getKvp().get("EXCEPTIONS");
            if (exceptions == null) {
                // use default
                handleDefault(exception, request, charset, verbose);
                return;
            }
        } catch (Exception e) {
            // width and height might be missing
            handleDefault(exception, request, charset, verbose);
            return;
        }
        if (JSONType.isJsonMimeType(exceptions)) {
            // use Json format
            JSONType.handleJsonException(LOGGER, exception, request, charset, verbose, false);
        } else if (JSONType.useJsonp(exceptions)) {
            // use JsonP format
            JSONType.handleJsonException(LOGGER, exception, request, charset, verbose, true);
        } else {
            handleDefault(exception, request, charset, verbose);
        }
    }

    private void handleDefault(
            ServiceException exception, Request request, String charset, boolean verbose) {
        if ("1.0.0".equals(request.getVersion())) {
            handle1_0(exception, request.getHttpResponse());
        } else {
            super.handleServiceException(exception, request);
        }
    }

    public void handle1_0(ServiceException e, HttpServletResponse response) {
        try {
            String tab = "   ";

            StringBuffer s = new StringBuffer();
            s.append("<?xml version=\"1.0\" ?>\n");
            s.append("<ServiceExceptionReport\n");
            s.append(tab + "version=\"1.2.0\"\n");
            s.append(tab + "xmlns=\"http://www.opengis.net/ogc\"\n");
            s.append(tab + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            s.append(tab);
            s.append("xsi:schemaLocation=\"http://www.opengis.net/ogc ");
            s.append(
                    ResponseUtils.appendPath(
                                    getInfo().getSchemaBaseURL(), "wfs/1.0.0/OGC-exception.xsd")
                            + "\">\n");

            s.append(tab + "<ServiceException");

            if ((e.getCode() != null) && !e.getCode().equals("")) {
                s.append(" code=\"" + ResponseUtils.encodeXML(e.getCode()) + "\"");
            }

            if ((e.getLocator() != null) && !e.getLocator().equals("")) {
                s.append(" locator=\"" + ResponseUtils.encodeXML(e.getLocator()) + "\"");
            }

            s.append(">");

            if (e.getMessage() != null) {
                s.append("\n" + tab + tab);
                OwsUtils.dumpExceptionMessages(e, s, true);

                if (verboseExceptions) {
                    ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream(stackTrace));

                    s.append("\nDetails:\n");
                    s.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
                }
            }

            s.append("\n</ServiceException>");
            s.append("</ServiceExceptionReport>");

            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getOutputStream().write(s.toString().getBytes());
            response.getOutputStream().flush();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
