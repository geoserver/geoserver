/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

/**
 * Listens to RequestData events. Instances will be picked up by the monitoring extension from the
 * spring context, register an instance there to have monitoring report events to it
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface RequestDataListener {
    /**
     * Reports that the request is started. Only the RequestData fields that can be derived from the
     * initial request and do not require post-processing will be filled
     */
    void requestStarted(RequestData rd);

    /** Fired when the request data is updated */
    void requestUpdated(RequestData rd);

    /**
     * Fired when the request data information is filled completely and is being stored as history
     * in the {@link MonitorDAO}
     */
    void requestCompleted(RequestData rd);

    /**
     * Fired when the post processing on the RequestData is complete (this happen in a secondary
     * thread, of the request one)
     */
    void requestPostProcessed(RequestData rd);
}
