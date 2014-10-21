/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.DataAccessManager;
import org.geoserver.xacml.geoxacml.GeoXACMLConfig;
import org.geoserver.xacml.geoxacml.XACMLConstants;
import org.geoserver.xacml.geoxacml.XACMLUtil;
import org.geoserver.xacml.role.XACMLRole;

import com.sun.xacml.Obligation;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

public class XACMLDataAccessManager implements DataAccessManager {

    private CatalogMode mode;

    private Object modeLock = new Object();

    private Logger Log;

    private static Map<String, CatalogMode> CatalogModeMap;
    static {
        CatalogModeMap = new HashMap<String, CatalogMode>(3);
        CatalogModeMap.put("HIDE", CatalogMode.HIDE);
        CatalogModeMap.put("CHALLENGE", CatalogMode.CHALLENGE);
        CatalogModeMap.put("MIXED", CatalogMode.MIXED);
    }

    public XACMLDataAccessManager() {
        if (Log == null)
            Log = Logger.getLogger(this.getClass().getName());

        GeoXACMLConfig.createDefaultRepositoryIfNotExisting();
    }

    public boolean canAccess(Authentication user, WorkspaceInfo workspace, AccessMode mode) {
        GeoXACMLConfig.getXACMLRoleAuthority().prepareRoles(user);
        List<RequestCtx> requestCtxts = buildWorkspaceRequestCtxListFromRoles(user, workspace, mode);
        if (requestCtxts.isEmpty())
            return false;

        List<ResponseCtx> responseCtxts = GeoXACMLConfig.getXACMLTransport()
                .evaluateRequestCtxList(requestCtxts);

        int xacmlDecision = XACMLUtil.getDecisionFromRoleResponses(responseCtxts);

        if (xacmlDecision == Result.DECISION_PERMIT)
            return true;
        return false;
    }

    public boolean canAccess(Authentication user, LayerInfo layer, AccessMode mode) {
        return canAccess(user, layer.getResource(), mode);
        // List<RequestCtx> requestCtxts = buildLayerInfoRequestCtxListFromRoles(user, layer, mode);
        // List<ResponseCtx> responseCtxts =
        // GeoXACMLConfig.getXACMLTransport().evaluateRequestCtxList(requestCtxts);
        //
        // int xacmlDecision = XACMLUtil.getDecisionFromRoleResponses(responseCtxts);
        //
        // if (xacmlDecision == Result.DECISION_PERMIT)
        // return true;
        // return false;
    }

    public boolean canAccess(Authentication user, ResourceInfo resource, AccessMode mode) {
        GeoXACMLConfig.getXACMLRoleAuthority().prepareRoles(user);
        List<RequestCtx> requestCtxts = buildResourceInfoRequestCtxListFromRoles(user, resource,
                mode);
        if (requestCtxts.isEmpty())
            return false;

        List<ResponseCtx> responseCtxts = GeoXACMLConfig.getXACMLTransport()
                .evaluateRequestCtxList(requestCtxts);

        int xacmlDecision = XACMLUtil.getDecisionFromRoleResponses(responseCtxts);

        if (xacmlDecision == Result.DECISION_PERMIT)
            return true;
        return false;
    }

    public CatalogMode getMode() {
        synchronized (modeLock) {
            if (mode != null)
                return mode;

            RequestCtx requestCtx = GeoXACMLConfig.getRequestCtxBuilderFactory()
                    .getCatalogRequestCtxBuilder().createRequestCtx();
            ResponseCtx responseCtx = GeoXACMLConfig.getXACMLTransport().evaluateRequestCtx(
                    requestCtx);

            Result result = responseCtx.getResults().iterator().next();
            if (result == null || result.getDecision() != Result.DECISION_PERMIT) {
                Log.severe("Geserver cannot access its catalog !!!");
                Log.severe(XACMLUtil.asXMLString(requestCtx));
                return useDefaultMode();
            }

            Obligation obligation = result.getObligations().iterator().next();
            if (obligation == null
                    || XACMLConstants.CatalogModeObligationId.equals(obligation.getId()
                            .toASCIIString()) == false) {
                Log.severe("No obligation with id: " + XACMLConstants.CatalogModeObligationId);
                Log.severe(XACMLUtil.asXMLString(requestCtx));
                return useDefaultMode();
            }

            Attribute catalogModeAssignment = obligation.getAssignments().iterator().next();
            if (catalogModeAssignment == null
                    || CatalogModeMap.containsKey(((StringAttribute) catalogModeAssignment
                            .getValue()).getValue()) == false) {
                Log.severe("No valid catalog mode ");
                Log.severe(XACMLUtil.asXMLString(requestCtx));
                return useDefaultMode();
            }

            String catalogModeKey = ((StringAttribute) catalogModeAssignment.getValue()).getValue();
            mode = CatalogModeMap.get(catalogModeKey);
            return mode;
        }

    }

    private CatalogMode useDefaultMode() {
        Log.info("Falling back to CatalogMode " + CatalogMode.HIDE);
        mode = CatalogMode.HIDE;
        return mode;
    }

    private List<RequestCtx> buildWorkspaceRequestCtxListFromRoles(Authentication auth,
            WorkspaceInfo workspaceInfo, AccessMode mode) {

        List<RequestCtx> resultList = new ArrayList<RequestCtx>();

        for (GrantedAuthority role : auth.getAuthorities()) {
            XACMLRole xacmlRole = (XACMLRole) role;
            if (xacmlRole.isEnabled() == false)
                continue;
            RequestCtx requestCtx = GeoXACMLConfig.getRequestCtxBuilderFactory()
                    .getWorkspaceRequestCtxBuilder(xacmlRole, workspaceInfo, mode)
                    .createRequestCtx();
            // XACMLUtil.getXACMLLogger().info(XACMLUtil.asXMLString(requestCtx));
            resultList.add(requestCtx);
        }

        return resultList;
    }

    private List<RequestCtx> buildResourceInfoRequestCtxListFromRoles(Authentication auth,
            ResourceInfo resourceInfo, AccessMode mode) {

        List<RequestCtx> resultList = new ArrayList<RequestCtx>();

        for (GrantedAuthority role : auth.getAuthorities()) {
            XACMLRole xacmlRole = (XACMLRole) role;
            if (xacmlRole.isEnabled() == false)
                continue;
            RequestCtx requestCtx = GeoXACMLConfig.getRequestCtxBuilderFactory()
                    .getResourceInfoRequestCtxBuilder(xacmlRole, resourceInfo, mode)
                    .createRequestCtx();
            // XACMLUtil.getXACMLLogger().info(XACMLUtil.asXMLString(requestCtx));
            resultList.add(requestCtx);
        }

        return resultList;
    }

}
