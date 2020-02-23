/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.springframework.http.HttpHeaders;

/** Appends warning messages in case of nearest match */
public class NearestMatchWarningAppender extends AbstractDispatcherCallback {

    static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    public enum WarningType {
        Nearest,
        Default,
        NotFound
    }

    static final ThreadLocal<List<String>> WARNINGS =
            ThreadLocal.withInitial(() -> new ArrayList<>());

    /**
     * Adds a default value or nearest value warning.
     *
     * @param layerName Mandatory, the layer being used
     * @param dimension The dimension name
     * @param value The actual value used, or null if it's a {@link WarningType#NotFound} warning
     *     type
     * @param unit The measure unit of measure
     */
    public static void addWarning(
            String layerName,
            String dimension,
            Object value,
            String unit,
            WarningType warningType) {
        List<String> warnings = WARNINGS.get();
        if (warningType == WarningType.NotFound) {
            warnings.add("99 No nearest value found on " + layerName + ": " + dimension);
        } else {
            String type = (warningType == WarningType.Nearest) ? "Nearest value" : "Default value";
            String unitSpec = unit == null ? "" : unit;
            String valueSpec = formatValue(value);
            warnings.add(
                    "99 " + type + " used: " + dimension + "=" + valueSpec + " " + unitSpec + " ("
                            + layerName + ")");
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(UTC_TZ);
            return sdf.format(value);
        } else if (value == null) {
            return "-";
        } else {
            // numbers mostly?
            return value.toString();
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        List<String> warnings = WARNINGS.get();
        if ("WMS".equalsIgnoreCase(request.getService())
                && warnings != null
                && !warnings.isEmpty()) {
            HttpServletResponse httpResponse = request.getHttpResponse();
            for (String warning : warnings) {
                httpResponse.addHeader(HttpHeaders.WARNING, warning);
            }
            return super.responseDispatched(request, operation, result, response);
        }

        return response;
    }

    @Override
    public void finished(Request request) {
        WARNINGS.remove();
    }
}
