/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import java.io.IOException;
import java.util.Map;

/** Plugin interface used to transform the position of files during rest uploads */
public interface RESTUploadPathMapper {
    /**
     * Remaps the position of a store path. The implementor is free to append, modify or replace the
     * store root directory, REST upload will append workspace/store to it
     */
    void mapStorePath(
            StringBuilder rootDir, String workspace, String store, Map<String, String> storeParams)
            throws IOException;

    /**
     * Remaps the position of a file inside a store (e.g., a image being harvested into a mosaic.
     * The implementor is free to alter the item path.
     */
    void mapItemPath(
            String workspace,
            String store,
            Map<String, String> storeParams,
            StringBuilder itemPath,
            String itemName)
            throws IOException;
}
