/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;

/** Landing page for the images service */
public class ImagesLandingPage extends AbstractLandingPageDocument {

    public static final String IMAGES_SERVICE_BASE = "ogc/images/v1";

    public ImagesLandingPage(String title, String description) {
        super(title, description, IMAGES_SERVICE_BASE);

        // collections
        new LinksBuilder(ImagesCollectionsDocument.class, IMAGES_SERVICE_BASE)
                .segment("/collections")
                .title("Image collections metadata as ")
                .rel(Link.REL_DATA_URI)
                .add(this);
    }
}
