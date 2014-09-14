/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.xacml.spring.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.intercept.web.FilterInvocation;
import org.springframework.security.vote.AccessDecisionVoter;
import org.geoserver.xacml.geoxacml.GeoXACMLConfig;
import org.geoserver.xacml.geoxacml.XACMLUtil;
import org.geoserver.xacml.role.XACMLRole;

import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;

/**
 * Spring Security Decision Voter using XACML policies
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLFilterDecisionVoter implements AccessDecisionVoter {

    public boolean supports(ConfigAttribute attr) {
        return true;
    }

    public boolean supports(Class aClass) {
        return true;
    }

    public int vote(Authentication auth, Object request, ConfigAttributeDefinition arg2) {
        
        HttpServletRequest httpRequest = ((FilterInvocation) request).getHttpRequest();
        String urlPath = httpRequest.getServletPath().toLowerCase();
        // String urlPath = ((FilterInvocation) request).getRequestUrl().toLowerCase();
        String method = httpRequest.getMethod();
        Map<String, Object> httpParams = httpRequest.getParameterMap();
        
        String remoteIP = httpRequest.getRemoteAddr();
        String remoteHost = httpRequest.getRemoteHost();
        

        List<RequestCtx> requestCtxts = buildRequestCtxListFromRoles(auth, urlPath, method,
                httpParams, remoteIP,remoteHost);
        if (requestCtxts.isEmpty())
            return XACMLDecisionMapper.Exact.getSpringSecurityDecisionFor(Result.DECISION_DENY);

        List<ResponseCtx> responseCtxts = GeoXACMLConfig.getXACMLTransport()
                .evaluateRequestCtxList(requestCtxts);

        int xacmlDecision = XACMLUtil.getDecisionFromRoleResponses(responseCtxts);
        return XACMLDecisionMapper.Exact.getSpringSecurityDecisionFor(xacmlDecision);

    }

    private List<RequestCtx> buildRequestCtxListFromRoles(Authentication auth, String urlPath,
            String method, Map<String, Object> httpParams,String remoteIP,String remoteHost) {

        GeoXACMLConfig.getXACMLRoleAuthority().prepareRoles(auth);

        List<RequestCtx> resultList = new ArrayList<RequestCtx>();

        for (GrantedAuthority role : auth.getAuthorities()) {
            XACMLRole xacmlRole = (XACMLRole) role;
            if (xacmlRole.isEnabled() == false)
                continue;
            RequestCtx requestCtx = GeoXACMLConfig.getRequestCtxBuilderFactory()
                    .getURLMatchRequestCtxBuilder(xacmlRole, urlPath, method, httpParams,remoteIP,remoteHost)
                    .createRequestCtx();
            // XACMLUtil.getXACMLLogger().info(XACMLUtil.asXMLString(requestCtx));
            resultList.add(requestCtx);
        }

        return resultList;
    }

}
