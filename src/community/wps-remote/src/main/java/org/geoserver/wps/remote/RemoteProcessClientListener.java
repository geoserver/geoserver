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

    /** Returns the assigned unique @param pId of the {@link RemoteProcess} */
    public String getPID();

    /** Sets the progress of the {@link RemoteProcess} associated to the remote service with the */
    public void progress(final String pId, final Double progress);

    /**
     * Gets the progress of the {@link RemoteProcess} associated to the remote service with the
     *
     * @return progress
     */
    double getProgress(String pId);

    /** Completes of the {@link RemoteProcess} associated to the remote service with the */
    public void complete(final String pId, final Object outputs);

    /** Raise an Exception to the {@link RemoteProcess} associated to the remote service with the */
    public void exceptionOccurred(final String pId, Exception cause, Map<String, Object> metadata);

    /**
     * Expose a log message to the {@link RemoteProcess} progress listener associated to the remote
     */
    public void setTask(final String pId, final String logMessage);
}
