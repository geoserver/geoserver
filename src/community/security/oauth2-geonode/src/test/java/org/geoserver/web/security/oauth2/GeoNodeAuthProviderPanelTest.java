/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.apache.wicket.model.Model;
import org.geoserver.security.oauth2.GeoNodeOAuth2FilterConfig;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GeoNodeAuthProviderPanelTest extends GeoServerWicketTestSupport {

    @Test
    public void smokeTest() {
        Model<GeoNodeOAuth2FilterConfig> model = new Model<>(new GeoNodeOAuth2FilterConfig());
        tester.startComponentInPage(new GeoNodeOAuth2AuthProviderPanel("geonode", model));
    }
}
