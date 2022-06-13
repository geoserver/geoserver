/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.util.UUID;

public interface GlobalModelService {

    Object get(UUID key);

    Object put(UUID key, Object value);

    Object delete(UUID key);
}
