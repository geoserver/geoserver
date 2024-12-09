/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import org.geoserver.config.GeoServer;
import org.geoserver.web.ogcapi.OgcApiServiceDescriptionProvider;
import org.geoserver.wfs.WFSInfo;

public class FeaturesServiceDescriptionProvider
        extends OgcApiServiceDescriptionProvider<WFSInfo, FeatureService> {

    public FeaturesServiceDescriptionProvider(GeoServer gs) {
        super(gs, "WFS", "Features", "Features");
    }
}
