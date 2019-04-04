/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.ExtensionFilter;
import org.geoserver.util.CacheProvider;
import org.geotools.util.logging.Logging;

/**
 * Filter to exclude extensions that conflict with hz-cluster.
 *
 * @author Kevin Smith, Boundless
 */
public class HzExtensionFilter implements ExtensionFilter {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    public HzExtensionFilter() {}

    @Override
    public boolean exclude(String beanId, Object bean) {
        if ((bean instanceof CacheProvider) && !(bean instanceof HzCacheProvider)) {
            // If another extension does this too then we're in trouble as only the default will be
            // used.
            LOGGER.log(
                    Level.INFO,
                    "hz-cluster module is supressing conflicting CacheProvider {0}",
                    new Object[] {beanId});
            return true;
        }
        return false;
    }
}
