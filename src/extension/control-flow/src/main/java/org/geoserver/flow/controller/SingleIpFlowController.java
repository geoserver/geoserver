/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import org.geoserver.ows.Request;

/**
 * A flow controller that throttles concurrent requests made from the same ip (single ip, specified
 * in configuration file)
 * 
 * @author Juan Marin, OpenGeo
 */
public class SingleIpFlowController extends SingleQueueFlowController {

    private final String ip;

    public SingleIpFlowController(final int queueSize, final String ip) {
        super(queueSize);
        this.ip = ip;
    }

    @Override
    boolean matchesRequest(Request request) {
        final String incomingIp = IpFlowController.getRemoteAddr(request.getHttpRequest());
        boolean matches = ip.equals(incomingIp);
        return matches;
    }

    public String getIp() {
        return ip;
    }

}
