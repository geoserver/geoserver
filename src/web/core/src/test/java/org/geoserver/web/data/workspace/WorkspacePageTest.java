package org.geoserver.web.data.workspace;

import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;

public class WorkspacePageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        login();
        tester.startPage(WorkspacePage.class);
        
        // print(tester.getLastRenderedPage(), true, true);
    }
    
    public void testLoad() {
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        
        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), getCatalog().getWorkspaces().size());
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cdf", ws.getName());
    }
}
