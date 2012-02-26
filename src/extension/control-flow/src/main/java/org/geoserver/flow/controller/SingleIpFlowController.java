/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.geoserver.ows.Request;

/**
 * A flow controller that throttles concurrent requests made from the same ip (single ip, specified in configuration file)
 * 
 * @author Juan Marin, OpenGeo
 */
public class SingleIpFlowController extends IpFlowController {

    public SingleIpFlowController(int queueSize) {
        super(queueSize);
    }

    public SingleIpFlowController(int queueSize, String ip) {
        super(queueSize);
        ipAddresses.add(ip);
    }

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        String incomingIp = getRemoteAddr(request.getHttpRequest());
        if (incomingIp.equals(ipAddresses.get(0))) {
            TimedBlockingQueue queue = null;
            if (incomingIp != null && !incomingIp.equals("")) {
                queue = queues.get(incomingIp);
            }

            if (queue == null) {
                queue = new TimedBlockingQueue(queueSize, true);
                queues.put(incomingIp, queue);
            }
            QUEUE_ID.set(incomingIp);

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
                LOGGER.fine("SingleIpFlowController(" + queueSize + "," + incomingIp
                        + ") queue size " + queue.size());
                LOGGER.fine("SingleIpFlowController(" + queueSize + "," + incomingIp
                        + ") total queues " + queues.size());
            }
        }
        return retval;
    }

}
