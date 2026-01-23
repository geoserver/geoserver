/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Ensures Bearer-token requests stay stateless even when a session-based login is enabled.
 *
 * <p>When {@code Authorization: Bearer <token>} is present, this repository stores the {@link SecurityContext} only for
 * the current request (request attributes) and never writes it to the HTTP session. For all other requests it behaves
 * like the default {@link HttpSessionSecurityContextRepository}.
 */
public final class BearerAwareSecurityContextRepository implements SecurityContextRepository {

    private final SecurityContextRepository sessionRepo = new HttpSessionSecurityContextRepository();
    private final SecurityContextRepository bearerRepo = new RequestAttributeSecurityContextRepository();

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        HttpServletRequest request = requestResponseHolder.getRequest();
        return isBearerRequest(request)
                ? bearerRepo.loadContext(requestResponseHolder)
                : sessionRepo.loadContext(requestResponseHolder);
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        if (isBearerRequest(request)) {
            bearerRepo.saveContext(context, request, response);
        } else {
            sessionRepo.saveContext(context, request, response);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return isBearerRequest(request) ? bearerRepo.containsContext(request) : sessionRepo.containsContext(request);
    }

    static boolean isBearerRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String authz = request.getHeader("Authorization");
        if (authz == null) {
            return false;
        }
        // RFC 6750 allows the scheme to be case-insensitive
        return authz.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
    }
}
