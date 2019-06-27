/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Handles all exceptions encoding them as a JSON response as indicated by the OGC standards */
@Component
public class DefaultAPIExceptionHandler implements APIExceptionHandler, ExtensionPriority {

    private static final Logger LOGGER = Logging.getLogger(DefaultAPIExceptionHandler.class);

    GeoServer geoServer;

    public DefaultAPIExceptionHandler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public boolean canHandle(Throwable t, APIRequestInfo request) {
        return true;
    }

    @Override
    public void handle(Throwable t, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String code = null;
        boolean statusSet = false;
        String description = null;
        if (t instanceof OWS20Exception) {
            OWS20Exception ex = (OWS20Exception) t;
            if (ex.getHttpCode() != null) {
                response.setStatus(ex.getHttpCode());
                statusSet = true;
            }
        } else if (t instanceof ServiceException) {
            code = ((ServiceException) t).getCode();
            OWS20Exception.OWSExceptionCode o20Code =
                    OWS20Exception.OWSExceptionCode.getByCode(code);
            if (o20Code != null) {
                response.setStatus(o20Code.getHttpCode());
                statusSet = true;
            }
        } else if (t instanceof APIException) {
            APIException ae = (APIException) t;
            response.setStatus(ae.getStatus().value());
            statusSet = true;
            code = ae.getCode();
        } else if (t instanceof MethodArgumentTypeMismatchException) {
            response.setStatus(400);
            statusSet = true;
            code = OWS20Exception.INVALID_PARAMETER_VALUE;
            description =
                    "Invalid syntax "
                            + ((MethodArgumentTypeMismatchException) t).getValue()
                            + " for parameter "
                            + ((MethodArgumentTypeMismatchException) t)
                                    .getParameter()
                                    .getParameterName();
        }
        if (!statusSet) response.setStatus(500);
        if (code == null) code = OWS20Exception.NO_APPLICABLE_CODE;
        if (description == null) description = getDescription(geoServer.getGlobal(), t);

        Map<String, String> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("description", description);
        try {
            ObjectMapper mapper = new ObjectMapper();
            ServletOutputStream os = response.getOutputStream();
            mapper.writeValue(os, error);
            os.flush();
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        }
    }

    private String getDescription(GeoServerInfo geoServer, Throwable t) {
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(t, sb, true);

        if (geoServer.getSettings().isVerboseExceptions()) {
            ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(stackTrace));

            sb.append("\nDetails:\n");
            sb.append(new String(stackTrace.toByteArray()));
        }

        return sb.toString();
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
