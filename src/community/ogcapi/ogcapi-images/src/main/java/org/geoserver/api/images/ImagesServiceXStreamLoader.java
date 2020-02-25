/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class ImagesServiceXStreamLoader extends XStreamServiceLoader<ImagesServiceInfo> {

    public ImagesServiceXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "tiles");
    }

    @Override
    protected ImagesServiceInfo createServiceFromScratch(GeoServer gs) {
        ImagesServiceInfoImpl info = new ImagesServiceInfoImpl();
        info.setName("images");
        info.setTitle("Image mosaicks discovery and management interface");
        return info;
    }

    @Override
    public Class<ImagesServiceInfo> getServiceClass() {
        return ImagesServiceInfo.class;
    }
}
