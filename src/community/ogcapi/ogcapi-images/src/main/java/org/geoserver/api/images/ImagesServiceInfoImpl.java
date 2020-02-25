/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import org.geoserver.config.impl.ServiceInfoImpl;

public class ImagesServiceInfoImpl extends ServiceInfoImpl implements ImagesServiceInfo {

    int maxImages = 1000;

    public int getMaxImages() {
        return maxImages;
    }

    public void setMaxImages(int maxImages) {
        this.maxImages = maxImages;
    }
}
