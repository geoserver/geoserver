/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.geoserver.metadata.data.service.GlobalModelService;
import org.springframework.stereotype.Component;

@Component
public class GlobalModelServiceImpl implements GlobalModelService {

    private Map<UUID, Object> data = new HashMap<>();

    @Override
    public Object get(UUID key) {
        return data.get(key);
    }

    @Override
    public Object put(UUID key, Object value) {
        return data.put(key, value);
    }

    @Override
    public Object delete(UUID key) {
        return data.remove(key);
    }
}
