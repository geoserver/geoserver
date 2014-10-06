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
public class CatalogRequestCtxBuilder extends RequestCtxBuilder {
    public final static XACMLRole GeoServerRole = new XACMLRole(XACMLConstants.GeoServerRole);

    public CatalogRequestCtxBuilder() {
        super(GeoServerRole, AccessMode.READ.toString());
    }

    @Override
    public RequestCtx createRequestCtx() {

        Set<Subject> subjects = new HashSet<Subject>(1);
        addRole(subjects);

        Set<Attribute> resources = new HashSet<Attribute>(1);
        addGeoserverResource(resources);
        addResource(resources, XACMLConstants.CatalogResourceURI, XACMLConstants.CatalogResouceName);

        Set<Attribute> actions = new HashSet<Attribute>(1);
        addAction(actions);

        Set<Attribute> environment = new HashSet<Attribute>(1);

        RequestCtx ctx = new RequestCtx(subjects, resources, actions, environment);
        return ctx;

    }

}
