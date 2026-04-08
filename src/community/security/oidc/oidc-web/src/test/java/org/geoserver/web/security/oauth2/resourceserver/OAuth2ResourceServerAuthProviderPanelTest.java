/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.resourceserver;

import static org.junit.Assert.assertNotNull;

import org.apache.wicket.model.Model;
import org.geoserver.security.oauth2.resourceserver.GeoServerOAuth2ResourceServerFilterConfig;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/** Tests for {@link OAuth2ResourceServerAuthProviderPanel}. */
public class OAuth2ResourceServerAuthProviderPanelTest extends GeoServerWicketTestSupport {

    @Test
    public void smokeTest() {
        Model<GeoServerOAuth2ResourceServerFilterConfig> model =
                new Model<>(new GeoServerOAuth2ResourceServerFilterConfig());
        FormTestPage testPage = new FormTestPage(id -> new OAuth2ResourceServerAuthProviderPanel(id, model));
        tester.startPage(testPage);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testPanelRendersIssuerUriField() {
        Model<GeoServerOAuth2ResourceServerFilterConfig> model =
                new Model<>(new GeoServerOAuth2ResourceServerFilterConfig());
        FormTestPage testPage = new FormTestPage(id -> new OAuth2ResourceServerAuthProviderPanel(id, model));
        tester.startPage(testPage);

        // Verify that the issuerUri field is rendered
        tester.assertComponent("form:panel:issuerUri", org.apache.wicket.markup.html.form.TextField.class);
    }

    @Test
    public void testPanelRendersHelpLinks() {
        Model<GeoServerOAuth2ResourceServerFilterConfig> model =
                new Model<>(new GeoServerOAuth2ResourceServerFilterConfig());
        FormTestPage testPage = new FormTestPage(id -> new OAuth2ResourceServerAuthProviderPanel(id, model));
        tester.startPage(testPage);

        // Verify help links are present
        tester.assertComponent("form:panel:resourceServerParametersHelp", org.geoserver.web.wicket.HelpLink.class);
        tester.assertComponent("form:panel:issuerUriHelp", org.geoserver.web.wicket.HelpLink.class);
    }

    @Test
    public void testConfigModel() {
        GeoServerOAuth2ResourceServerFilterConfig config = new GeoServerOAuth2ResourceServerFilterConfig();
        config.setIssuerUri("https://test.example.com");

        Model<GeoServerOAuth2ResourceServerFilterConfig> model = new Model<>(config);

        assertNotNull(model.getObject());
        assertNotNull(model.getObject().getIssuerUri());
    }
}
