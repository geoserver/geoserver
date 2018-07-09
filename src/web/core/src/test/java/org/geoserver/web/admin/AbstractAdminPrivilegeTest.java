/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AdminRequest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layer.NewFeatureTypePage;
import org.geoserver.web.data.layer.SQLViewNewPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractAdminPrivilegeTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Arrays.asList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Arrays.asList("ROLE_SF_ADMIN"));

        setupAccessRules();

        Catalog cat = getCatalog();

        // add two workspace local layer group
        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("cite_local");
        lg.setWorkspace(cat.getWorkspaceByName("cite"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.LAKES)));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.FORESTS)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);
        cat.add(lg);

        lg = cat.getFactory().createLayerGroup();
        lg.setName("sf_local");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.AGGREGATEGEOFEATURE)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);
        cat.add(lg);

        // add two global layer group
        lg = cat.getFactory().createLayerGroup();
        lg.setName("cite_global");
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.LAKES)));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.FORESTS)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);
        cat.add(lg);

        lg = cat.getFactory().createLayerGroup();
        lg.setName("sf_global");
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.AGGREGATEGEOFEATURE)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);
        cat.add(lg);
    }

    protected abstract void setupAccessRules() throws IOException;

    @After
    public void finishAdminRequest() {
        AdminRequest.finish();
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    @Test
    public void testWorkspaceAllPage() throws Exception {
        loginAsCite();

        tester.startPage(WorkspacePage.class);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        // assert only cite workspace visible
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());

        // the actual web request is finished, so we need to fake another one
        AdminRequest.start(new Object());
        assertEquals(1, dv.getDataProvider().size());

        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cite", ws.getName());
    }

    @Test
    public void testWorkspaceNewPage() throws Exception {
        loginAsCite();

        tester.startPage(WorkspaceNewPage.class);
        tester.assertRenderedPage(UnauthorizedPage.class);
    }

    @Test
    public void testWorkspaceEditPage() throws Exception {
        loginAsCite();

        tester.startPage(WorkspaceEditPage.class, new PageParameters().add("name", "cite"));
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testWorkspaceEditPageUnauthorized() throws Exception {
        loginAsCite();
        tester.startPage(WorkspaceEditPage.class, new PageParameters().add("name", "cdf"));
        tester.assertErrorMessages(new String[] {"Could not find workspace \"cdf\""});
    }

    @Test
    public void testLayerAllPage() throws Exception {
        loginAsCite();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
        print(tester.getLastRenderedPage(), true, true, true);

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(
                getCatalog().getResourcesByNamespace("cite", ResourceInfo.class).size(), dv.size());
    }

    @Test
    public void testStoreAllPage() throws Exception {
        loginAsCite();

        tester.startPage(StorePage.class);
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(getCatalog().getStoresByWorkspace("cite", StoreInfo.class).size(), dv.size());
    }

    @Test
    public void testStoreNewPage() throws Exception {
        loginAsCite();
        AdminRequest.start(new Object());

        final String dataStoreFactoryDisplayName = new PropertyDataStoreFactory().getDisplayName();
        tester.startPage(new DataAccessNewPage(dataStoreFactoryDisplayName));
        tester.assertRenderedPage(DataAccessNewPage.class);
        tester.assertNoErrorMessage();

        // the actual web request is finished, so we need to fake another one
        AdminRequest.start(new Object());

        DropDownChoice<WorkspaceInfo> wsChoice =
                (DropDownChoice<WorkspaceInfo>)
                        tester.getComponentFromLastRenderedPage(
                                "dataStoreForm:workspacePanel:border:border_body:paramValue");

        assertEquals(1, wsChoice.getChoices().size());
        assertEquals("cite", wsChoice.getChoices().get(0).getName());
    }

    @Test
    public void testStoreEditPage() throws Exception {
        loginAsCite();

        tester.startPage(
                DataAccessEditPage.class,
                new PageParameters().add("wsName", "cite").add("storeName", "cite"));
        tester.assertRenderedPage(DataAccessEditPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testStoreEditPageUnauthorized() throws Exception {
        loginAsCite();

        tester.startPage(
                DataAccessEditPage.class,
                new PageParameters().add("wsName", "cdf").add("storeName", "cdf"));
        tester.assertRenderedPage(StorePage.class);
        tester.assertErrorMessages(
                new String[] {"Could not find data store \"cdf\" in workspace \"cdf\""});
    }

    @Test
    public void testLayerGroupAllPageAsAdmin() throws Exception {
        login();
        tester.startPage(LayerGroupPage.class);
        tester.assertRenderedPage(LayerGroupPage.class);

        Catalog cat = getCatalog();

        DataView view =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(cat.getLayerGroups().size(), view.getItemCount());
    }

    @Test
    public void testLayerGroupAllPage() throws Exception {
        loginAsCite();

        tester.startPage(LayerGroupPage.class);
        tester.assertRenderedPage(LayerGroupPage.class);

        Catalog cat = getCatalog();

        DataView view =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        AdminRequest.start(new Object());
        assertEquals(cat.getLayerGroups().size(), view.getItemCount());

        for (Iterator<Item> it = view.getItems(); it.hasNext(); ) {
            String name =
                    it.next()
                            .get("itemProperties:0:component:link:label")
                            .getDefaultModelObjectAsString();
            assertFalse("sf_local".equals(name));
        }
    }

    @Test
    public void testLayerGroupEditPageAsAdmin() throws Exception {
        login();

        tester.startPage(LayerGroupEditPage.class);
        tester.assertRenderedPage(LayerGroupEditPage.class);
        tester.assertModelValue("publishedinfo:tabs:panel:workspace", null);

        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:workspace");
        assertTrue(choice.isNullValid());
        assertFalse(choice.isRequired());
    }

    @Test
    public void testLayerGroupEditPage() throws Exception {
        loginAsCite();

        tester.startPage(LayerGroupEditPage.class);
        tester.assertRenderedPage(LayerGroupEditPage.class);

        Catalog cat = getCatalog();
        tester.assertModelValue(
                "publishedinfo:tabs:panel:workspace", cat.getWorkspaceByName("cite"));

        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:workspace");
        assertFalse(choice.isNullValid());
        assertTrue(choice.isRequired());
    }

    @Test
    public void testLayerGroupEditPageGlobal() throws Exception {
        loginAsCite();

        tester.startPage(
                LayerGroupEditPage.class,
                new PageParameters().add(LayerGroupEditPage.GROUP, "cite_global"));
        tester.assertRenderedPage(LayerGroupEditPage.class);

        // assert all form components disabled except for cancel
        assertFalse(
                tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:name")
                        .isEnabled());
        assertFalse(
                tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:workspace")
                        .isEnabled());
        assertNull(tester.getComponentFromLastRenderedPage("publishedinfo:save"));
        assertTrue(tester.getComponentFromLastRenderedPage("publishedinfo:cancel").isEnabled());
    }

    public void testSqlViewNewPageAsWorkspaceAdmin() throws Exception {
        loginAsCite();

        PageParameters pp = new PageParameters();
        pp.add(SQLViewNewPage.WORKSPACE, "cite");

        // not a jdbc datastore obviously but we don't need one to simply test that the
        // page will render with worksapce admin privilieges
        pp.add(SQLViewNewPage.DATASTORE, "cite");

        new SQLViewNewPage(pp);

        RequestCycle cycle = RequestCycle.get();
        RenderPageRequestHandler handler =
                (RenderPageRequestHandler) cycle.getRequestHandlerScheduledAfterCurrent();
        assertFalse(UnauthorizedPage.class.equals(handler.getPageClass()));
    }

    public void testCreateNewFeatureTypePageAsWorkspaceAdmin() throws Exception {
        loginAsCite();

        PageParameters pp = new PageParameters();
        pp.add(NewFeatureTypePage.WORKSPACE, "cite");

        // not a jdbc datastore obviously but we don't need one to simply test that the
        // page will render with worksapce admin privilieges
        pp.add(NewFeatureTypePage.DATASTORE, "cite");

        new NewFeatureTypePage(pp);

        RequestCycle cycle = RequestCycle.get();
        RenderPageRequestHandler handler =
                (RenderPageRequestHandler) cycle.getRequestHandlerScheduledAfterCurrent();
        assertFalse(UnauthorizedPage.class.equals(handler.getPageClass()));
    }
}
