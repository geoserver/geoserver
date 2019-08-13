/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractLandingPageDocument;

/** Landing page for the styles service */
public class StylesLandingPage extends AbstractLandingPageDocument {

    public static final String STYLES_SERVICE_BASE = "ogc/styles";

    public StylesLandingPage(String title, String description) {
        super(title, description, STYLES_SERVICE_BASE);

        final APIRequestInfo requestInfo = APIRequestInfo.get();
        String baseUrl = requestInfo.getBaseURL();

        // collections
        addLinksFor(
                baseUrl,
                STYLES_SERVICE_BASE + "/styles",
                StylesDocument.class,
                "Styles Metadata as ",
                "styles",
                null,
                "data");
    }
}
