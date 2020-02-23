/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * GeoServer {@link RequestMatcher} implementation.
 *
 * <p>The class is responsible for checking against {@link HTTPMethod} objects and the passed {@link
 * RequestMatcher} objects.
 *
 * @author christian
 */
public class GeoServerRequestMatcher implements RequestMatcher {

    private Set<HTTPMethod> methods;
    private RequestMatcher[] matchers;

    public GeoServerRequestMatcher(Set<HTTPMethod> methods, RequestMatcher... matchers) {
        this.methods = methods;
        //        if (methods !=null && methods.isEmpty())
        //            methods=null;
        this.matchers = matchers;
    }

    /**
     * First, the HTTP method is checked using {@link #matchesHTTPMethod(HttpServletRequest)}. If
     * <code>true</code>, the request is checked against the {@link RequestMatcher} objects in
     * {@link #matchers}. The first match returns <code>true</code>.
     *
     * <p>If no match occurs, return <code>false</code>.
     */
    @Override
    public boolean matches(HttpServletRequest request) {

        if (matchesHTTPMethod(request) == false) return false;

        if (matchers == null) return false;

        for (RequestMatcher matcher : matchers) {
            if (matcher.matches(request)) return true;
        }
        return false;
    }

    /**
     * If {@link #methods} is <code>null</code>, the return value is always <code>true</code>.
     *
     * <p>Return <code>true</code> if the HTTP method is contained in {@link #methods}
     */
    protected boolean matchesHTTPMethod(HttpServletRequest request) {
        if (methods == null) return true;
        HTTPMethod method = HTTPMethod.fromString(request.getMethod());
        return methods.contains(method);
    }
}
