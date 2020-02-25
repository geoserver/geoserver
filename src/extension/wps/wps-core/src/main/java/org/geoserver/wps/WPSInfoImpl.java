/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.security.CatalogMode;

/**
 * WPS information implementation
 *
 * @author Lucas Reed, Refractions Research Inc
 */
@SuppressWarnings("unchecked")
public class WPSInfoImpl extends ServiceInfoImpl implements WPSInfo {

    static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";

    static final Double DEFAULT_CONNECTION_TIMEOUT = 30.0;

    static final String KEY_RESOURCE_EXPIRATION_TIMEOUT = "resourceExpirationTimeout";

    static final int DEFAULT_RESOURCE_EXPIRATION_TIMEOUT = 60 * 5;

    static final String KEY_MAX_SYNCH = "maxSynchronousProcesses";

    static final int DEFAULT_MAX_SYNCH = Runtime.getRuntime().availableProcessors();

    static final String KEY_MAX_ASYNCH = "maxAsynchronousProcesses";

    static final int DEFAULT_MAX_ASYNCH = Runtime.getRuntime().availableProcessors();

    /**
     * Connection timeout in seconds. Using a double allows for fractional values, like as an
     * instance, half a second ==> 0.5
     */
    Double connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    /** Resource expiration timeout in seconds. */
    Integer resourceExpirationTimeout = DEFAULT_RESOURCE_EXPIRATION_TIMEOUT;

    /** Maximum number of synchronous requests running in parallel */
    Integer maxSynchronousProcesses = DEFAULT_MAX_SYNCH;

    /** Maximum number of asynchronous requests running in parallel */
    Integer maxAsynchronousProcesses = DEFAULT_MAX_ASYNCH;

    /** List of process groups/factories. */
    List<ProcessGroupInfo> processGroups = new ArrayList<ProcessGroupInfo>();

    /** Where to store the WPS artifacts (inputs, outputs, and so on) */
    String storageDirectory;

    /**
     * How to handle requests for processes that have been secured, and should not be reached
     * without the proper authentication
     */
    CatalogMode catalogMode;

    /**
     * The global maximum size of a complex input, in MB. Per process configuration can override it
     */
    int maxComplexInputSize;

    /**
     * How many seconds a process can run in synchronous mode (with the user waiting on the HTTP
     * connection) before it gets killed by the WPS container
     */
    int maxSynchronousExecutionTime;

    /**
     * How many seconds a process can run or queue in synchronous mode (with the user waiting on the
     * HTTP connection) before it gets killed by the WPS container
     */
    Integer maxSynchronousTotalTime;

    /**
     * How many seconds a process can run in asynchronous mode (with the user polling for its
     * status) before it gets killed by the WPS container
     */
    int maxAsynchronousExecutionTime;

    /*
     * How many seconds a process can run or queue in asynchronous mode (with the user polling for its status)
     * before it gets killed by the WPS container
     */
    Integer maxAsynchronousTotalTime;

    public WPSInfoImpl() {
        title = "Prototype GeoServer WPS";
    }

    /**
     * Returns the connection timeout (in seconds). It represents the timeout to be used during WPS
     * execute requests, when opening the connection/reading through it.
     *
     * @return the timeout, or -1 if infinite timeout.
     */
    public double getConnectionTimeout() {
        if (connectionTimeout == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if (md == null) {
                return DEFAULT_CONNECTION_TIMEOUT;
            }
            Double timeout = md.get(KEY_CONNECTION_TIMEOUT, Double.class);
            if (timeout == null) {
                return DEFAULT_CONNECTION_TIMEOUT;
            }
            connectionTimeout = timeout;
        }

        return connectionTimeout;
    }

    /**
     * Sets the connection timeout (in seconds) to be used in WPS execute requests. -1 for infinite
     * timeout
     */
    public void setConnectionTimeout(double connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getResourceExpirationTimeout() {
        if (resourceExpirationTimeout == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if (md == null) {
                return DEFAULT_RESOURCE_EXPIRATION_TIMEOUT;
            }
            Integer timeout = md.get(KEY_RESOURCE_EXPIRATION_TIMEOUT, Integer.class);
            if (timeout == null) {
                return DEFAULT_RESOURCE_EXPIRATION_TIMEOUT;
            }
            resourceExpirationTimeout = timeout;
        }

        return resourceExpirationTimeout;
    }

    public void setResourceExpirationTimeout(int resourceExpirationTimeout) {
        this.resourceExpirationTimeout = resourceExpirationTimeout;
    }

    public int getMaxSynchronousProcesses() {
        if (maxSynchronousProcesses == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if (md == null) {
                return DEFAULT_MAX_SYNCH;
            }
            Integer max = md.get(KEY_MAX_SYNCH, Integer.class);
            if (max == null) {
                return DEFAULT_MAX_SYNCH;
            }
            maxSynchronousProcesses = max;
        }

        return maxSynchronousProcesses;
    }

    public void setMaxSynchronousProcesses(int maxSynchronousProcesses) {
        this.maxSynchronousProcesses = maxSynchronousProcesses;
    }

    public int getMaxAsynchronousProcesses() {
        if (maxAsynchronousProcesses == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if (md == null) {
                return DEFAULT_MAX_ASYNCH;
            }
            Integer max = md.get(KEY_MAX_ASYNCH, Integer.class);
            if (max == null) {
                return DEFAULT_MAX_ASYNCH;
            }
            maxAsynchronousProcesses = max;
        }

        return maxAsynchronousProcesses;
    }

    public void setMaxAsynchronousProcesses(int maxAsynchronousProcesses) {
        this.maxAsynchronousProcesses = maxAsynchronousProcesses;
    }

    @Override
    public List<ProcessGroupInfo> getProcessGroups() {
        return processGroups;
    }

    public void setProcessGroups(List<ProcessGroupInfo> processGroups) {
        this.processGroups = processGroups;
    }

    @Override
    public String getStorageDirectory() {
        return storageDirectory;
    }

    @Override
    public void setStorageDirectory(String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    @Override
    public CatalogMode getCatalogMode() {
        if (catalogMode == null) {
            catalogMode = CatalogMode.HIDE;
        }
        return catalogMode;
    }

    @Override
    public void setCatalogMode(CatalogMode catalogMode) {
        this.catalogMode = catalogMode;
    }

    public int getMaxComplexInputSize() {
        return maxComplexInputSize;
    }

    public void setMaxComplexInputSize(int maxComplexInputSize) {
        this.maxComplexInputSize = maxComplexInputSize;
    }

    /**
     * How many seconds a process can run in synchronous mode (with the user waiting on the HTTP
     * connection) before it gets killed by the WPS container
     */
    @Override
    public int getMaxSynchronousExecutionTime() {
        return maxSynchronousExecutionTime;
    }

    @Override
    public void setMaxSynchronousExecutionTime(int maxSynchronousExecutionTime) {
        this.maxSynchronousExecutionTime = maxSynchronousExecutionTime;
    }

    /**
     * How many seconds a process can run or queue in synchronous mode (with the user waiting on the
     * HTTP connection) before it gets killed by the WPS container
     */
    @Override
    public Integer getMaxSynchronousTotalTime() {
        return (maxSynchronousTotalTime != null)
                ? maxSynchronousTotalTime
                : maxSynchronousExecutionTime;
    }

    @Override
    public void setMaxSynchronousTotalTime(Integer maxSynchronousTotalTime) {
        this.maxSynchronousTotalTime = maxSynchronousTotalTime;
    }

    /**
     * How many seconds a process can run in synchronous mode (with the user polling for its status)
     * before it gets killed by the WPS container
     */
    @Override
    public int getMaxAsynchronousExecutionTime() {
        return maxAsynchronousExecutionTime;
    }

    @Override
    public void setMaxAsynchronousExecutionTime(int maxAsynchronousExecutionTime) {
        this.maxAsynchronousExecutionTime = maxAsynchronousExecutionTime;
    }

    /**
     * How many seconds a process can run in synchronous mode (with the user polling for its status)
     * before it gets killed by the WPS container
     */
    @Override
    public Integer getMaxAsynchronousTotalTime() {
        return (maxAsynchronousTotalTime != null)
                ? maxAsynchronousTotalTime
                : maxAsynchronousExecutionTime;
    }

    @Override
    public void setMaxAsynchronousTotalTime(Integer maxAsynchronousTotalTime) {
        this.maxAsynchronousTotalTime = maxAsynchronousTotalTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((catalogMode == null) ? 0 : catalogMode.hashCode());
        result = prime * result + ((connectionTimeout == null) ? 0 : connectionTimeout.hashCode());
        result =
                prime * result
                        + ((maxAsynchronousProcesses == null)
                                ? 0
                                : maxAsynchronousProcesses.hashCode());
        result = prime * result + maxAsynchronousExecutionTime;
        result =
                prime * result
                        + ((maxAsynchronousTotalTime == null)
                                ? 0
                                : maxAsynchronousTotalTime.hashCode());
        result = prime * result + maxComplexInputSize;
        result = prime * result + maxSynchronousExecutionTime;
        result =
                prime * result
                        + ((maxSynchronousTotalTime == null)
                                ? 0
                                : maxSynchronousTotalTime.hashCode());
        result =
                prime * result
                        + ((maxSynchronousProcesses == null)
                                ? 0
                                : maxSynchronousProcesses.hashCode());
        result = prime * result + ((processGroups == null) ? 0 : processGroups.hashCode());
        result =
                prime * result
                        + ((resourceExpirationTimeout == null)
                                ? 0
                                : resourceExpirationTimeout.hashCode());
        result = prime * result + ((storageDirectory == null) ? 0 : storageDirectory.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        WPSInfoImpl other = (WPSInfoImpl) obj;
        if (catalogMode != other.catalogMode) return false;
        if (connectionTimeout == null) {
            if (other.connectionTimeout != null) return false;
        } else if (!connectionTimeout.equals(other.connectionTimeout)) return false;
        if (maxAsynchronousProcesses == null) {
            if (other.maxAsynchronousProcesses != null) return false;
        } else if (!maxAsynchronousProcesses.equals(other.maxAsynchronousProcesses)) return false;
        if (maxAsynchronousExecutionTime != other.maxAsynchronousExecutionTime) return false;
        if (maxAsynchronousTotalTime == null) {
            if (other.maxAsynchronousTotalTime != null) return false;
        } else if (!maxAsynchronousTotalTime.equals(other.maxAsynchronousTotalTime)) return false;
        if (maxComplexInputSize != other.maxComplexInputSize) return false;
        if (maxSynchronousExecutionTime != other.maxSynchronousExecutionTime) return false;
        if (maxSynchronousTotalTime == null) {
            if (other.maxSynchronousTotalTime != null) return false;
        } else if (!maxSynchronousTotalTime.equals(other.maxSynchronousTotalTime)) return false;
        if (maxSynchronousProcesses == null) {
            if (other.maxSynchronousProcesses != null) return false;
        } else if (!maxSynchronousProcesses.equals(other.maxSynchronousProcesses)) return false;
        if (processGroups == null) {
            if (other.processGroups != null) return false;
        } else if (!processGroups.equals(other.processGroups)) return false;
        if (resourceExpirationTimeout == null) {
            if (other.resourceExpirationTimeout != null) return false;
        } else if (!resourceExpirationTimeout.equals(other.resourceExpirationTimeout)) return false;
        if (storageDirectory == null) {
            if (other.storageDirectory != null) return false;
        } else if (!storageDirectory.equals(other.storageDirectory)) return false;
        return true;
    }
}
