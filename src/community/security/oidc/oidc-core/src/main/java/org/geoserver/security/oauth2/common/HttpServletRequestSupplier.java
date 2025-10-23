/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Provides access to the current {@link HttpServletRequest}.
 *
 * @author awaterme
 */
public class HttpServletRequestSupplier implements Supplier<HttpServletRequest> {

    @Override
    public HttpServletRequest get() {
        ServletRequestAttributes lAttrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (lAttrs == null) {
            throw new IllegalStateException("Failed to obtain ServletRequestAttributes.");
        }
        return lAttrs.getRequest();
    }
}
