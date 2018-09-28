/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import org.geowebcache.grid.GridSet;

public class TilingSchemeDescriptionRequest extends BaseRequest {

    private GridSet gridSet;

    public GridSet getGridSet() {
        return gridSet;
    }

    public void setGridSet(GridSet gridSet) {
        this.gridSet = gridSet;
    }
}
