/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;

/** Landing page for the styles service */
public class GeoVolumesLandingPage extends AbstractLandingPageDocument {

    public static final String GEOVOLUMES_SERVICE_BASE = "ogc/3dgeovolumes/v1";

    public GeoVolumesLandingPage(String title, String description) {
        super(title, description, GEOVOLUMES_SERVICE_BASE);

        // collections
        new LinksBuilder(GeoVolumes.class, GEOVOLUMES_SERVICE_BASE)
                .segment("/collections")
                .title("Collections ")
                .rel(Link.REL_DATA_URI)
                .add(this);
    }
}
