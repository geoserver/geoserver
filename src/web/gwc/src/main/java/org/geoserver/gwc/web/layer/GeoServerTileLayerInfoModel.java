/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.apache.wicket.model.Model;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;

public class GeoServerTileLayerInfoModel extends Model<GeoServerTileLayerInfo> {
    private static final long serialVersionUID = 2246174669786551903L;

    private Boolean enabled;

    private final boolean isNew;

    public GeoServerTileLayerInfoModel(GeoServerTileLayerInfo info, boolean isNew) {
        super(info);
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
