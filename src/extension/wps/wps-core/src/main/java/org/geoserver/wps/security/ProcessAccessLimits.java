/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.security;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ProcessAccessLimits extends AccessLimits {
    private static final long serialVersionUID = -3253977289877833644L;

    private boolean allowed;

    private String resource;

    public ProcessAccessLimits(CatalogMode mode, boolean allowed, String resource) {
        super(mode);
        this.resource = resource;
        this.allowed = allowed;
    }

    public boolean isAllowed() {
        checkCatalogMode();
        return this.allowed;
    }

    /*
     * Changes WPS permissions computed form rules configuration based on CATALOG MODE settings.
     * Following this rules:
     *
     * HIDE: GetCapabilities -> hides processes for not authorized roles, shows otherwise
     * DescribeProcess -> hides informations for not authorized roles, shows otherwise Execute ->
     * hides processes for not authorized roles, executes otherwise
     *
     * CHALLENGE: GetCapabilities -> shows processes for all DescribeProcess -> rise unauthorized
     * access exception for not authorized roles, shows informations otherwise Execute -> rise
     * unauthorized access exception for not authorized roles, executes otherwise
     *
     * MIXED: GetCapabilities -> hides processes for not authorized roles, shows otherwise
     * DescribeProcess -> rise unauthorized access exception for not authorized roles, shows
     * informations otherwise Execute -> rise unauthorized access exception for not authorized
     * roles, executes otherwise
     */
    private void checkCatalogMode() {
        if (!this.allowed) {
            Request request = Dispatcher.REQUEST.get();
            // If in HIDE mode stay hidden
            CatalogMode mode = getMode();
            if (mode == CatalogMode.MIXED) {
                // In MIXED mode the process stay hidden
                if (request == null || !"GetCapabilities".equalsIgnoreCase(request.getRequest())) {
                    throw unauthorizedAccess(resource);
                }
            } else if (mode == CatalogMode.CHALLENGE) {
                // In CHALLENGE mode the process is always visible
                this.allowed = true;
                // But throw unauthorized access in Execute and Describe request
                if (request != null
                        && !"GetCapabilities".equalsIgnoreCase(request.getRequest())
                        && ("Execute".equalsIgnoreCase(request.getRequest())
                                || "DescribeProcess".equalsIgnoreCase(request.getRequest()))) {
                    throw unauthorizedAccess(resource);
                }
            }
        }
    }

    private static RuntimeException unauthorizedAccess(String resourceName) {
        // not hide, and not filtering out a list, this
        // is an unauthorized direct resource access, complain
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user == null || user.getAuthorities().size() == 0)
            return new InsufficientAuthenticationException(
                    "Cannot access " + resourceName + " as anonymous");
        else
            return new AccessDeniedException(
                    "Cannot access " + resourceName + " with the current privileges");
    }
}
