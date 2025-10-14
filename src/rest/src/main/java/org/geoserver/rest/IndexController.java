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
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.template.TemplateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The IndexController lists the paths available for the Spring MVC RequestMappingHandler Specifically, it
 * auto-generates an index page containing all non-templated paths relative to the router root.
 */
@RestController
@RequestMapping(
        path = RestBaseController.ROOT_PATH,
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE})
public class IndexController extends RestBaseController {

    private final BeansWrapper wrapper = TemplateUtils.getSafeWrapper(null, FULL_ACCESS, null);

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @GetMapping(
            value = {"", "index"},
            produces = {MediaType.TEXT_HTML_VALUE})
    public RestWrapper get() {

        SimpleHash model = new SimpleHash(this.wrapper);
        model.put("links", getLinks());
        model.put("page", RequestInfo.get());

        return wrapObject(model, SimpleHash.class);
    }

    protected Set<String> getLinks() {

        // Ensure sorted, unique keys
        Set<String> s = new TreeSet<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> item : handlerMethods.entrySet()) {
            RequestMappingInfo mapping = item.getKey();

            // Only list "get" endpoints
            if (mapping.getMethodsCondition().getMethods().contains(RequestMethod.GET)) {
                PatternsRequestCondition patternsRequestCondition = mapping.getPatternsCondition();
                if (patternsRequestCondition != null && patternsRequestCondition.getPatterns() != null) {
                    for (String pattern : patternsRequestCondition.getPatterns()) {
                        if (!pattern.contains("{")) {
                            String path = pattern;
                            // exclude other rest apis, like gwc/rest
                            final int rootSize = RestBaseController.ROOT_PATH.length() + 1;
                            if (path.startsWith(RestBaseController.ROOT_PATH) && path.length() > rootSize) {
                                // trim root path
                                path = path.substring(rootSize);

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

    @Override
    public String getTemplateName(Object o) {
        return "index";
    }
}
