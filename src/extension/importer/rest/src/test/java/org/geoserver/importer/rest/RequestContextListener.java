/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AbstractResourceAccessManager;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The purpose of this class is to test that requests spring context are correctly shared between
 * multiple threads. This happens for example when we perform a job asynchronously.
 */
public final class RequestContextListener extends AbstractResourceAccessManager {

    private CallBack callBack;

    public interface CallBack {
        void invoked(HttpServletRequest request, Authentication user, CatalogInfo info);
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if (callBack != null) {
            // let's see if we can access this request context
            try {
                HttpServletRequest request =
                        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                                .getRequest();
                callBack.invoked(request, user, resource);
            } catch (Exception exception) {
                // the call back should have test if the request context was properly obtained
            }
        }
        return super.getAccessLimits(user, resource);
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        if (callBack != null) {
            // let's see if we can access this request context
            try {
                HttpServletRequest request =
                        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                                .getRequest();
                callBack.invoked(request, user, workspace);
            } catch (Exception exception) {
                // the call back should have test if the request context was properly obtained
            }
        }
        return super.getAccessLimits(user, workspace);
    }
}
