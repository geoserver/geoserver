/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import com.google.common.cache.Cache;
import java.io.Serializable;

public interface CacheProvider {

    public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(String cacheName);
}
