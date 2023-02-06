/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import org.geoserver.config.impl.ServiceInfoImpl;

public class ImagesServiceInfoImpl extends ServiceInfoImpl implements ImagesServiceInfo {

    int maxImages = 1000;

    @Override
    public String getType() {
        return "Images";
    }

    @Override
    public int getMaxImages() {
        return maxImages;
    }

    @Override
    public void setMaxImages(int maxImages) {
        this.maxImages = maxImages;
    }
}
