/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.List;
import org.geoserver.config.ServiceInfo;
import org.geoserver.security.CatalogMode;

/**
 * Configuration related
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public interface WPSInfo extends ServiceInfo {

    /**
     * Returns the connection timeout (in seconds). It represents the timeout to be used during WPS
     * execute requests, when opening the connection/reading through it.
     *
     * @return the timeout, or -1 if infinite timeout.
     */
    double getConnectionTimeout();

    /**
     * Sets the connection timeout (in seconds) to be used in WPS execute requests. -1 for infinite
     * timeout
     */
    void setConnectionTimeout(double timeout);

    /**
     * Returns the resource expiration timeout (in seconds). Temporary resources such as stored
     * Execute responses and output stored as reference will be deleted after this timeout
     */
    int getResourceExpirationTimeout();

    /** Sets the resource expiration timeout. */
    void setResourceExpirationTimeout(int resourceExpirationTimeout);

    /**
     * Returns the maximum number of processes that can run in synchronous mode in parallel.
     * Defaults to the number of available CPU cores
     */
    public int getMaxSynchronousProcesses();

    /** Sets the maximum number of processes that can run in synchronous mode in parallel. */
    public void setMaxSynchronousProcesses(int maxSynchronousProcesses);

    /**
     * Returns the maximum number of processes that can run in asynchronous mode in parallel.
     * Defaults to the number of available CPU cores
     */
    public int getMaxAsynchronousProcesses();

    /** Sets the maximum number of processes that can run in asynchronous mode in parallel. */
    public void setMaxAsynchronousProcesses(int maxAsynchronousProcesses);

    /** Retrieves the process groups configurations */
    public List<ProcessGroupInfo> getProcessGroups();

    /** Gets the current output storage directory */
    public String getStorageDirectory();

    /**
     * Sets the output storage directory, that is, the directory used to store the request, status
     * and final response of asynch requests, as well as any output that is meant to be referred to
     * by URL instead of being included inline in Execute the response.
     */
    public void setStorageDirectory(String storageDirectory);

    /**
     * Controls how the server allows access to secured processes, in a similar way to how the
     * catalog controls access to secured layers
     */
    public CatalogMode getCatalogMode();

    /** Sets the policy to control access to secured processes */
    public void setCatalogMode(CatalogMode catalogMode);

    /**
     * Returns the global maximum size of a complex input, in MB. Per process configuration can
     * override it. Zero or a negative number means no limit.
     */
    public int getMaxComplexInputSize();

    /**
     * Sets the global maximum size of a complex input, in MB. Per process configuration can
     * override it. Zero or a negative number means no limit.
     */
    public void setMaxComplexInputSize(int maxInputSizeMB);

    /**
     * How many seconds a process can run in asynchronous mode (with the user polling for its
     * status) before it gets killed by the WPS container (0 or a negative value means no limit)
     */
    public abstract int getMaxAsynchronousExecutionTime();

    /**
     * How many seconds a process can run or queue in asynchronous mode (with the user polling for
     * its status) before it gets killed by the WPS container (0 or a negative value means no limit)
     */
    public abstract Integer getMaxAsynchronousTotalTime();

    /**
     * Sets how many seconds a process can run in asynchronous mode (with the user polling for its
     * status) before it gets killed by the WPS container (0 or a negative value means no limit)
     */
    public abstract void setMaxAsynchronousExecutionTime(int maxAsynchronousExecutionTime);

    /**
     * Sets how many seconds a process can run or queue in asynchronous mode (with the user polling
     * for its status) before it gets killed by the WPS container (0 or a negative value means no
     * limit)
     */
    public abstract void setMaxAsynchronousTotalTime(Integer maxAsynchronousTotalTime);

    /**
     * How many seconds a process can run in synchronous mode (with the user waiting on the HTTP
     * connection) before it gets killed by the WPS container (0 or a negative value means no limit)
     */
    public abstract int getMaxSynchronousExecutionTime();

    /**
     * How many seconds a process can run or queue in synchronous mode (with the user waiting on the
     * HTTP connection) before it gets killed by the WPS container (0 or a negative value means no
     * limit)
     */
    public abstract Integer getMaxSynchronousTotalTime();

    /**
     * Sets how many seconds a process can run in synchronous mode (with the user waiting on the
     * HTTP connection) before it gets killed by the WPS container (0 or a negative value means no
     * limit)
     */
    public abstract void setMaxSynchronousExecutionTime(int maxSynchronousExecutionTime);

    /**
     * Sets how many seconds a process can run or queue in synchronous mode (with the user waiting
     * on the HTTP connection) before it gets killed by the WPS container (0 or a negative value
     * means no limit)
     */
    public abstract void setMaxSynchronousTotalTime(Integer maxSynchronousTotalTime);
}
