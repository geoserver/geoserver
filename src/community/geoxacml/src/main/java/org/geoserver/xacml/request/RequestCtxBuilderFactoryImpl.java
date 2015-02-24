/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.request;

import java.util.Map;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.xacml.role.XACMLRole;

/**
 * Default implementation for {@link RequestCtxBuilderFactory}
 * 
 * @author Christian Mueller
 * 
 */
public class RequestCtxBuilderFactoryImpl implements RequestCtxBuilderFactory {

    public RequestCtxBuilder getCatalogRequestCtxBuilder() {
        return new CatalogRequestCtxBuilder();
    }

    public RequestCtxBuilder getXACMLRoleRequestCtxBuilder(XACMLRole targetRole, String userName) {
        return new XACMLRoleRequestCtxBuilder(targetRole, userName);
    }

    public RequestCtxBuilder getWorkspaceRequestCtxBuilder(XACMLRole role, WorkspaceInfo info,
            AccessMode mode) {
        return new WorkspaceRequestCtxBuilder(role, info, mode);
    }

    public RequestCtxBuilder getURLMatchRequestCtxBuilder(XACMLRole role, String urlString,
            String action, Map<String, Object> httpParams,String remoteIP,String remoteHost)  {
        return new URLMatchRequestCtxBuilder(role, urlString, action, httpParams,remoteIP,remoteHost);
    }

    public RequestCtxBuilder getResourceInfoRequestCtxBuilder(XACMLRole role,
            ResourceInfo resourceInfo, AccessMode mode) {
        return new ResourceInfoRequestCtxBuilder(role, resourceInfo, mode);
    }

}
