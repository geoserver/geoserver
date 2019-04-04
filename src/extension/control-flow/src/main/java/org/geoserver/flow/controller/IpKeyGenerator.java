/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Request;

/**
 * Returns the IP address as the user key
 *
 * @author Andrea Aime - GeoSolutions
 */
public class IpKeyGenerator implements KeyGenerator {

    @Override
    public String getUserKey(Request request) {
        HttpServletRequest httpRequest = request.getHttpRequest();
        return IpFlowController.getRemoteAddr(httpRequest);
    }
}
