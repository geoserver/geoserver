/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.List;
import org.geoserver.ows.Request;

/**
 * Sources of FlowContoller for the {@link ControlFlowCallback}
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface FlowControllerProvider {

    /**
     * Returns the set of flow controllers to be used in the {@link ControlFlowCallback}, for the
     * given request. It is up to the FlowControllerProvider to manage the lifecycle of flow
     * controllers, and make sure they are not getting re-created on a request per request basis.
     * The flow controllers will be applied in the order they are returned
     */
    List<FlowController> getFlowControllers(Request request) throws Exception;

    /**
     * Maximum time the request can be held in queue before giving up to it.
     *
     * @return The maximum time in milliseconds. Use 0 or a negative number for no timeout
     */
    long getTimeout(Request request);
}
