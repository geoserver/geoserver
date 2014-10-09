/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.geoserver.coverage.layer.CoverageTileLayerInfo;

public class CoverageTileLayerInfoModel extends GeoServerTileLayerInfoModel {

    private Boolean enabled;
    
    public CoverageTileLayerInfoModel(CoverageTileLayerInfo info, boolean isNew) {
        super(info, isNew); // TODO Auto-generated constructor stub
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
