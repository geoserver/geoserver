/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Coverage Settings configuration.
 *
 * @author Daniele Romagnoli, GeoSolutions
 */
public interface CoverageAccessInfo extends Cloneable, Serializable {

    public enum QueueType {
        UNBOUNDED,
        DIRECT
    }

    /** The threadPoolExecutor */
    ThreadPoolExecutor getThreadPoolExecutor();

    void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor);

    /** The ThreadPoolExecutor core pool size. */
    int getCorePoolSize();

    void setCorePoolSize(int corePoolSize);

    /** The ThreadPoolExecutor keep alive time. */
    int getKeepAliveTime();

    void setKeepAliveTime(int keepAliveTime);

    /** The ThreadPoolExecutor max pool size. */
    int getMaxPoolSize();

    void setMaxPoolSize(int maxPoolSize);

    /** The ThreadPoolExecutor queue type. */
    QueueType getQueueType();

    void setQueueType(QueueType queueType);

    /** Disposes the global configuration object. */
    void dispose();

    /** Flag controlling the image io cache. */
    void setImageIOCacheThreshold(long threshold);

    long getImageIOCacheThreshold();

    public CoverageAccessInfo clone();
}
