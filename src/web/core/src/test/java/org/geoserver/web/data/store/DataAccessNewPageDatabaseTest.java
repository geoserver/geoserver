/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.wicket.MarkupContainer;
import org.geoserver.security.AdminRequest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.junit.After;
import org.junit.Test;

/** Test suite for {@link DataAccessNewPage}, using {@link PostgisNGDataStoreFactory} */
public class DataAccessNewPageDatabaseTest extends GeoServerWicketTestSupport {
    final JDBCDataStoreFactory dataStoreFactory = new PostgisNGDataStoreFactory();

    private AbstractDataAccessPage startPage() {
        AdminRequest.start(new Object());
        login();
        final AbstractDataAccessPage page =
                new DataAccessNewPage(dataStoreFactory.getDisplayName());
        tester.startPage(page);

        return page;
    }

    @After
    public void clearAdminRequest() {
        AdminRequest.finish();
    }

    @Test
    public void testPageRendersOnLoad() {
        startPage();

        tester.assertLabel("dataStoreForm:storeType", dataStoreFactory.getDisplayName());
        tester.assertLabel("dataStoreForm:storeTypeDescription", dataStoreFactory.getDescription());

        tester.assertComponent("dataStoreForm:workspacePanel", WorkspacePanel.class);
    }

    @Test
    public void testDbtypeParameterHidden() {
        startPage();

        // check the dbtype field is not visible
        MarkupContainer container =
                (MarkupContainer)
                        tester.getComponentFromLastRenderedPage(
                                "dataStoreForm:parametersPanel:parameters:0");
        assertEquals("dbtype", container.getDefaultModelObject());
        assertFalse(container.get("parameterPanel").isVisible());
    }
}
