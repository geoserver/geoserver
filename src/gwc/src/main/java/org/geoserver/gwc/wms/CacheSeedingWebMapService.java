/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Method;
import java.util.Map;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;

/**
 * {@link WebMapService#getMap(GetMapRequest)} Spring's AOP method interceptor to seed a (meta)tile
 *
 * <p>{@link GeoServerTileLayer} issues a GetMap request that will be handled by this interceptor
 * instead of directly calling {@link WebMapService#getMap(GetMapRequest)} in order to respect the
 * normal flow of operations through the GeoServer {@link Dispatcher} and hence avoid overwhelming
 * the server with too many requests. That is, adheres to the expectations of the control-flow and
 * monitoring modules by not bypassing the dispatcher.
 *
 * @author Gabriel Roldan
 */
public class CacheSeedingWebMapService implements MethodInterceptor {

    public CacheSeedingWebMapService() {}

    /**
     * Wraps {@link WebMapService#getMap(GetMapRequest)}, called by the {@link Dispatcher}
     *
     * @see WebMapService#getMap(GetMapRequest)
     * @see
     *     org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public WebMap invoke(MethodInvocation invocation) throws Throwable {

        final Method method = invocation.getMethod();
        checkArgument(method.getDeclaringClass().equals(WebMapService.class));
        checkArgument("getMap".equals(method.getName()));

        final Object[] arguments = invocation.getArguments();

        checkArgument(arguments.length == 1);
        checkArgument(arguments[0] instanceof GetMapRequest);

        final GetMapRequest request = (GetMapRequest) arguments[0];

        WebMap map = (WebMap) invocation.proceed();

        final Map<String, String> rawKvp = request.getRawKvp();
        boolean isSeedingRequest =
                rawKvp != null && rawKvp.containsKey(GeoServerTileLayer.GWC_SEED_INTERCEPT_TOKEN);
        if (isSeedingRequest) {
            GeoServerTileLayer.WEB_MAP.set(map);
            // returning null makes the Dispatcher ignore further processing the request
            return null;
        }

        return map;
    }
}
