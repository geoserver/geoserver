/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import org.geoserver.config.impl.ServiceInfoImpl;

public class DGGSInfoImpl extends ServiceInfoImpl implements DGGSInfo {

    Integer maxNumberOfZonesForPreview = DEFAULT_MAX_ZONES_PREVIEW;
    Integer maxNeighborDistance = DEFAULT_MAX_NEIGHBOR_DISTANCE;

    @Override
    public String getType() {
        return "DGGS";
    }

    @Override
    public int getMaxNumberOfZonesForPreview() {
        return maxNumberOfZonesForPreview == null
                ? DEFAULT_MAX_ZONES_PREVIEW
                : maxNumberOfZonesForPreview;
    }

    @Override
    public void setMaxNumberOfZonesForPreview(int maxNumberOfZonesForPreview) {
        this.maxNumberOfZonesForPreview = maxNumberOfZonesForPreview;
    }

    @Override
    public int getMaxNeighborDistance() {
        return maxNeighborDistance == null ? DEFAULT_MAX_NEIGHBOR_DISTANCE : maxNeighborDistance;
    }

    @Override
    public void setMaxNeighborDistance(int maxNeighborDistance) {
        this.maxNeighborDistance = maxNeighborDistance;
    }
}
