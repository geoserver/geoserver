/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.opengis.wfs20.impl.GetFeatureTypeImpl;

/** This class extends WFS 2.0 GetFeatureType just to allow having a custom KVP reader for it */
public class GetFeatureType extends GetFeatureTypeImpl {

    private Integer resolution;

    /** Custom vector tile resolution */
    public Integer getResolution() {
        return resolution;
    }

    public void setResolution(Integer resolution) {
        this.resolution = resolution;
    }
}
