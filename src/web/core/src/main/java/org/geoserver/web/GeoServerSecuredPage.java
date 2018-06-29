/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.request.http.WebRequest;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * Base class for secured web pages. By default it only allows
 *
 * @author Andrea Aime - TOPP
 */
public class GeoServerSecuredPage extends GeoServerBasePage {

    public static final ComponentAuthorizer DEFAULT_AUTHORIZER = new DefaultPageAuthorizer();
    public static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST_KEY";

    public GeoServerSecuredPage() {
        super();

        if (GeoServerSecurityFilterChainProxy.isSecurityEnabledForCurrentRequest() == false)
            return; // nothing to do

        Authentication auth = getSession().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            // emulate what spring security url control would do so that we get a proper redirect
            // after login
            HttpServletRequest httpRequest =
                    (HttpServletRequest) ((WebRequest) getRequest()).getContainerRequest();
            // ExceptionTranslationFilter translator = (ExceptionTranslationFilter)
            // getGeoServerApplication().getBean("consoleExceptionTranslationFilter");
            SavedRequest savedRequest =
                    new DefaultSavedRequest(httpRequest, new PortResolverImpl());

            HttpSession session = httpRequest.getSession();
            // TODO, Justin, WebAttributes.SAVED_REQUEST has disappeared in spring security
            // framework
            session.setAttribute(SAVED_REQUEST, savedRequest);

            // then redirect to the login page
            throw new RestartResponseException(GeoServerLoginPage.class);
        } else if (!getPageAuthorizer().isAccessAllowed(this.getClass(), auth))
            throw new RestartResponseException(UnauthorizedPage.class);
    }

    /**
     * Override to use a page authorizer other than the default one. When you do so, remember to
     * perform the same change in the associated {@link MenuPageInfo} instance
     */
    protected ComponentAuthorizer getPageAuthorizer() {
        return DEFAULT_AUTHORIZER;
    }

    /**
     * Convenience method to determine if the current user is authenticated as full administartor.
     */
    protected boolean isAuthenticatedAsAdmin() {

        return ComponentAuthorizer.ADMIN.isAccessAllowed(
                GeoServerSecuredPage.class, SecurityContextHolder.getContext().getAuthentication());
    }
}
