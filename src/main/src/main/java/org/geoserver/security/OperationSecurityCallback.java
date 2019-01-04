/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.List;
import java.util.Set;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * This callback performs security access checks at the service/method level based on rules provided
 * by the {@link ServiceAccessRuleDAO}
 */
public class OperationSecurityCallback implements DispatcherCallback {

    ServiceAccessRuleDAO dao;

    public OperationSecurityCallback(ServiceAccessRuleDAO dao) {
        this.dao = dao;
    }

    public void finished(Request request) {
        // nothing to do
    }

    public Request init(Request request) {
        return request;
    }

    public Operation operationDispatched(Request request, Operation operation) {
        String service = request.getService();
        String method = request.getRequest();

        // find the best matching rule. Rules are sorted by specificity so any rule matching
        // last will be more specific than the ones matching earlier (e.g., wms.GetMap is moer
        // specific than just wms.* which is more specific than *.*)
        List<ServiceAccessRule> rules = dao.getRules();
        ServiceAccessRule bestMatch = null;
        for (ServiceAccessRule rule : rules) {
            if (rule.getService().equals(ServiceAccessRule.ANY)
                    || rule.getService().equalsIgnoreCase(service)) {
                if (rule.getMethod().equals(ServiceAccessRule.ANY)
                        || rule.getMethod().equalsIgnoreCase(method)) {
                    bestMatch = rule;
                }
            }
        }

        // if there is a matching rule apply it
        if (bestMatch != null) {
            Set<String> allowedRoles = bestMatch.getRoles();
            // if the rule is not the kind that allows everybody in check if the current
            // user is authenticated and has one of the required roles
            if (!allowedRoles.contains(ServiceAccessRule.ANY) && !allowedRoles.isEmpty()) {
                Authentication user = SecurityContextHolder.getContext().getAuthentication();

                if (user == null || user.getAuthorities().size() == 0)
                    throw new InsufficientAuthenticationException(
                            "Cannot access " + service + "." + method + " as anonymous");

                boolean roleFound = false;
                for (GrantedAuthority role : user.getAuthorities()) {
                    if (allowedRoles.contains(role.getAuthority())) {
                        roleFound = true;
                        break;
                    }
                }

                if (!roleFound) {
                    throw new AccessDeniedException(
                            "Cannot access "
                                    + service
                                    + "."
                                    + method
                                    + " with the current privileges");
                }
            }
        }

        return operation;
    }

    public Object operationExecuted(Request request, Operation operation, Object result) {
        // nothing to do
        return result;
    }

    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return response;
    }

    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }
}
