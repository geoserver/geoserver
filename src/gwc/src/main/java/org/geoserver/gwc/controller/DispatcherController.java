/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.controller.GeoWebCacheDispatcherController;

public class DispatcherController extends GeoWebCacheDispatcherController {

    public static final ThreadLocal<String> BASE_URL = new InheritableThreadLocal<String>();

    @Override
    public void handleRestApiRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        BASE_URL.set(ResponseUtils.baseURL(request));
        super.handleRestApiRequest(request, response);
    }
}
