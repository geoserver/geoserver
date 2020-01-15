package org.geoserver.metadata.data.service;

import java.util.UUID;

public interface GlobalModelService {

    Object get(UUID key);

    Object put(UUID key, Object value);

    Object delete(UUID key);
}
