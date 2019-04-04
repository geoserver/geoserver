/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

public interface TileLayerCatalogListener {

    enum Type {
        CREATE,
        MODIFY,
        DELETE
    }

    void onEvent(String layerId, Type type);
}
