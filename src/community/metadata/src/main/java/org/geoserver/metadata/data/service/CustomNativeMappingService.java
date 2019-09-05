package org.geoserver.metadata.data.service;

import java.util.List;
import org.geoserver.catalog.LayerInfo;

public interface CustomNativeMappingService {

    void mapCustomToNative(LayerInfo layer);

    default void mapNativeToCustom(LayerInfo layer) {
        mapNativeToCustom(layer, null);
    }

    void mapNativeToCustom(LayerInfo layer, List<Integer> indexes);
}
