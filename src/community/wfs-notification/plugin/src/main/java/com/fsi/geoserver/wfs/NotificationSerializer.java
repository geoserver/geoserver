package com.fsi.geoserver.wfs;

import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.identity.Identifier;

public interface NotificationSerializer {

    String serializeInsertOrUpdate(Feature f);

    String serializeDelete(Name typeName, Identifier id);

}