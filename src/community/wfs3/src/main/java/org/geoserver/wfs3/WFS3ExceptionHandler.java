/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;

/**
 * Returns exceptions as a JSON document according to the WFS 3 draft spec
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WFS3ExceptionHandler extends ServiceExceptionHandler {

    private GeoServer geoServer;

    public WFS3ExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    @Override
    public void handleServiceException(ServiceException exception, Request request) {
        HttpServletResponse response = request.getHttpResponse();
        response.setContentType(BaseRequest.JSON_MIME);

        if (exception instanceof OWS20Exception) {
            OWS20Exception ex = (OWS20Exception) exception;
            if (ex.getHttpCode() != null) {
                response.setStatus(ex.getHttpCode());
            } else {
                response.setStatus(500);
            }
        } else {
            OWS20Exception.OWSExceptionCode code =
                    OWS20Exception.OWSExceptionCode.getByCode(exception.getCode());
            if (code != null) {
                response.setStatus(code.getHttpCode());
            } else {
                response.setStatus(500);
            }
        }

        Map<String, String> error = new LinkedHashMap<>();
        error.put("code", exception.getCode());
        error.put("description", getDescription(geoServer.getGlobal(), exception));
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), error);
        } catch (Exception ex) {
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        } finally {
            try {
                request.getHttpResponse().getOutputStream().flush();
            } catch (IOException ignored) {
            }
        }
    }

    private String getDescription(GeoServerInfo geoServer, ServiceException e) {
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(e, sb, true);

        if (geoServer.getSettings().isVerboseExceptions()) {
            ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(stackTrace));

            sb.append("\nDetails:\n");
            sb.append(new String(stackTrace.toByteArray()));
        }

        return sb.toString();
    }
}
