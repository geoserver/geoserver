/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.geoxacml;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.easymock.classextension.EasyMock;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.xacml.role.XACMLRole;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.xacml.geoxacml.attr.GeometryAttribute;

import com.sun.xacml.Obligation;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Subject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Testing the following situations
 * 
 * Layer topp:asia is locked down
 * 
 * Layer topp:statis is accessable from ROLE_ANONYMOUS for wms GetMap and GetFeatureInfo requests An
 * obligation containing a geometry is passed back to inform the PEP about the restriction
 * 
 * Layer toop:europe is from ROLE_ANONYMOUS for wms GetMap and GetFeatureInfo requests The Bounding
 * Box will be evaluateted against a rect (10,10,20,20) If there is no intersection, the decision is
 * Deny If there is an intersection, the decision is Permit and rect (10,10,20,20) is passed back in
 * an obligation.
 * 
 * 
 * 
 * @author Christian Mueller
 * 
 */
public class GeoXACMLGeometryTest extends GeoServerTestSupport {

    NamespaceInfo nameSpace;

    ResourceInfo states;

    ResourceInfo europe;

    ResourceInfo asia;

    XACMLRole role;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        nameSpace = new NamespaceInfoImpl();
        nameSpace.setURI("topp");

        states = new FeatureTypeInfoImpl(null);
        states.setNamespace(nameSpace);
        states.setName("states");

        europe = new FeatureTypeInfoImpl(null);
        europe.setNamespace(nameSpace);
        europe.setName("europe");

        asia = new FeatureTypeInfoImpl(null);
        asia.setNamespace(nameSpace);
        asia.setName("asia");

        GeoXACMLConfig.setPolicyRepsoitoryBaseDir("src/test/resources/publicReadGeoRestricted/");
        GeoXACMLConfig.reset();
        role = new XACMLRole("ROLE_ANONYMOUS");
    }

    public void testLayerAccessStates() {
        SecurityContextHolder.getContext().setAuthentication(null);
        ReferencedEnvelope env = new ReferencedEnvelope(10, 20, 40, 60, null);
        setDispatcherRequest("GetMap", env, null);

        RequestCtx request = GeoXACMLConfig.getRequestCtxBuilderFactory()
                .getResourceInfoRequestCtxBuilder(role, states, AccessMode.READ).createRequestCtx();
        // dumpRequestCtx(request);
        ResponseCtx response = GeoXACMLConfig.getXACMLTransport().evaluateRequestCtx(request);
        assertTrue(response.getResults().iterator().next().getDecision() == Result.DECISION_PERMIT);
        checkForObligation(response);
    }

    public void testLayerAccessAsia() {
        SecurityContextHolder.getContext().setAuthentication(null);
        ReferencedEnvelope env = new ReferencedEnvelope(10, 20, 40, 60, null);
        setDispatcherRequest("GetFeatureInfo", env, "EPSG:4326");

        RequestCtx request = GeoXACMLConfig.getRequestCtxBuilderFactory()
                .getResourceInfoRequestCtxBuilder(role, asia, AccessMode.READ).createRequestCtx();
        // dumpRequestCtx(request);
        ResponseCtx response = GeoXACMLConfig.getXACMLTransport().evaluateRequestCtx(request);
        assertTrue(response.getResults().iterator().next().getDecision() == Result.DECISION_DENY);
    }

    public void testLayerAccessEuropePermit() {
        SecurityContextHolder.getContext().setAuthentication(null);
        ReferencedEnvelope env = new ReferencedEnvelope(12, 17, 12, 17, null);
        setDispatcherRequest("GetFeatureInfo", env, "EPSG:4326");

        RequestCtx request = GeoXACMLConfig.getRequestCtxBuilderFactory()
                .getResourceInfoRequestCtxBuilder(role, europe, AccessMode.READ).createRequestCtx();
        // dumpRequestCtx(request);
        ResponseCtx response = GeoXACMLConfig.getXACMLTransport().evaluateRequestCtx(request);
        assertTrue(response.getResults().iterator().next().getDecision() == Result.DECISION_PERMIT);
        checkForObligation(response);
    }

    public void testLayerAccessEuropeDeny() {
        SecurityContextHolder.getContext().setAuthentication(null);
        ReferencedEnvelope env = new ReferencedEnvelope(6, 8, 6, 8, null);
        setDispatcherRequest("GetFeatureInfo", env, "EPSG:4326");

        RequestCtx request = GeoXACMLConfig.getRequestCtxBuilderFactory()
                .getResourceInfoRequestCtxBuilder(role, europe, AccessMode.READ).createRequestCtx();
        // dumpRequestCtx(request);
        ResponseCtx response = GeoXACMLConfig.getXACMLTransport().evaluateRequestCtx(request);
        assertTrue(response.getResults().iterator().next().getDecision() == Result.DECISION_DENY);
    }

    private void dumpRequestCtx(RequestCtx request) {
        Logger.getAnonymousLogger().info(XACMLUtil.asXMLString(request));
    }

    private void checkForObligation(ResponseCtx response) {
        Result result = response.getResults().iterator().next();
        assertNotNull(result);
        Obligation obligation = result.getObligations().iterator().next();
        assertNotNull(obligation);
        Attribute assignment = obligation.getAssignments().iterator().next();
        GeometryAttribute geomAttr = (GeometryAttribute) assignment.getValue();
        assertNotNull(geomAttr.getGeometry());

    }

    private void setDispatcherRequest(String requestName, ReferencedEnvelope env, String srsName) {

        Map<String, Object> kvp = new HashMap<String, Object>();
        if (env != null)
            kvp.put("BBOX", env);
        if (srsName != null)
            kvp.put("SRS", srsName);

        Request owsRequest = EasyMock.createMock(Request.class);
        EasyMock.expect(owsRequest.getService()).andReturn("wms").anyTimes();
        EasyMock.expect(owsRequest.getRequest()).andReturn(requestName).anyTimes();
        EasyMock.expect(owsRequest.getKvp()).andReturn(kvp).anyTimes();
        EasyMock.replay(owsRequest);
        Dispatcher.REQUEST.set(owsRequest);
    }

    public void testRoleAttributes() {

        UserDetailsImpl readerDetails = new UserDetailsImpl("reader", "pwreader",
                new GrantedAuthority[] { new GrantedAuthorityImpl("READER") });
        readerDetails.setPersNr(4711);
        GeometryFactory fac = new GeometryFactory();
        LinearRing r = fac.createLinearRing(new Coordinate[] { new Coordinate(11, 11),
                new Coordinate(14, 11), new Coordinate(14, 14), new Coordinate(11, 14),
                new Coordinate(11, 11), });
        Polygon poly = fac.createPolygon(r, new LinearRing[] {});
        poly.setUserData("EPSG:4326");
        readerDetails.setGeometryRestriction(poly);

        GeoXACMLConfig.getXACMLRoleAuthority().transformUserDetails(readerDetails);

        Authentication reader = new TestingAuthenticationToken(readerDetails, "pwreader",
                readerDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(reader);
        // ///////
        GeoXACMLConfig.getXACMLRoleAuthority().prepareRoles(reader);

        // //////
        XACMLRole readerRole = (XACMLRole) reader.getAuthorities()[0];

        RequestCtx request = GeoXACMLConfig.getRequestCtxBuilderFactory()
                .getResourceInfoRequestCtxBuilder(readerRole, europe, AccessMode.READ)
                .createRequestCtx();

        // System.out.println(XACMLUtil.asXMLString(request));

        Subject subject = request.getSubjects().iterator().next();
        for (Attribute attr : subject.getAttributes()) {
            if (attr.getId().toString().equals(XACMLConstants.RoleParamPrefix + "persNr"))
                assertTrue(((IntegerAttribute) attr.getValue()).getValue() == 4711);
            if (attr.getId().toString().equals(
                    XACMLConstants.RoleParamPrefix + "geometryRestriction"))
                assertTrue(attr.getValue() instanceof GeometryAttribute);
            if (attr.getId().toString().equals(XACMLConstants.RoleParamPrefix + "persNr2"))
                assertTrue(((IntegerAttribute) attr.getValue()).getValue() == 4712);
            if (attr.getId().toString().equals(
                    XACMLConstants.RoleParamPrefix + "geometryRestriction2"))
                assertTrue(attr.getValue() instanceof GeometryAttribute);

        }

    }
}
