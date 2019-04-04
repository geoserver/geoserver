/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  * This code is licensed under the GPL 2.0 license, available at the root
 *  * application directory.
 *
 */

package org.geoserver.wfs3.response;

import java.io.IOException;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;

public class LandingPageHTMLResponse extends AbstractHTMLResponse {

    public LandingPageHTMLResponse(GeoServerResourceLoader loader, GeoServer geoServer)
            throws IOException {
        super(LandingPageDocument.class, loader, geoServer);
    }

    @Override
    protected String getTemplateName(Object value) {
        return "landingPage.ftl";
    }

    @Override
    protected ResourceInfo getResource(Object value) {
        return null;
    }

    @Override
    protected String getFileName(Object value, Operation operation) {
        return "landingPage";
    }
}
