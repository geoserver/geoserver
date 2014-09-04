/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.request;

import java.util.HashSet;
import java.util.Set;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.role.XACMLRole;

import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

/**
 * Builds a request for workspace access control
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class WorkspaceRequestCtxBuilder extends RequestCtxBuilder {
    private String workspaceName = null;

    public String getWorkspaceName() {
        return workspaceName;
    }

    public WorkspaceRequestCtxBuilder(XACMLRole role, WorkspaceInfo workspace, AccessMode mode) {
        super(role, mode.toString());
        this.workspaceName = workspace.getName();
    }

    @Override
    public RequestCtx createRequestCtx() {

        Set<Subject> subjects = new HashSet<Subject>(1);
        addRole(subjects);

        Set<Attribute> resources = new HashSet<Attribute>(1);
        addGeoserverResource(resources);
        addResource(resources, XACMLConstants.WorkspaceURI, workspaceName);

        Set<Attribute> actions = new HashSet<Attribute>(1);
        addAction(actions);

        Set<Attribute> environment = new HashSet<Attribute>(1);

        RequestCtx ctx = new RequestCtx(subjects, resources, actions, environment);
        return ctx;
    }

}
