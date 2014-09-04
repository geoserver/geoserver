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
import org.geoserver.xacml.geoxacml.GeoXACMLConfig;
import org.geoserver.xacml.role.XACMLRole;

/**
 * Interface for RequestCtxBuilders User can do an own implementation and configure in
 * applicationContext.xml The Factory is instantiated in {@link GeoXACMLConfig}
 * 
 * @author Christian Mueller
 * 
 */
public interface RequestCtxBuilderFactory {
    public RequestCtxBuilder getCatalogRequestCtxBuilder();

    public RequestCtxBuilder getXACMLRoleRequestCtxBuilder(XACMLRole targetRole, String userName);

    public RequestCtxBuilder getWorkspaceRequestCtxBuilder(XACMLRole role, WorkspaceInfo info,
            AccessMode mode);

    public RequestCtxBuilder getURLMatchRequestCtxBuilder(XACMLRole role, String urlString,
            String method, Map<String, Object> httpParams,String remoteIP,String remoteHost);

    public RequestCtxBuilder getResourceInfoRequestCtxBuilder(XACMLRole role,
            ResourceInfo resourceInfo, AccessMode mode);
}
