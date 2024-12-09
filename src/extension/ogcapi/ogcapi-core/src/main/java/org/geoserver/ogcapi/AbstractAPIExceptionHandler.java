/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Base class for exception handling. Has support for extracting basic information from common
 * exception types.
 */
public abstract class AbstractAPIExceptionHandler implements APIExceptionHandler {
    protected static final Logger LOGGER = Logging.getLogger(DefaultAPIExceptionHandler.class);
    protected GeoServer geoServer;

    public AbstractAPIExceptionHandler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public boolean canHandle(Throwable t, APIRequestInfo request) {
        return true;
    }

    @Override
    public void handle(Throwable t, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String type = null;
        boolean statusSet = false;
        String title = null;
        if (t instanceof OWS20Exception) {
            OWS20Exception ex = (OWS20Exception) t;
            if (ex.getHttpCode() != null) {
                response.setStatus(ex.getHttpCode());
                statusSet = true;
            }
        } else if (t instanceof OWS20Exception) {
            OWS20Exception t2 = (OWS20Exception) t;
            response.setStatus(t2.getHttpCode());
            type = t2.getCode();
            statusSet = true;
        } else if (t instanceof ServiceException) {
            type = ((ServiceException) t).getCode();
            OWS20Exception.OWSExceptionCode o20Code =
                    OWS20Exception.OWSExceptionCode.getByCode(type);
            if (o20Code != null) {
                response.setStatus(o20Code.getHttpCode());
                statusSet = true;
            }
        } else if (t instanceof APIException) {
            APIException ae = (APIException) t;
            response.setStatus(ae.getStatus().value());
            statusSet = true;
            type = ae.getCode();
        } else if (t instanceof MethodArgumentTypeMismatchException) {
            response.setStatus(400);
            statusSet = true;
            type = OWS20Exception.INVALID_PARAMETER_VALUE;
            title =
                    "Invalid syntax "
                            + ((MethodArgumentTypeMismatchException) t).getValue()
                            + " for parameter "
                            + ((MethodArgumentTypeMismatchException) t)
                                    .getParameter()
                                    .getParameterName();
        }
        if (!statusSet) response.setStatus(500);
        if (type == null) type = OWS20Exception.NO_APPLICABLE_CODE;
        if (title == null) title = getDescription(t);

        writeResponse(response, t, type, title);
    }

    protected abstract void writeResponse(
            HttpServletResponse response, Throwable t, String type, String title);

    protected String getDescription(Throwable t) {
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(t, sb, false);
        return sb.toString();
    }

    protected String getStackTrace(Throwable t) {
        String details = null;
        if (geoServer.getSettings().isVerboseExceptions()) {
            ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(stackTrace));
            details = new String(stackTrace.toByteArray());
        }
        return details;
    }
}
