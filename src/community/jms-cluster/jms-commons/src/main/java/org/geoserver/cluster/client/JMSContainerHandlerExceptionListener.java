/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.client;

public interface JMSContainerHandlerExceptionListener {

    /**
     * @see {@link DefaultMessageListenerContainer#handleListenerSetupFailure(Throwable, boolean)}
     * @param ex - the incoming exception to handle
     * @param alreadyRecovered - true if the error is already recovered by a different handler
     */
    public void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered);
}
