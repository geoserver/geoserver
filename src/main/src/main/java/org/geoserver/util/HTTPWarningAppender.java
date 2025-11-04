/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.springframework.http.HttpHeaders;

/** Appends warning messages in case of nearest match or default value */
public class HTTPWarningAppender extends AbstractDispatcherCallback {

    static final ThreadLocal<Set<DimensionWarning>> WARNINGS = ThreadLocal.withInitial(() -> new LinkedHashSet<>());

    @Override
    public Request init(Request request) {
        // just to make sure there is no accumulation in the GWC caching paths, as they
        // setup internal requests with potentially no cleanup, and because code calling
        // getWarnings or addWarning will trigger instantiation of a set in the TL anyways
        WARNINGS.remove();
        return request;
    }

    /** Adds a dimension warning, so that it can be added into the response HTTP headers */
    public static void addWarning(DimensionWarning warning) {
        Set<DimensionWarning> warnings = WARNINGS.get();
        warnings.add(warning);
    }

    /** Retuns the warnings collected so far, as an immutable set */
    public static Set<DimensionWarning> getWarnings() {
        return Collections.unmodifiableSet(WARNINGS.get());
    }

    /** Returns true if the collected warnings match any of the provided types, false otherwise */
    public static boolean anyMatch(Set<DimensionWarning.WarningType> types) {
        Objects.requireNonNull(types);
        return WARNINGS.get().stream().anyMatch(w -> types.contains(w.getWarningType()));
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return super.operationExecuted(request, operation, result);
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        Set<DimensionWarning> warnings = WARNINGS.get();
        if (warnings != null && !warnings.isEmpty()) {
            HttpServletResponse httpResponse = request.getHttpResponse();
            for (DimensionWarning warning : warnings) {
                httpResponse.addHeader(HttpHeaders.WARNING, warning.getHeader());
            }
        }

        return response;
    }

    @Override
    public void finished(Request request) {
        WARNINGS.remove();
    }
}
