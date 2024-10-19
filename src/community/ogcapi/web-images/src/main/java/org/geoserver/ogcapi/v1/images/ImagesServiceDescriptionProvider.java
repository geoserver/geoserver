/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;

public class ImagesServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<ImagesServiceInfo, ImagesService> {

    public ImagesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "Experimental", "Images", "Images");
    }
}
