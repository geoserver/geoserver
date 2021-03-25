/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.images;

import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;

/** Landing page for the images service */
public class ImagesLandingPage extends AbstractLandingPageDocument {

    public static final String IMAGES_SERVICE_BASE = "ogc/images";

    public ImagesLandingPage(String title, String description) {
        super(title, description, IMAGES_SERVICE_BASE);

        // collections
        addLinksFor(
                IMAGES_SERVICE_BASE + "/collections",
                ImagesCollectionsDocument.class,
                "Image collections metadata as ",
                "collections",
                null,
                Link.REL_DATA_URI);
    }
}
