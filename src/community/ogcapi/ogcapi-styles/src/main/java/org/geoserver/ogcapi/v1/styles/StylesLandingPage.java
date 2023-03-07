/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.LinksBuilder;

/** Landing page for the styles service */
public class StylesLandingPage extends AbstractLandingPageDocument {

    public static final String STYLES_SERVICE_BASE = "ogc/styles/v1";

    public StylesLandingPage(String title, String description) {
        super(title, description, STYLES_SERVICE_BASE);

        // collections
        new LinksBuilder(StylesDocument.class, STYLES_SERVICE_BASE)
                .segment("/styles")
                .title("Styles Metadata as ")
                .rel("styles")
                .add(this);
    }
}
