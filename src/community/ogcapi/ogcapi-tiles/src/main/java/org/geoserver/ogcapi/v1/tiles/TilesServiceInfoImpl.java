/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import org.geoserver.config.impl.ServiceInfoImpl;

public class TilesServiceInfoImpl extends ServiceInfoImpl implements TilesServiceInfo {
    @Override
    public String getType() {
        return "Tiles";
    }
}
