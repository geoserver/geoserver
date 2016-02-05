/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.Logging;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public final class Filter implements GeoServerFilter, ExtensionPriority {

    private static final Logger LOGGER = Logging.getLogger(Filter.class);

    private static List<Rule> rules;

    public Filter(ResourceStore dataDirectory) {
        final Resource resource = dataDirectory.get(RulesDao.getRulesPath());
        rules = RulesDao.getRules(resource.in());
        resource.addListener(new ResourceListener() {
            @Override
            public void changed(ResourceNotification notify) {
                rules = RulesDao.getRules(resource.in());
            }
        });
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (httpServletRequest.getRequestURI().contains("web/wicket")
                || httpServletRequest.getRequestURI().contains("geoserver/web")) {
            chain.doFilter(request, response);
            return;
        }
        UrlTransform urlTransform = new UrlTransform(httpServletRequest.getRequestURI(), httpServletRequest.getParameterMap());
        String originalRequest = urlTransform.toString();
        for (Rule rule : rules) {
            rule.apply(urlTransform);
        }
        if (urlTransform.haveChanged()) {
            Utils.info(LOGGER, "Request '%s' transformed to '%s'.", originalRequest, urlTransform.toString());
            chain.doFilter(new RequestWrapper(urlTransform, httpServletRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }
}
