/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.request;

import java.util.HashSet;
import java.util.Set;

import org.geoserver.security.AccessMode;
import org.geoserver.security.DataAccessManager.CatalogMode;
import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.role.XACMLRole;

import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

/**
 * Builds a request for testing access of geoserver to the catalog (always Permit) The idea here is
 * to pass back the {@link CatalogMode} in an XACML obligation.
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLRoleRequestCtxBuilder extends RequestCtxBuilder {
    public final static XACMLRole RoleEnablementRole = new XACMLRole(
            XACMLConstants.RoleEnablementRole);

    XACMLRole targetRole = null;

    String userName = null;

    public XACMLRoleRequestCtxBuilder(XACMLRole targetRole, String userName) {
        super(RoleEnablementRole, AccessMode.READ.toString());
        this.targetRole = targetRole;
        this.userName = userName;
    }

    @Override
    public RequestCtx createRequestCtx() {

        Set<Subject> subjects = new HashSet<Subject>(1);
        addRole(subjects);

        Set<Attribute> resources = new HashSet<Attribute>(1);
        addGeoserverResource(resources);
        addResource(resources, XACMLConstants.RoleEnablemetnResourceURI, targetRole.getAuthority());


        Set<Attribute> actions = new HashSet<Attribute>(1);
        addAction(actions);

        Set<Attribute> environment = new HashSet<Attribute>(1);
        if (userName != null) {
            environment.add(new Attribute(XACMLConstants.UserEnvironmentURI,null,null,new StringAttribute(userName)));            
        }

        
        RequestCtx ctx = new RequestCtx(subjects, resources, actions, environment);
        return ctx;

    }

}
