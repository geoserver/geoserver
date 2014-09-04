/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.request;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geoserver.ows.Dispatcher;
import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.geoxacml.XACMLUtil;
import org.geoserver.xacml.role.XACMLRole;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.xacml.geoxacml.attr.GMLVersion;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.vfny.geoserver.Request;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Base class for geoxacml request context builders The class inheritance structure is mirrored from
 * {@link Request}
 * 
 * 
 * @author Christian Mueller
 * 
 */
public abstract class RequestCtxBuilder extends Object {

    private XACMLRole role;

    private String action;

    public XACMLRole getRole() {
        return role;
    }

    protected RequestCtxBuilder(XACMLRole role, String action) {
        this.role = role;
        this.action = action;
    }

    protected void addRole(Set<Subject> subjects) {

        URI roleURI = null;
        try {
            roleURI = new URI(role.getAuthority());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Set<Attribute> subjectAttributes = new HashSet<Attribute>(1 + role.getAttributes().size());

        AttributeValue roleAttributeValue = new AnyURIAttribute(roleURI);
        Attribute roleAttribute = new Attribute(XACMLConstants.RoleAttributeURI, null, null,
                roleAttributeValue);
        subjectAttributes.add(roleAttribute);

        for (Attribute attr : role.getAttributes()) {
            subjectAttributes.add(attr);
        }

        Subject subject = new Subject(subjectAttributes);
        subjects.add(subject);

    }

    protected void addAction(Set<Attribute> actions) {
        actions.add(new Attribute(XACMLConstants.ActionAttributeURI, null, null,
                new StringAttribute(action)));
    }

    protected void addResource(Set<Attribute> resources, URI id, String resourceName) {
        resources.add(new Attribute(id, null, null, new StringAttribute(resourceName)));
    }

    protected void addGeoserverResource(Set<Attribute> resources) {
        resources.add(new Attribute(XACMLConstants.ResourceAttributeURI, null, null,
                new StringAttribute("GeoServer")));
    }

    protected void addOWSService(Set<Attribute> resources) {
        org.geoserver.ows.Request owsRequest = Dispatcher.REQUEST.get();
        if (owsRequest == null)
            return;
        resources.add(new Attribute(XACMLConstants.OWSRequestResourceURI, null, null,
                new StringAttribute(owsRequest.getRequest())));
        resources.add(new Attribute(XACMLConstants.OWSServiceResourceURI, null, null,
                new StringAttribute(owsRequest.getService())));
    }

    protected void addGeometry(Set<Attribute> resources, URI attributeURI, Geometry g,
            String srsName) {

        String gmlType = XACMLUtil.getGMLTypeFor(g);

        GeometryAttribute geomAttr = null;
        try {
            geomAttr = new GeometryAttribute(g, srsName, null, GMLVersion.Version3, gmlType);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        resources.add(new Attribute(attributeURI, null, null, geomAttr));
    }

    protected void addBbox(Set<Attribute> resources) {
        org.geoserver.ows.Request owsRequest = Dispatcher.REQUEST.get();
        if (owsRequest == null)
            return;

        Map kvp = owsRequest.getKvp();
        if (kvp == null)
            return;

        ReferencedEnvelope env = (ReferencedEnvelope) kvp.get("BBOX");
        if (env == null)
            return;

        String srsName = (String) kvp.get("SRS");
        Geometry geom = JTS.toGeometry((Envelope) env);

        addGeometry(resources, XACMLConstants.BBoxResourceURI, geom, srsName);

    }

    abstract public RequestCtx createRequestCtx();

}
