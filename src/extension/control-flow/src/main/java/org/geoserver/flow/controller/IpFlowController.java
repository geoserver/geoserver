/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * A flow controller that throttles concurrent requests made from the same ip (any ip)
 * 
 * @author Juan Marin, OpenGeo
 */

public class IpFlowController extends QueueController {

    /**
     * A flow controller that throttles concurrent requests made from the same ip (any ip)
     * 
     * @author Juan Marin, OpenGeo
     */

    static final Logger LOGGER = Logging.getLogger(IpFlowController.class);

    public IpFlowController(int queueSize) {
        this.queueSize = queueSize;
    }

    protected List<String> ipAddresses = new ArrayList<String>();

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        // check if this client already made other connections
        String incomingIp = "";
        String ip = getRemoteAddr(request.getHttpRequest());
        if (ipAddresses.size() > 0) {
            for (String ipAddress : ipAddresses) {
                if (ipAddress.equals(ip)) {
                    incomingIp = ipAddress;
                    break;
                }
            }
        }

        if (incomingIp.equals("")) {
            incomingIp = ip;
        }

        // see if we have that queue already
        TimedBlockingQueue queue = null;
        if (incomingIp != null && !incomingIp.equals("")) {
            queue = queues.get(incomingIp);
        }

        // generate a unique queue id for this client if none was found
        if (queue == null) {
            queue = new TimedBlockingQueue(queueSize, true);
            queues.put(incomingIp, queue);
        }
        QUEUE_ID.set(incomingIp);
        ipAddresses.add(incomingIp);

        // queue token handling
        try {
            if (timeout > 0) {
                retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
            } else {
                queue.put(request);
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Unexpected interruption while "
                    + "blocking on the request queue");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("IpFlowController(" + queueSize + "," + incomingIp + ") queue size "
                    + queue.size());
            LOGGER.fine("IpFlowController(" + queueSize + "," + incomingIp + ") total queues "
                    + queues.size());
        }
        return retval;
    }

    protected String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }

}
