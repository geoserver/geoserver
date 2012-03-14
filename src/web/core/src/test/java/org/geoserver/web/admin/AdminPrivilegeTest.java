package org.geoserver.web.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AdminRequest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.layergroup.LayerGroupNewPage;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.data.workspace.WorkspaceNewPage;
import org.geoserver.web.data.workspace.WorkspacePage;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public class AdminPrivilegeTest extends GeoServerWicketTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);

        //add a new user with only admin privileges to a single workspace
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();
        
        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,ROLE_CITE_ADMIN");
        props.put("sf", "sf,ROLE_SF_ADMIN");
        props.store(new FileOutputStream(users), "");

        File layers = new File(security, "layers.properties");
        props.put("*.*.r", "*");
        props.put("*.*.w", "*");
        props.put("*.*.a", "ROLE_ADMINISTRATOR");
        props.put("cite.*.a", "ROLE_CITE_ADMIN");
        props.put("sf.*.a", "ROLE_SF_ADMIN");
        props.store(new FileOutputStream(layers), "");
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        Catalog cat = getCatalog();

        //add two workspace local layer group
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

    @Override
    protected void tearDownInternal() throws Exception {
        AdminRequest.finish();
        super.tearDownInternal();
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    public void testWorkspaceAllPage() throws Exception {
        loginAsCite();

        tester.startPage(WorkspacePage.class);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        //assert only cite workspace visible
        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());

        //the actual web request is finished, so we need to fake another one
        AdminRequest.start(new Object());
        assertEquals(1, dv.getDataProvider().size());
        
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cite", ws.getName());
    }

    public void testWorkspaceNewPage() throws Exception {
        loginAsCite();

        tester.startPage(WorkspaceNewPage.class);
        tester.assertRenderedPage(UnauthorizedPage.class);
    }

    public void testWorkspaceEditPage() throws Exception {
        loginAsCite();

        tester.startPage(WorkspaceEditPage.class,new PageParameters("name=cite"));
        tester.assertRenderedPage(WorkspaceEditPage.class);
        tester.assertNoErrorMessage();
    }

    public void testWorkspaceEditPageUnauthorized() throws Exception {
        loginAsCite();
        tester.startPage(WorkspaceEditPage.class,new PageParameters("name=cdf"));
        tester.assertErrorMessages(new String[]{"Could not find workspace \"cdf\""});
    }

    public void testLayerAllPage() throws Exception {
        loginAsCite();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);

        DataView dv = 
            (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(getCatalog().getResourcesByNamespace("cite", ResourceInfo.class).size(), dv.size());
    }

    public void testStoreAllPage() throws Exception {
        loginAsCite();

        tester.startPage(StorePage.class);
        tester.assertRenderedPage(StorePage.class);
        tester.assertNoErrorMessage();
        
        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(getCatalog().getStoresByWorkspace("cite", StoreInfo.class).size(), dv.size());
    }

    public void testStoreNewPage() throws Exception {
        loginAsCite();

        final String dataStoreFactoryDisplayName = new PropertyDataStoreFactory().getDisplayName();
        tester.startPage(new DataAccessNewPage(dataStoreFactoryDisplayName));
        tester.assertRenderedPage(DataAccessNewPage.class);
        tester.assertNoErrorMessage();

        //the actual web request is finished, so we need to fake another one
        AdminRequest.start(new Object());

        DropDownChoice<WorkspaceInfo> wsChoice = (DropDownChoice<WorkspaceInfo>) 
            tester.getComponentFromLastRenderedPage("dataStoreForm:workspacePanel:border:paramValue");

        assertEquals(1, wsChoice.getChoices().size());
        assertEquals("cite", wsChoice.getChoices().get(0).getName());
    }

    public void testStoreEditPage() throws Exception {
        loginAsCite();
        
        tester.startPage(DataAccessEditPage.class, new PageParameters("wsName=cite,storeName=cite"));
        tester.assertRenderedPage(DataAccessEditPage.class);
        tester.assertNoErrorMessage();
    }

    public void testStoreEditPageUnauthorized() throws Exception {
        loginAsCite();
        
        tester.startPage(DataAccessEditPage.class, new PageParameters("wsName=cdf,storeName=cdf"));
        tester.assertRenderedPage(StorePage.class);
        tester.assertErrorMessages(new String[]{"Could not find data store \"cdf\" in workspace \"cdf\""});
    }

    public void testLayerGroupAllPageAsAdmin() throws Exception {
        login();
        tester.startPage(LayerGroupPage.class);
        tester.assertRenderedPage(LayerGroupPage.class);

        Catalog cat = getCatalog();

        DataView view = 
            (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(cat.getLayerGroups().size(), view.getItemCount());
    }

    public void testLayerGroupAllPage() throws Exception {
        loginAsCite();

        tester.startPage(LayerGroupPage.class);
        tester.assertRenderedPage(LayerGroupPage.class);
        
        Catalog cat = getCatalog();

        DataView view = 
            (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        AdminRequest.start(new Object());
        assertEquals(2, view.getItemCount());

        for (Iterator<Item> it = view.getItems(); it.hasNext();) {
            String name = it.next().get("itemProperties:0:component:link:label")
                .getDefaultModelObjectAsString();
            assertFalse("sf_local".equals(name));
        }
    }

    public void testLayerGroupNewPageAsAdmin() throws Exception {
        login();

        tester.startPage(LayerGroupNewPage.class);
        tester.assertRenderedPage(LayerGroupNewPage.class);
        tester.assertModelValue("form:workspace", null);
        
        DropDownChoice choice = 
            (DropDownChoice) tester.getComponentFromLastRenderedPage("form:workspace");
        assertTrue(choice.isNullValid());
        assertFalse(choice.isRequired());
    }

    public void testLayerGroupNewPage() throws Exception {
        loginAsCite();

        tester.startPage(LayerGroupNewPage.class);
        tester.assertRenderedPage(LayerGroupNewPage.class);

        Catalog cat = getCatalog();
        tester.assertModelValue("form:workspace", cat.getWorkspaceByName("cite"));
        
        DropDownChoice choice = 
            (DropDownChoice) tester.getComponentFromLastRenderedPage("form:workspace");
        assertFalse(choice.isNullValid());
        assertTrue(choice.isRequired());
    }

    public void testLayerGroupEditPageGlobal() throws Exception {
        loginAsCite();

        tester.startPage(LayerGroupEditPage.class, new PageParameters(LayerGroupEditPage.GROUP+"=cite_global"));
        tester.assertRenderedPage(LayerGroupEditPage.class);

        //assert all form components disabled except for cancel
        assertFalse(tester.getComponentFromLastRenderedPage("form:name").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:workspace").isEnabled());
        assertNull(tester.getComponentFromLastRenderedPage("form:save"));
        assertTrue(tester.getComponentFromLastRenderedPage("form:cancel").isEnabled());
    }
}
