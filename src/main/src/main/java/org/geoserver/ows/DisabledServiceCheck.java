/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.DisabledServiceResourceFilter;
import org.geotools.util.Version;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;

/**
 * Intercepts requests to all OWS services ensuring that the service is enabled.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public class DisabledServiceCheck implements DispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(DisabledServiceCheck.class);
    /** GeoServer configuration */
    private GeoServer geoServer;

    public DisabledServiceCheck(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    /**
     * Look up ServiceInfo if available.
     *
     * @return ServiceInfo, or {@code null} if not available
     * @throws Exception
     */
    public static ServiceInfo lookupServiceInfo(Service service) throws Exception {
        // first get serviceInfo object from service
        Object s = service.getService();

        // get the getServiceInfo() method
        Method m = null;

        // if this object is actually proxied, we need to a big more work
        if (s instanceof Proxy) {
            Class<?>[] interfaces = s.getClass().getInterfaces();
            for (int i = 0; m == null && i < interfaces.length; i++) {
                m = OwsUtils.getter(interfaces[i], "serviceInfo", ServiceInfo.class);
            }
        } else {
            m = OwsUtils.getter(s.getClass(), "serviceInfo", ServiceInfo.class);
        }
        if (m != null) {
            return (ServiceInfo) m.invoke(s);
        }
        return null;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) {
        try {
            ServiceInfo cachedInfo = lookupServiceInfo(service);
            ServiceInfo info = null;
            if (cachedInfo != null) {
                GeoServer geoServer = GeoServerExtensions.bean(GeoServer.class);
                if (geoServer != null && cachedInfo.getId() != null) {
                    info = geoServer.getService(cachedInfo.getId(), ServiceInfo.class);
                }
                if (info == null) {
                    info = cachedInfo;
                }
            }

            if (info == null) {
                // log a warning, we could not perform an important check
                LOGGER.warning(
                        "Could not get a ServiceInfo for service "
                                + service.getId()
                                + " even if the service implements ServiceInfo, thus could not check if the service is enabled");
            } else {
                // check if the service is enabled
                if (!info.isEnabled()) {
                    throw new ServiceException(
                            "Service " + info.getName() + " is disabled", ServiceException.SERVICE_UNAVAILABLE);
                }

                // check if the requested version is disabled
                String requestedVersion = request.getVersion();
                if (requestedVersion != null) {
                    List<Version> disabledVersions = info.getDisabledVersions();
                    if (disabledVersions != null && !disabledVersions.isEmpty()) {
                        Version reqVersion = new Version(requestedVersion);
                        if (disabledVersions.contains(reqVersion)) {
                            throw new ServiceException(
                                    "Service " + info.getName() + " version " + requestedVersion + " is disabled",
                                    ServiceException.SERVICE_UNAVAILABLE);
                        }
                    }
                }

                // check if service is disabled for layer
                String context = context(request);
                if (context != null && context.contains("/")) {
                    String layerName = context.replace("/", ":");
                    LayerInfo layerInfo = getLayerByName(layerName);
                    if (layerInfo != null) {
                        List<String> disabledServices =
                                DisabledServiceResourceFilter.disabledServices(layerInfo.getResource());
                        boolean disabled = disabledServices.stream()
                                .anyMatch(serviceType -> Strings.CI.equals(service.getId(), serviceType));
                        if (disabled) {
                            throw new ServiceException(
                                    "Service " + info.getName() + " is disabled for layer " + layerName,
                                    ServiceException.SERVICE_UNAVAILABLE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO: log this
            if (e instanceof ServiceException exception) {
                throw exception;
            }
            throw new ServiceException(e);
        }
        return service;
    }

    /**
     * Extracts the context from the request, used to check for layer reference.
     *
     * @param request The request.
     * @return The context.
     */
    String context(Request request) {
        String context = request.getContext();
        if (context != null) {
            if (context.contains("gwc/service")) {
                // Account for context provided to GwcServiceDispatcherCallback
                context = context.substring(0, context.indexOf("gwc/service"));
            }
            // remove leading and trailing slashes
            if (context.startsWith("/")) {
                context = context.substring(1);
            }
            if (context.endsWith("/")) {
                context = context.substring(0, context.length() - 1);
            }
            int idx1 = context.indexOf("/");
            if (idx1 != -1) {
                int idx2 = context.indexOf("/", idx1 + 1);
                if (idx2 != -1) {
                    return context.substring(0, idx2);
                }
            }
        }
        return context;
    }

    LayerInfo getLayerByName(String layerName) {
        // We need access to actual catalog, not filtered for current service
        Catalog catalog = geoServer.getCatalog();
        while (catalog instanceof Wrapper w && w.isWrapperFor(Catalog.class)) {
            Catalog unwrapped = ((Wrapper) catalog).unwrap(Catalog.class);
            if (unwrapped == catalog || unwrapped == null) {
                break;
            }
            catalog = unwrapped;
        }
        return catalog.getLayerByName(layerName);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
        return response;
    }

    @Override
    public void finished(Request request) {}
}
