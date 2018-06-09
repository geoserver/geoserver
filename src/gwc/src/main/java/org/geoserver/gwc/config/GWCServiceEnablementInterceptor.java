/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geowebcache.service.Service;

/**
 * Intercepts calls to {@link Service org.geowebcache.service.Service} and checks whether the
 * service is enabled, throwing a 404 http error code exception if it's not, and proceeding normaly
 * if the service is enabled.
 *
 * @author Gabriel Roldan
 */
public class GWCServiceEnablementInterceptor implements MethodInterceptor {

    private GWC gwcFacade;
    private final GeoServer geoServer;

    /**
     * @param gwc provides access to the {@link GWCConfig configuration} to check whether a service
     *     is {@link GWC#isServiceEnabled(Service) enabled}.
     */
    public GWCServiceEnablementInterceptor(final GWC gwc, GeoServer geoServer) {
        this.gwcFacade = gwc;
        this.geoServer = geoServer;
    }

    /**
     * Intercepts the {@code getConveyor} and {@code handleRequest} calls to a {@link Service}
     * instance and checks whether the service is enabled.
     *
     * @see
     *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        final String methodName = invocation.getMethod().getName();
        if ("getConveyor".equals(methodName) || "handleRequest".equals(methodName)) {
            final org.geowebcache.service.Service service = (Service) invocation.getThis();
            boolean serviceEnabled;
            if (service.getPathName().equals("wmts")) {
                serviceEnabled = geoServer.getService(WMTSInfo.class).isEnabled();
            } else {
                serviceEnabled = gwcFacade.isServiceEnabled(service);
            }
            if (!serviceEnabled) {
                throw new org.geowebcache.service.HttpErrorCodeException(
                        400, "Service is disabled");
            }
        }
        return invocation.proceed();
    }
}
