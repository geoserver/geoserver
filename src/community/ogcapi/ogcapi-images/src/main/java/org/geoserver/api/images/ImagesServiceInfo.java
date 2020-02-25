/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import org.geoserver.config.ServiceInfo;

/** Images 1.0 service configuration. */
public interface ImagesServiceInfo extends ServiceInfo {

    /** Returns the max number of images returned during paging */
    public int getMaxImages();

    /** Sets the maximum number of images returned during paging */
    public void setMaxImages(int maxImages);
}
