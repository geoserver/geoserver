/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.Model;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.UserDetailsDisplaySettingsInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class EmailLabelTest extends GeoServerWicketTestSupport {

    @Test
    public void testHiddenMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setEmailDisplayMode(UserDetailsDisplaySettingsInfo.EmailDisplayMode.HIDDEN);
        gs.save(global);

        EmailLabel emailLabel = new EmailLabel("label", Model.of("test@example.com"));

        assertEquals("(hidden)", emailLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testDomainOnlyMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setEmailDisplayMode(UserDetailsDisplaySettingsInfo.EmailDisplayMode.DOMAIN_ONLY);
        gs.save(global);

        EmailLabel emailLabel = new EmailLabel("label", Model.of("test@example.com"));

        assertEquals("example.com", emailLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testMaskedMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setEmailDisplayMode(UserDetailsDisplaySettingsInfo.EmailDisplayMode.MASKED);
        gs.save(global);

        EmailLabel emailLabel = new EmailLabel("label", Model.of("test_.foo-bar@xyz.example.com"));

        assertEquals("t************@xyz.example.com", emailLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testFullMode() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setEmailDisplayMode(UserDetailsDisplaySettingsInfo.EmailDisplayMode.FULL);
        gs.save(global);

        EmailLabel emailLabel = new EmailLabel("label", Model.of("test@example.com"));

        assertEquals("test@example.com", emailLabel.getDefaultModelObjectAsString());
    }

    @Test
    public void testHiddenStyle() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        global.getUserDetailsDisplaySettings()
                .setEmailDisplayMode(UserDetailsDisplaySettingsInfo.EmailDisplayMode.HIDDEN);
        gs.save(global);

        EmailLabel emailLabel = new EmailLabel("label", Model.of("test@example.com"));

        tester.startComponentInPage(emailLabel);

        List<? extends Behavior> behaviors = emailLabel.getBehaviors();
        assertEquals(1, behaviors.size());

        Behavior behavior = behaviors.get(0);
        assertInstanceOf(AttributeModifier.class, behavior);
        assertEquals("class", ((AttributeModifier) behavior).getAttribute());
        assertTrue(behavior.toString().contains("italic"));
    }

    @Test
    public void testRevealBehavior() {

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();

        UserDetailsDisplaySettingsInfo userDetailsDisplaySettings = global.getUserDetailsDisplaySettings();
        userDetailsDisplaySettings.setEmailDisplayMode(UserDetailsDisplaySettingsInfo.EmailDisplayMode.MASKED);
        userDetailsDisplaySettings.setRevealEmailAtClick(true);
        gs.save(global);

        EmailLabel emailLabel = new EmailLabel("label", Model.of("test@example.com"));

        tester.startComponentInPage(emailLabel);

        List<? extends Behavior> behaviors = emailLabel.getBehaviors();
        assertEquals(1, behaviors.size());

        Behavior behavior = behaviors.get(0);
        assertInstanceOf(AjaxEventBehavior.class, behavior);
        assertEquals("click", ((AjaxEventBehavior) behavior).getEvent());
    }
}
