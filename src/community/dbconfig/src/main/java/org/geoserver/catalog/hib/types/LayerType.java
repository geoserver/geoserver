package org.geoserver.catalog.hib.types;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.hibernate.types.EnumUserType;

public class LayerType extends EnumUserType<LayerInfo.Type> {

    public LayerType() {
        super(LayerInfo.Type.class);
    }

}
