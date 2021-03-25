package org.geoserver.ogcapi.dggs;

import org.geoserver.config.ServiceInfo;

public interface DGGSInfo extends ServiceInfo {

    static final int DEFAULT_MAX_ZONES_PREVIEW = 50;
    static final int DEFAULT_MAX_NEIGHBOR_DISTANCE = 50;

    /**
     * Get the maximum number of zones to be displayed in a preview. Can be defined by the user. By
     * default, 50.
     *
     * @return maxNumberOfZonesForPreview
     */
    int getMaxNumberOfZonesForPreview();

    /**
     * Sets the maximum number of zones to be displayed in a preview
     *
     * @param maxNumberOfZonesForPreview
     */
    void setMaxNumberOfZonesForPreview(int maxNumberOfZonesForPreview);

    /** Returns maximum neighboring distance, by default, 50 */
    int getMaxNeighborDistance();

    /**
     * Sets the maximum neighbor request distance
     *
     * @param maxNeighborDistance
     */
    void setMaxNeighborDistance(int maxNeighborDistance);
}
