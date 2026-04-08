/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.Service;
import org.geotools.util.logging.Logging;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Configures the dispatcher to be cite compliant based on the specified service configuration.
 *
 * <p>TODO: Cite compliance should be a server wide thing. This should be addressed when we ( if we ) refactor server
 * configuration. When that happens this class can be retired.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CiteComplianceHack implements HandlerInterceptor {

    private static final Logger LOGGER = Logging.getLogger(CiteComplianceHack.class);
    GeoServer gs;
    Class<? extends ServiceInfo> serviceClass;

    public CiteComplianceHack(GeoServer gs, Class<? extends ServiceInfo> serviceClass) {
        this.gs = gs;
        this.serviceClass = serviceClass;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof Dispatcher dispatcher) {
            String service = findService(dispatcher, request, response);
            if (service != null
                    && (service.equalsIgnoreCase(getInfo().getId())
                            || service.equalsIgnoreCase(getInfo().getName()))) {
                dispatcher.setCiteCompliant(getInfo().isCiteCompliant());
            }
        }

        return true;
    }

    private String findService(Dispatcher dispatcher, HttpServletRequest request, HttpServletResponse response) {
        // create a new request instance
        Request req = new Request();

        // set request / response
        req.setHttpRequest(request);
        req.setHttpResponse(response);
        Dispatcher.initRequestContext(req);

        // find the service
        try {
            return dispatcher.getServiceFromRequest(req);
        } catch (Exception ex1) {
            LOGGER.log(Level.FINE, "Exception while looking for the 'Service' from the request", ex1);
            // load from the context
            try {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(req.path);
                if (builder != null
                        && builder.build() != null
                        && builder.build().getPath() != null) {
                    Service serviceDescriptor = dispatcher.findService(
                            Objects.requireNonNull(builder.build().getPath()), req.getVersion(), req.getNamespace());
                    if (serviceDescriptor != null) {
                        return serviceDescriptor.getId();
                    }
                }
            } catch (Exception ex2) {
                LOGGER.log(Level.FINE, "Exception while decoding OWS URL " + request.getServletPath(), ex2);
            }
            return null;
        }
    }

    @Override
    public void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
            throws Exception {
        // do nothing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // do nothing
    }

    ServiceInfo getInfo() {
        return gs.getService(serviceClass);
    }
}
