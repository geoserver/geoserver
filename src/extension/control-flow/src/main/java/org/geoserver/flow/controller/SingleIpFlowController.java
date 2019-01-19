/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

/**
 * A flow controller that throttles concurrent requests made from the same ip (single ip, specified
 * in configuration file)
 *
 * @author Juan Marin, OpenGeo
 */
public class SingleIpFlowController extends SingleQueueFlowController {

    public SingleIpFlowController(final int queueSize, final String ip) {
        // building a simpLe thread blocker as this queue is for a single IP, there is no priority
        // concept here
        super(new IpRequestMatcher(ip), queueSize, new SimpleThreadBlocker(queueSize));
    }
}
