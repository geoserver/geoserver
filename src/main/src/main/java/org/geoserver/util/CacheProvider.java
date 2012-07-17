package org.geoserver.util;

import java.io.Serializable;

import com.google.common.cache.Cache;

public interface CacheProvider {

    public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(String cacheName);
}
