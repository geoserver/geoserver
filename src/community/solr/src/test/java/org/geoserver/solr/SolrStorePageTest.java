package org.geoserver.solr;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geotools.data.solr.SolrDataStoreFactory;
import org.junit.Ignore;
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
    @Ignore
    public void testChangeWorkspaceNamespace() throws Exception {
        startPage();

        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();

        tester.assertModelValue("dataStoreForm:workspacePanel:border:paramValue", defaultWs);

        // configure the store
        FormTester ft = tester.newFormTester("dataStoreForm");
        ft.setValue("dataStoreNamePanel:border:paramValue", "testStore");
        ft.setValue("parametersPanel:url:border:paramValue", "file://" + new File("./target").getCanonicalPath());
        ft.select("workspacePanel:border:paramValue", 2);
        ft.submit();
        tester.executeAjaxEvent("dataStoreForm:workspacePanel:border:paramValue", "onchange");
        tester.executeAjaxEvent("dataStoreForm:save", "onclick");
        
        
        // get the workspace we have just configured in the GUI
        WorkspacesModel wm = new WorkspacesModel();
        List<WorkspaceInfo> wl = (List<WorkspaceInfo>) wm.getObject();
        WorkspaceInfo ws = wl.get(2);

        // check it's the same
        StoreInfo store = getCatalog().getStoreByName("testStore", DataStoreInfo.class);
        assertEquals(getCatalog().getNamespaceByPrefix(ws.getName()).getURI(), store.getConnectionParameters().get("namespace"));
    }
}
