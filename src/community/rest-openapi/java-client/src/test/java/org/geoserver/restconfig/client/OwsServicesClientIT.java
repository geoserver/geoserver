package org.geoserver.restconfig.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.model.config.WFSInfo;
import org.geoserver.openapi.model.config.WMSInfo;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** Integration test suite for {@link SettingsClient} */
public class OwsServicesClientIT {

    public @Rule IntegrationTestSupport support = new IntegrationTestSupport();

    public @Rule ExpectedException ex = ExpectedException.none();

    private OwsServicesClient ows;

    private WorkspaceInfo ws;

    public @Before void before() {
        Assume.assumeTrue(this.support.isAlive());
        ows = this.support.client().ows();
        ws = this.support.createWorkspace(UUID.randomUUID().toString());
    }

    public @Test void changeGlobalWMS() {
        WMSInfo wms = ows.getWMSSettings();
        assertNotNull(wms);
        String rnd = UUID.randomUUID().toString();
        String name = "Global Name " + rnd;
        String title = "Global Title " + rnd;
        String abstrct = "Global Abstract " + rnd;
        wms.setName(name);
        wms.setTitle(title);
        wms.setAbstrct(abstrct);
        ows.updateWMSSettings(wms);

        WMSInfo updated = ows.getWMSSettings();
        assertNotNull(updated);
        assertEquals(name, updated.getName());
        assertEquals(title, updated.getTitle());
        assertEquals(abstrct, updated.getAbstrct());
    }

    public @Test void changeGlobalWFS() {
        WFSInfo wfs = ows.getWFSSettings();
        assertNotNull(wfs);
        String rnd = UUID.randomUUID().toString();
        String name = "Global Name " + rnd;
        String title = "Global Title " + rnd;
        String abstrct = "Global Abstract " + rnd;
        wfs.setName(name);
        wfs.setTitle(title);
        wfs.setAbstrct(abstrct);
        ows.updateWFSSettings(wfs);

        WFSInfo updated = ows.getWFSSettings();
        assertNotNull(updated);
        assertEquals(name, updated.getName());
        assertEquals(title, updated.getTitle());
        assertEquals(abstrct, updated.getAbstrct());
    }

    public @Test void changeWorkspaceWMS() {
        try {
            ows.getLocalWMSSettings(this.ws.getName());
            fail("Expected 404 when workspace wms is not enabled");
        } catch (ServerException.NotFound expected) {
            assertTrue(true);
        }
        final WMSInfo global = ows.getWMSSettings();

        // configure local wms based on global settings
        WMSInfo workspaceWMS = ows.getWMSSettings();

        String name = "Local Name";
        String title = "Local Title";
        String abstrct = "Local Abstract";

        workspaceWMS.setWorkspace(this.ws);
        workspaceWMS.setName(name);
        workspaceWMS.setTitle(title);
        workspaceWMS.setAbstrct(abstrct);

        ows.updateLocalWMSSettings(this.ws.getName(), workspaceWMS);

        WMSInfo updated = ows.getLocalWMSSettings(this.ws.getName());
        assertNotNull(updated);
        assertEquals(name, updated.getName());
        assertEquals(title, updated.getTitle());
        assertEquals(abstrct, updated.getAbstrct());

        assertEquals(global, ows.getWMSSettings());
    }

    public @Test void changeWorkspaceWFS() {
        try {
            ows.getLocalWFSSettings(this.ws.getName());
            fail("Expected 404 when workspace wms is not enabled");
        } catch (ServerException.NotFound expected) {
            assertTrue(true);
        }
        final WFSInfo global = ows.getWFSSettings();

        // configure local wfs based on global settings
        WFSInfo workspaceWFS = ows.getWFSSettings();

        String name = "Local Name";
        String title = "Local Title";
        String abstrct = "Local Abstract";

        workspaceWFS.setWorkspace(this.ws);
        workspaceWFS.setName(name);
        workspaceWFS.setTitle(title);
        workspaceWFS.setAbstrct(abstrct);

        ows.updateLocalWFSSettings(this.ws.getName(), workspaceWFS);

        WFSInfo updated = ows.getLocalWFSSettings(this.ws.getName());
        assertNotNull(updated);
        assertEquals(name, updated.getName());
        assertEquals(title, updated.getTitle());
        assertEquals(abstrct, updated.getAbstrct());

        assertEquals(global, ows.getWFSSettings());
    }
}
