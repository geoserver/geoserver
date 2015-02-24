/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.request;

import java.util.HashSet;
import java.util.Set;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.role.XACMLRole;

import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

/**
 * Builds a request for layer info access control
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class ResourceInfoRequestCtxBuilder extends RequestCtxBuilder {
    private String resourceName = null;

    private String workspaceName = null;

    public String getResouceName() {
        return resourceName;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public ResourceInfoRequestCtxBuilder(XACMLRole role, ResourceInfo resourceInfo, AccessMode mode) {
        super(role, mode.toString());
        this.resourceName = resourceInfo.getName();
        if (resourceInfo.getNamespace() != null) {
            this.workspaceName = resourceInfo.getNamespace().getName();
            if (this.workspaceName == null)
                this.workspaceName = resourceInfo.getNamespace().getURI();
        } else {
            this.workspaceName = resourceInfo.getStore().getWorkspace().getName();
        }
    }

    @Override
    public RequestCtx createRequestCtx() {

        Set<Subject> subjects = new HashSet<Subject>(1);
        addRole(subjects);

        Set<Attribute> resources = new HashSet<Attribute>(1);
        addGeoserverResource(resources);
        addOWSService(resources);
        addResource(resources, XACMLConstants.WorkspaceURI, workspaceName);
        addResource(resources, XACMLConstants.GeoServerResouceURI, resourceName);
        addBbox(resources);

        Set<Attribute> actions = new HashSet<Attribute>(1);
        addAction(actions);

        Set<Attribute> environment = new HashSet<Attribute>(1);

        RequestCtx ctx = new RequestCtx(subjects, resources, actions, environment);
        return ctx;
    }

}
