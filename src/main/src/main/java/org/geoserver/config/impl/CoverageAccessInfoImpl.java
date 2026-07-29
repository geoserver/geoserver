/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.ThreadPoolExecutor;
import org.geoserver.config.CoverageAccessInfo;

public class CoverageAccessInfoImpl implements Serializable, CoverageAccessInfo {

    @Serial
    private static final long serialVersionUID = 8909514231467268331L;

    transient ThreadPoolExecutor threadPoolExecutor;

    public static final int DEFAULT_MaxPoolSize = 5;
    int maxPoolSize = DEFAULT_MaxPoolSize;

    public static final int DEFAULT_CorePoolSize = 5;
    int corePoolSize = DEFAULT_CorePoolSize;

    public static final int DEFAULT_KeepAliveTime = 30000;
    int keepAliveTime = DEFAULT_KeepAliveTime;

    public static final QueueType DEFAULT_QUEUE_TYPE = QueueType.UNBOUNDED;
    QueueType queueType = DEFAULT_QUEUE_TYPE;

    public static final long DEFAULT_ImageIOCacheThreshold = 10 * 1024;
    long imageIOCacheThreshold = DEFAULT_ImageIOCacheThreshold;

    // nullable, so a config saved before this setting existed deserializes as null and falls back to the default,
    // instead of a misleading explicit 0
    public static final int DEFAULT_GranuleCacheMaxSizeMB = 128;
    public static final int DEFAULT_GranuleCacheThresholdKB = 1024;
    Integer granuleCacheMaxSizeMB;
    Integer granuleCacheThresholdKB;

    public CoverageAccessInfoImpl() {
        threadPoolExecutor = null;
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    @Override
    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public int getCorePoolSize() {
        return corePoolSize;
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    @Override
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    @Override
    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    @Override
    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    @Override
    public QueueType getQueueType() {
        return queueType;
    }

    @Override
    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    @Override
    public void setImageIOCacheThreshold(long imageIOCacheThreshold) {
        this.imageIOCacheThreshold = imageIOCacheThreshold;
    }

    @Override
    public long getImageIOCacheThreshold() {
        return imageIOCacheThreshold;
    }

    @Override
    public int getGranuleCacheMaxSizeMB() {
        // null (never configured, or a pre-existing config) falls back to the default budget, so the cache is on
        // out of the box; it stays idle until a mosaic opts in via its read parameters
        return granuleCacheMaxSizeMB != null ? granuleCacheMaxSizeMB : DEFAULT_GranuleCacheMaxSizeMB;
    }

    @Override
    public void setGranuleCacheMaxSizeMB(int granuleCacheMaxSizeMB) {
        this.granuleCacheMaxSizeMB = granuleCacheMaxSizeMB;
    }

    @Override
    public int getGranuleCacheThresholdKB() {
        return granuleCacheThresholdKB != null ? granuleCacheThresholdKB : DEFAULT_GranuleCacheThresholdKB;
    }

    @Override
    public void setGranuleCacheThresholdKB(int granuleCacheThresholdKB) {
        this.granuleCacheThresholdKB = granuleCacheThresholdKB;
    }

    @Override
    public void dispose() {}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + corePoolSize;
        result = prime * result + (int) (imageIOCacheThreshold ^ (imageIOCacheThreshold >>> 32));
        result = prime * result + keepAliveTime;
        result = prime * result + maxPoolSize;
        result = prime * result + ((queueType == null) ? 0 : queueType.hashCode());
        result = prime * result + getGranuleCacheMaxSizeMB();
        result = prime * result + getGranuleCacheThresholdKB();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CoverageAccessInfoImpl other = (CoverageAccessInfoImpl) obj;
        if (corePoolSize != other.corePoolSize) return false;
        if (imageIOCacheThreshold != other.imageIOCacheThreshold) return false;
        if (keepAliveTime != other.keepAliveTime) return false;
        if (maxPoolSize != other.maxPoolSize) return false;
        if (getGranuleCacheMaxSizeMB() != other.getGranuleCacheMaxSizeMB()) return false;
        if (getGranuleCacheThresholdKB() != other.getGranuleCacheThresholdKB()) return false;
        if (queueType == null) {
            if (other.queueType != null) return false;
        } else if (!queueType.equals(other.queueType)) return false;
        return true;
    }

    @Override
    public CoverageAccessInfoImpl clone() {
        try {
            return (CoverageAccessInfoImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
