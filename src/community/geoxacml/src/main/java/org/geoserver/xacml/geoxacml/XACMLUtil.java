/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.geoxacml;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;
import org.geotools.xacml.geoxacml.attr.GML3Support;

import com.sun.xacml.Indenter;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Some utility methods
 * 
 * @author Mueller Christian
 * 
 */
public class XACMLUtil {

    static public String asXMLString(RequestCtx ctx) {
        OutputStream out = new ByteArrayOutputStream();
        ctx.encode(out, new Indenter(2));
        return out.toString();
    }

    static public String asXMLString(ResponseCtx ctx) {
        OutputStream out = new ByteArrayOutputStream();
        ctx.encode(out, new Indenter(2));
        return out.toString();
    }

    /**
     * One Permit is enough, but all responses must be checked if there was a processing error
     * 
     * @param responses
     *            from role requests
     * @return XACML decision
     */
    public static int getDecisionFromRoleResponses(List<ResponseCtx> responses) {
        boolean hasPermit = false;

        for (ResponseCtx responseCtx : responses) {
            int decision = getDecisionFromResponseContext(responseCtx);
            if (decision == Result.DECISION_INDETERMINATE) // Error
                return decision;
            if (decision == Result.DECISION_PERMIT)
                hasPermit = true;
        }
        return hasPermit ? Result.DECISION_PERMIT : Result.DECISION_DENY;

    }

    public static int getDecisionFromResponseContext(ResponseCtx responseCtx) {
        Set<Result> results = responseCtx.getResults();
        // Set<Obligation> permitObligations = new HashSet<Obligation>();
        // Set<Obligation> denyObligations = new HashSet<Obligation>();
        Set<String> resources = new HashSet<String>();

        boolean hasPermit = false, hasDeny = false;
        for (Result result : results) {
            int decision = result.getDecision();
            resources.add(result.getResource());
            if (decision == Result.DECISION_INDETERMINATE)
                return Result.DECISION_INDETERMINATE; // error
            if (decision == Result.DECISION_DENY) {
                hasDeny = true;
                // denyObligations.addAll(result.getObligations());
            }
            if (decision == Result.DECISION_PERMIT) {
                hasPermit = true;
                // permitObligations.addAll(result.getObligations());
            }
        }
        if (hasDeny && hasPermit) {
            logDecision(Result.DECISION_INDETERMINATE, resources);
            return Result.DECISION_INDETERMINATE;
        }
        if (!hasDeny && !hasPermit) {
            logDecision(Result.DECISION_NOT_APPLICABLE, resources);
            return Result.DECISION_NOT_APPLICABLE;
        }
        if (hasDeny) {
            logDecision(Result.DECISION_DENY, resources);
            return Result.DECISION_DENY;
        }

        return Result.DECISION_PERMIT;
    }

    private static void logDecision(int decision, Set<String> resources) {
        StringBuffer buff = new StringBuffer("User: ");
        buff.append(authenticationAsString());
        buff.append(" resource: ");
        for (String resource : resources) {
            buff.append(resource).append(",");
        }
        if (resources.size() > 1)
            buff.setLength(buff.length() - 1);
        buff.append(" ");
        buff.append(Result.DECISIONS[decision]);
        getXACMLLogger().info(buff.toString());
    }

    private static String authenticationAsString() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return "anonymous";
        String userName = auth.getCredentials() instanceof UserDetails ? ((UserDetails) auth
                .getPrincipal()).getUsername() : auth.getCredentials().toString();
        StringBuffer buff = new StringBuffer(userName);
        buff.append(" [ ");
        for (GrantedAuthority ga : auth.getAuthorities()) {
            buff.append(ga.getAuthority()).append(",");
        }
        if (auth.getAuthorities().length > 0)
            buff.setLength(buff.length() - 1);
        buff.append(" ] ");
        return buff.toString();
    }

    public static Logger getXACMLLogger() {
        return Logger.getLogger("XACML");
    }

    public static String getGMLTypeFor(Geometry g) {
        String gmlType = null;
        if (g instanceof Point)
            gmlType = GML3Support.GML_POINT;
        if (g instanceof LineString)
            gmlType = GML3Support.GML_LINESTRING;
        if (g instanceof Polygon)
            gmlType = GML3Support.GML_POLYGON;
        if (g instanceof MultiPoint)
            gmlType = GML3Support.GML_MULTIPOINT;
        if (g instanceof MultiLineString)
            gmlType = GML3Support.GML_MULTICURVE;
        if (g instanceof MultiPolygon)
            gmlType = GML3Support.GML_MULTISURFACE;

        if (gmlType == null) {
            throw new RuntimeException("No GML type for " + g.getClass().getName());
        }
        return gmlType;
    }

}
