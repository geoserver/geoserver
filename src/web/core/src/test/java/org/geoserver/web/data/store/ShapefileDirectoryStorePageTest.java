/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geotools.data.shapefile.ShapefileDirectoryFactory;
import org.junit.Test;

/**
 * Test for the shapefile directory ppanel
 *
 * @author Andrea Aime
 */
public class ShapefileDirectoryStorePageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    private AbstractDataAccessPage startPage() {
        final String dataStoreFactoryDisplayName = new ShapefileDirectoryFactory().getDisplayName();

        login();
        final AbstractDataAccessPage page = new DataAccessNewPage(dataStoreFactoryDisplayName);
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true, true);
        }

        return page;
    }

    @Test
    public void testChangeWorkspaceNamespace() throws Exception {
        startPage();

        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();

        tester.assertModelValue(
                "dataStoreForm:workspacePanel:border:border_body:paramValue", defaultWs);

        // configure the store
        FormTester ft = tester.newFormTester("dataStoreForm");
        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "testStore");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "file://" + new File("./target").getCanonicalPath());
        ft.select("workspacePanel:border:border_body:paramValue", 2);
        tester.executeAjaxEvent(
                "dataStoreForm:workspacePanel:border:border_body:paramValue", "change");

        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "testStore");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "file://" + new File("./target").getCanonicalPath());
        ft.select("workspacePanel:border:border_body:paramValue", 2);
        ft.submit();
        tester.executeAjaxEvent("dataStoreForm:save", "click");

        // get the workspace we have just configured in the GUI
        WorkspacesModel wm = new WorkspacesModel();
        List<WorkspaceInfo> wl = (List<WorkspaceInfo>) wm.getObject();
        WorkspaceInfo ws = wl.get(2);

        // check it's the same
        StoreInfo store = getCatalog().getStoreByName("testStore", DataStoreInfo.class);
        assertEquals(
                getCatalog().getNamespaceByPrefix(ws.getName()).getURI(),
                store.getConnectionParameters().get("namespace"));
    }
}
