/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WorkspacePageTest extends GeoServerWicketTestSupport {

    @Before
    public void init() {
        login();
        tester.startPage(WorkspacePage.class);

        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), getCatalog().getWorkspaces().size());
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cdf", ws.getName());
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);
        login();

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        // Ensure the data provider is an instance of WorkspaceProvider
        assertTrue(dv.getDataProvider() instanceof WorkspaceProvider);

        // Cast to WorkspaceProvider
        WorkspaceProvider provider = (WorkspaceProvider) dv.getDataProvider();

        // should show both columns
        assertTrue(provider.getProperties().contains(WorkspaceProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(WorkspaceProvider.MODIFIED_TIMESTAMP));
    }
}
