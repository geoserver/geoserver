/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
