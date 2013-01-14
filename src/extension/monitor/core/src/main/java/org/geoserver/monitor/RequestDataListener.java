/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

/**
 * Listens to RequestData events. Instances will be picked up by the monitoring extension from the
 * spring context, register an instance there to have monitoring report events to it
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public interface RequestDataListener {
    /**
     * Reports that the request is started. Only the RequestData fields that can be derived from the
     * initial request and do not require post-processing will be filled
     * 
     * @param rd
     */
    void requestStarted(RequestData rd);

    /**
     * Fired when the request data is updated
     * 
     * @param rd
     */
    void requestUpdated(RequestData rd);

    /**
     * Fired when the request data information is filled completely and is being stored as history
     * in the {@link MonitorDAO}
     * 
     * @param rd
     */
    void requestCompleted(RequestData rd);

    /**
     * Fired when the post processing on the RequestData is complete (this happen in a secondary
     * thread, of the request one)
     * 
     * @param rd
     */
    void requestPostProcessed(RequestData rd);
}
