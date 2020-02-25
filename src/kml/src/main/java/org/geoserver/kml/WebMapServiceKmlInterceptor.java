/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;

public class WebMapServiceKmlInterceptor implements MethodInterceptor {

    private WMS wms;
    private WebMapService webMapService;

    public WebMapServiceKmlInterceptor(WMS wms, WebMapService webMapService) {
        this.wms = wms;
        this.webMapService = webMapService;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("kml")) {
            try {
                GetMapRequest getMap = (GetMapRequest) invocation.getArguments()[0];
                return KMLReflector.doWms(getMap, webMapService, wms);
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw e;
                } else {
                    throw new ServiceException(e);
                }
            }
        } else {
            return invocation.proceed();
        }
    }
}
