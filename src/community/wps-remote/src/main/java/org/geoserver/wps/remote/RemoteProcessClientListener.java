/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;

/**
 * Interface allowing a {@link RemoteProcess} instance to listen to the {@link RemoteProcessClient}
 * messages.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface RemoteProcessClientListener {

    /**
     * Returns the assigned unique @param pId of the {@link RemoteProcess}
     *
     * @return
     */
    public String getPID();

    /**
     * Sets the progress of the {@link RemoteProcess} associated to the remote service with the
     * unique @param pId
     *
     * @param pId
     * @param progress
     */
    public void progress(final String pId, final Double progress);

    /**
     * Completes of the {@link RemoteProcess} associated to the remote service with the
     * unique @param pId
     *
     * @param pId
     * @param outputs
     */
    public void complete(final String pId, final Object outputs);

    /**
     * Raise an Exception to the {@link RemoteProcess} associated to the remote service with the
     * unique @param pId
     *
     * @param pId
     * @param cause
     * @param metadata
     */
    public void exceptionOccurred(final String pId, Exception cause, Map<String, Object> metadata);
}
