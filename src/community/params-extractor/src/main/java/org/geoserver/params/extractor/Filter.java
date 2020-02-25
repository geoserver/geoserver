/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public final class Filter implements GeoServerFilter, ExtensionPriority {

    private static final Logger LOGGER = Logging.getLogger(Filter.class);

    // this becomes true if the filter is initialized from the web container
    // via web.xml, so that we know we should ignore the spring initialized filter
    static boolean USE_AS_SERVLET_FILTER = false;

    // marks the instance initialized via web.xml (if any) so that we can avoid
    // duplicate filter application by the spring instance
    private boolean servletInstance = false;

    private List<Rule> rules;

    public Filter() {
        // this is called if we initialize the filter in web.xml, so let's turn on the
        // flags for this scenario
        USE_AS_SERVLET_FILTER = true;
        servletInstance = true;
    }

    public Filter(GeoServerDataDirectory dataDirectory) {
        servletInstance = false;
        initRules(dataDirectory);
    }

    private void initRules(GeoServerDataDirectory dataDirectory) {
        if (dataDirectory != null) {
            Resource resource = dataDirectory.get(RulesDao.getRulesPath());
            rules = RulesDao.getRules(resource.in());
            resource.addListener(notify -> rules = RulesDao.getRules(resource.in()));
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    /**
     * This method is called only when the Filter is used as a standard web container Filter. When
     * this happens the related instance will be used instead of the one initialized by Spring.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        GeoServerDataDirectory dataDirectory =
                GeoServerExtensions.bean(GeoServerDataDirectory.class);

        initRules(dataDirectory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (isEnabled()) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            if (!httpServletRequest.getRequestURI().contains("web/wicket")
                    && !httpServletRequest.getRequestURI().contains("geoserver/web")) {
                UrlTransform urlTransform =
                        new UrlTransform(
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getParameterMap());
                String originalRequest = urlTransform.toString();
                rules.forEach(rule -> rule.apply(urlTransform));
                if (urlTransform.haveChanged()) {
                    Utils.info(
                            LOGGER,
                            "Request '%s' transformed to '%s'.",
                            originalRequest,
                            urlTransform.toString());
                    request = new RequestWrapper(urlTransform, httpServletRequest);
                }
            }
        }
        chain.doFilter(request, response);
    }

    boolean isEnabled() {
        return !USE_AS_SERVLET_FILTER || servletInstance;
    }

    @Override
    public void destroy() {}
}
