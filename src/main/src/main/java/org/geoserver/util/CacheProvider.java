/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.Serializable;

import com.google.common.cache.Cache;

public interface CacheProvider {

    public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(String cacheName);
}
