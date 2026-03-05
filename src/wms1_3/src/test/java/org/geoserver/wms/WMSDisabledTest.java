/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.Version;
import org.junit.Test;
import org.w3c.dom.Document;

public class WMSDisabledTest extends WMSTestSupport {

    /** Tests that a disabled version returns an exception */
    @Test
    public void testDisabledVersionReturnsException() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        wms.getDisabledVersions().clear();
        wms.getDisabledVersions().add(new Version("1.3.0"));
        getGeoServer().save(wms);

        // request disabled version -> should fail
        Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());

        wms.getDisabledVersions().clear();
        getGeoServer().save(wms);
    }

    /** Tests that an enabled version still works when another version is disabled */
    @Test
    public void testEnabledVersionStillWorks() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        wms.getDisabledVersions().clear();
        wms.getDisabledVersions().add(new Version("1.1.1"));
        getGeoServer().save(wms);

        // request enabled version -> should succeed
        Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());

        wms.getDisabledVersions().clear();
        getGeoServer().save(wms);
    }

    /**
     * Tests that explicitly requesting a version disabled at the workspace level returns an exception, even when that
     * version is enabled globally.
     */
    @Test
    public void testWorkspaceDisabledVersionReturnsException() throws Exception {
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("sf");
        WMSInfo wsWms = createWorkspaceWmsOverride(ws, new Version("1.3.0"));
        getGeoServer().add(wsWms);

        try {
            Document doc = getAsDOM("sf/wms?service=WMS&version=1.3.0&request=GetCapabilities");
            assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
        } finally {
            getGeoServer().remove(wsWms);
        }
    }

    /**
     * Tests that version negotiation (no VERSION parameter) on a workspace endpoint selects the highest version that is
     * enabled in that workspace, ignoring globally enabled versions that are disabled in the workspace.
     */
    @Test
    public void testWorkspaceVersionNegotiationSkipsDisabledVersion() throws Exception {
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("sf");
        WMSInfo wsWms = createWorkspaceWmsOverride(ws, new Version("1.3.0"));
        getGeoServer().add(wsWms);

        try {
            // no VERSION -> negotiates down to 1.1.1 (highest enabled in this workspace)
            Document doc = getAsDOM("sf/wms?service=WMS&request=GetCapabilities");
            assertEquals("WMT_MS_Capabilities", doc.getDocumentElement().getNodeName());
            assertEquals("1.1.1", doc.getDocumentElement().getAttribute("version"));
        } finally {
            getGeoServer().remove(wsWms);
        }
    }

    /** Tests that disabling a version in a workspace override does not affect the global service endpoint. */
    @Test
    public void testWorkspaceDisabledVersionDoesNotAffectGlobal() throws Exception {
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("sf");
        WMSInfo wsWms = createWorkspaceWmsOverride(ws, new Version("1.3.0"));
        getGeoServer().add(wsWms);

        try {
            // global endpoint must still serve 1.3.0
            Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
            assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        } finally {
            getGeoServer().remove(wsWms);
        }
    }

    /**
     * Tests that a workspace-level service override with an empty disabled-versions list serves versions that are
     * disabled globally, i.e. the workspace override takes full precedence over the global setting.
     */
    @Test
    public void testGlobalDisabledVersionDoesNotAffectWorkspaceOverride() throws Exception {
        WMSInfo global = getGeoServer().getService(WMSInfo.class);
        global.getDisabledVersions().clear();
        global.getDisabledVersions().add(new Version("1.3.0"));
        getGeoServer().save(global);

        // workspace override with 1.3.0 enabled (empty disabled list)
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("sf");
        WMSInfo wsWms = createWorkspaceWmsOverride(ws);
        getGeoServer().add(wsWms);

        try {
            Document doc = getAsDOM("sf/wms?service=WMS&version=1.3.0&request=GetCapabilities");
            assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        } finally {
            getGeoServer().remove(wsWms);
            global.getDisabledVersions().clear();
            getGeoServer().save(global);
        }
    }

    /** Creates a workspace-scoped WMSInfo override based on the global service, with the given versions disabled. */
    private WMSInfo createWorkspaceWmsOverride(WorkspaceInfo ws, Version... disabledVersions) {
        WMSInfoImpl wsWms = new WMSInfoImpl();
        OwsUtils.copy(getGeoServer().getService(WMSInfo.class), wsWms, WMSInfo.class);
        wsWms.setWorkspace(ws);
        wsWms.setDisabledVersions(new ArrayList<>(Arrays.asList(disabledVersions)));
        return wsWms;
    }
}
