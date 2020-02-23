/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * Pluggable callback exposing the Wicket {@link RequestCycle} stages
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface WicketCallback {

    /** Called when the request cycle object is beginning its response */
    void onBeginRequest();

    /** Called when the request cycle object has detached all request targets. */
    void onAfterTargetsDetached();

    /** Called when the request cycle object has finished its response */
    void onEndRequest();

    /** Called when a request target is set on the request cycle */
    void onRequestTargetSet(RequestCycle cycle, Class<? extends IRequestablePage> requestTarget);

    /**
     * Called when a runtime exception is thrown, just before the actual handling of the runtime
     * exception.
     *
     * @param cycle The request cycle
     * @param ex The exception
     */
    void onRuntimeException(org.apache.wicket.request.cycle.RequestCycle cycle, Exception ex);
}
