package org.geoserver.restconfig.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.model.config.ContactInfo;
import org.geoserver.openapi.model.config.GeoServerInfo;
import org.geoserver.openapi.model.config.SettingsInfo;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Integration test suite for {@link SettingsClient} */
public class SettingsClientIT {

    public @Rule IntegrationTestSupport support = new IntegrationTestSupport();

    public @Rule ExpectedException ex = ExpectedException.none();

    private SettingsClient settingsClient;

    public @Before void before() {
        Assume.assumeTrue(this.support.isAlive());
        settingsClient = this.support.client().settings();
    }

    public @After void after() {}

    public @Test void changeGlobalConfig() {
        GeoServerInfo global = settingsClient.getGlobalConfig();
        assertNotNull(global);
        SettingsInfo settings = global.getSettings();
        assertNotNull(settings);

        String proxyBaseUrl = "${X-Forwarded-Proto}://${X-Forwarded-Host}${X-Forwarded-Path}";
        GeoServerInfo update = new GeoServerInfo();
        update.setUseHeadersProxyURL(true);
        SettingsInfo newSettings = new SettingsInfo();
        newSettings.setProxyBaseUrl(proxyBaseUrl);
        update.setSettings(newSettings);

        GeoServerInfo updated = settingsClient.updateGlobalConfig(update);
        assertNotNull(updated);
        assertNotSame(update, updated);
        assertTrue(updated.getUseHeadersProxyURL());
        assertNotNull(updated.getSettings());
        assertEquals(proxyBaseUrl, updated.getSettings().getProxyBaseUrl());

        update.setUseHeadersProxyURL(false);
        update.setSettings(null);

        updated = settingsClient.updateGlobalConfig(update);
        assertNotNull(updated);
        assertNotSame(update, updated);
        assertFalse(updated.getUseHeadersProxyURL());
        assertNotNull(updated.getSettings());
        assertNull(updated.getSettings().getProxyBaseUrl());
    }

    public @Test void createAndChangeWorkspaceSettings() {
        final String workspace = UUID.randomUUID().toString();
        final boolean isolated = true;
        WorkspaceInfo ws = this.support.createWorkspace(workspace, isolated);
        assertTrue(ws.getIsolated());

        // an empty object is returned when local settings have not been set
        SettingsInfo settings = settingsClient.getLocalSettings(workspace);
        assertNotNull(settings);
        assertNull(settings.getId());
        assertNull(settings.getTitle());
        assertNotNull(settings.getContact());
        assertNull(settings.getContact().getAddress());

        ContactInfo contact =
                new ContactInfo()
                        .address("address")
                        .addressCity("addressCity")
                        .addressCountry("addressCountry")
                        .addressType("addressType")
                        .contactEmail("contact@example.com");

        SettingsInfo request = new SettingsInfo();
        request.charset("ISO-8859-1")
                .contact(contact)
                .onlineResource("http://example.com")
                .title("contact title")
                .contact(contact);
        settingsClient.updateLocalSettings(workspace, request);

        settings = settingsClient.getLocalSettings(workspace);
        assertNotNull(settings);
        assertNotNull(settings.getId());
        assertEquals(request.getCharset(), settings.getCharset());
        assertEquals(request.getOnlineResource(), settings.getOnlineResource());
        assertNotSame(request.getContact(), settings.getContact());
        assertEquals(request.getContact(), settings.getContact());
    }
}
