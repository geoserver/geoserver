/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.Collection;

/**
 * A builder for the flow controllers
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public interface ControlFlowConfigurator {

    /**
     * Builds the set of flow controllers to be used in the {@link ControlFlowCallback}
     */
    Collection<FlowController> buildFlowControllers() throws Exception;
    
    /**
     * Maximum time the request can be held in queue before giving up to it.
     * @return The maximum time in milliseconds. Use 0 or a negative number for no timeout
     */
    long getTimeout();

    /**
     * Returns true if the set of flow controllers changed since last invocation of
     * {@link #buildFlowControllers()}
     * 
     * @return
     */
    boolean isStale();
}
