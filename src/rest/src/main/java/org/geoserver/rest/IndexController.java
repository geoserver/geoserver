/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.geoserver.template.GeoServerMemberAccessPolicy.FULL_ACCESS;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.SimpleHash;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.geoserver.template.TemplateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The IndexController generates the main entry point for the GeoServer REST API.
 *
 * <p>This controller serves as a directory or index page for the REST API, automatically discovering and listing all
 * available REST endpoints that have GET methods. It filters the displayed endpoints based on the user's access rights
 * - administrators see all endpoints, while workspace administrators only see endpoints they have access to.
 *
 * <p>The controller responds to requests to the REST API root (/rest/) and produces representations in JSON, XML, or
 * HTML based on the client's Accept header.
 *
 * <p>Specifically, it auto-generates an index page containing all non-templated paths relative to the router root.
 */
@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE})
public class IndexController extends RestBaseController {

    /**
     * FreeMarker beans wrapper used for template model creation with full access to methods and properties. This allows
     * the template to access all properties of the wrapped objects.
     */
    private final BeansWrapper wrapper = TemplateUtils.getSafeWrapper(null, FULL_ACCESS, null);

    /**
     * Spring's handler mapping that contains information about all registered request mappings. Used to discover
     * available REST endpoints.
     */
    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Service that determines whether a user with workspace administrator privileges can access specific REST
     * endpoints.
     */
    @Autowired
    private WorkspaceAdminAuthorizer wsAdminAuthorizer;

    /**
     * Handles GET requests to the REST API root and builds a list of available endpoints. This method serves the main
     * index page of the REST API.
     *
     * <p>When accessed by administrators, all available endpoints are shown. When accessed by workspace administrators,
     * only endpoints they have access to are shown.
     *
     * @return a RestWrapper containing the model with the list of links for the template to render
     */
    @GetMapping(
            value = {"", "index"},
            produces = {MediaType.TEXT_HTML_VALUE})
    public RestWrapper get() {
        // Create the model for the template
        SimpleHash model = new SimpleHash(this.wrapper);

        // Get all available REST endpoints
        Set<String> links = getLinks();

        // Filter links for workspace administrators
        if (!isAdmin()) {
            links = filterLinksForWorkspaceAdmin(links);
        }

        // Add the links and page info to the model
        model.put("links", links);
        model.put("page", RequestInfo.get());

        return wrapObject(model, SimpleHash.class);
    }

    /**
     * Filters the given set of REST endpoint links to only include those that a workspace administrator has access to.
     *
     * <p>This method checks each link against the WorkspaceAdminAuthorizer to determine if the current user can access
     * it.
     *
     * @param links the complete set of endpoint links
     * @return a filtered set containing only the links the workspace admin can access
     */
    private Set<String> filterLinksForWorkspaceAdmin(Set<String> links) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return links.stream()
                .filter(uri -> wsAdminAuthorizer.canAccess(authentication, "/rest/" + uri, HttpMethod.GET))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Discovers all available REST API endpoints by introspecting Spring's request mappings.
     *
     * <p>This method filters the endpoints to only include:
     *
     * <ul>
     *   <li>GET methods (read-only operations)
     *   <li>Non-templated paths (no path variables like {id})
     *   <li>Paths under the REST root
     * </ul>
     *
     * @return a sorted set of REST endpoint paths relative to the REST root
     */
    protected Set<String> getLinks() {
        // Ensure sorted, unique keys
        Set<String> s = new TreeSet<>();

        // Get all registered handler methods from Spring MVC
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> item : handlerMethods.entrySet()) {
            RequestMappingInfo mapping = item.getKey();

            // Only list "get" endpoints
            if (mapping.getMethodsCondition().getMethods().contains(RequestMethod.GET)) {
                PatternsRequestCondition patternsRequestCondition = mapping.getPatternsCondition();
                if (patternsRequestCondition != null && patternsRequestCondition.getPatterns() != null) {
                    for (String pattern : patternsRequestCondition.getPatterns()) {
                        // Skip templated paths (those with path variables)
                        if (!pattern.contains("{")) {
                            String path = pattern;
                            // exclude other rest apis, like gwc/rest
                            final int rootSize = RestBaseController.ROOT_PATH.length() + 1;
                            if (path.startsWith(RestBaseController.ROOT_PATH) && path.length() > rootSize) {
                                // trim root path
                                path = path.substring(rootSize);

                                // Remove wildcard endings
                                if (path.endsWith("/**")) {
                                    path = path.substring(0, path.length() - 3);
                                }
                                if (!path.isEmpty()) {
                                    s.add(path);
                                }
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    /**
     * Returns the template name to use for rendering the index page.
     *
     * @param o the object being wrapped
     * @return the name of the FreeMarker template to use
     */
    @Override
    public String getTemplateName(Object o) {
        return "index";
    }

    /**
     * Checks if the current user has administrative privileges.
     *
     * @return true if the current user is a GeoServer administrator, false otherwise
     */
    private boolean isAdmin() {
        GeoServerSecurityManager manager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        return manager.checkAuthenticationForAdminRole();
    }
}
