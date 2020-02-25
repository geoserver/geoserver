/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Interceptor that attaches the RepositoryProvider to all requests that go to the /geogig route.
 */
@Component
public class GeogigRequestInterceptor extends HandlerInterceptorAdapter {

    public static RepositoryProvider repoProvider = null;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (repoProvider == null) {
            repoProvider = new GeoServerRepositoryProvider();
        }
        request.setAttribute(RepositoryProvider.KEY, repoProvider);
        return true;
    }
}
