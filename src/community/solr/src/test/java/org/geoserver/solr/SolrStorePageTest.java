/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.List;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geotools.data.solr.SolrDataStoreFactory;
import org.junit.Test;

public class SolrStorePageTest extends GeoServerWicketTestSupport {

    private DataAccessNewPage startPage() {
        final String dataStoreFactoryDisplayName = new SolrDataStoreFactory().getDisplayName();
        final DataAccessNewPage page = new DataAccessNewPage(dataStoreFactoryDisplayName);
        login();
        tester.startPage(page);
        return page;
    }

    @Test
    public void testDeprecatedParamsHidden() throws Exception {
        startPage();

        // print(tester.getLastRenderedPage(), true, true);

        // check the deprecated fields are not visible
        MarkupContainer container =
                (MarkupContainer)
                        tester.getComponentFromLastRenderedPage(
                                "dataStoreForm:parametersPanel:parameters:1");
        assertEquals("layer_mapper", container.getDefaultModelObject());
        assertFalse(container.get("parameterPanel").isVisible());
        container =
                (MarkupContainer)
                        tester.getComponentFromLastRenderedPage(
                                "dataStoreForm:parametersPanel:parameters:2");
        assertEquals("layer_name_field", container.getDefaultModelObject());
        assertFalse(container.get("parameterPanel").isVisible());
    }

    @Test
    public void testChangeWorkspaceNamespace() throws Exception {
        startPage();

        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();

        tester.assertModelValue(
                "dataStoreForm:workspacePanel:border:border_body:paramValue", defaultWs);
        // print(tester.getLastRenderedPage(), true, true);

        // configure the store
        FormTester ft = tester.newFormTester("dataStoreForm", false);
        ft.select("workspacePanel:border:border_body:paramValue", 2);
        tester.executeAjaxEvent(
                "dataStoreForm:workspacePanel:border:border_body:paramValue", "change");

        ft.setValue("dataStoreNamePanel:border:border_body:paramValue", "testStore");
        ft.setValue(
                "parametersPanel:parameters:0:parameterPanel:border:border_body:paramValue",
                "file://" + new File("./target").getCanonicalPath());
        ft.select("workspacePanel:border:border_body:paramValue", 2);
        ft.submit("save");
        tester.assertNoFeedbackMessage(FeedbackMessage.ERROR);

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
