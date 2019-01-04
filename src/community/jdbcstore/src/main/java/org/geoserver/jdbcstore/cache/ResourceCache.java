/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore.cache;

import java.io.File;
import java.io.IOException;
import org.geoserver.platform.resource.Resource;

/**
 * Interface for Resource Cache.
 *
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public interface ResourceCache {
    public File cache(Resource res, boolean createDirectory) throws IOException;
}
