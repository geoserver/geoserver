/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.data.StyleEditPage;
import org.geoserver.wms.web.data.StyleNewPage;
import org.geoserver.wms.web.data.StylePage;
import org.junit.Test;

public class AdminPrivilegesTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Arrays.asList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Arrays.asList("ROLE_SF_ADMIN"));

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");
        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_CITE_ADMIN");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_SF_ADMIN");

        Catalog cat = getCatalog();

        // add two workspace specific styles
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(cat.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        cat.add(s);
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    @Test
    public void testStyleAllPageAsAdmin() throws Exception {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        tester.debugComponentTrees();
        Catalog cat = getCatalog();

        DataView view =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(cat.getStyles().size(), view.getItemCount());
    }

    @Test
    public void testStyleAllPage() throws Exception {
        loginAsCite();

        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);

        Catalog cat = getCatalog();

        DataView view =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        // logged in as CITE, will only see styles in this workspace
        int expected = 1;

        AdminRequest.start(new Object());
        assertEquals(expected, view.getItemCount());

        for (Iterator<Item> it = view.getItems(); it.hasNext(); ) {
            String name =
                    it.next()
                            .get("itemProperties:0:component:link:label")
                            .getDefaultModelObjectAsString();
            assertFalse("sf_style".equals(name));
        }
    }

    @Test
    public void testStyleNewPageAsAdmin() throws Exception {
        login();

        tester.startPage(StyleNewPage.class);
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertModelValue("styleForm:context:panel:workspace", null);

        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "styleForm:context:panel:workspace");
        assertTrue(choice.isNullValid());
        assertFalse(choice.isRequired());
    }

    @Test
    public void testStyleNewPage() throws Exception {
        loginAsCite();

        tester.startPage(StyleNewPage.class);
        tester.assertRenderedPage(StyleNewPage.class);

        Catalog cat = getCatalog();
        tester.assertModelValue(
                "styleForm:context:panel:workspace", cat.getWorkspaceByName("cite"));

        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "styleForm:context:panel:workspace");
        assertFalse(choice.isNullValid());
        assertTrue(choice.isRequired());
    }

    @Test
    public void testStyleEditPageGlobal() throws Exception {
        loginAsCite();

        tester.startPage(
                StyleEditPage.class, new PageParameters().add(StyleEditPage.NAME, "point"));
        tester.assertRenderedPage(StyleEditPage.class);

        // assert all form components disabled except for cancel
        assertFalse(
                tester.getComponentFromLastRenderedPage("styleForm:context:panel:name")
                        .isEnabled());
        assertFalse(
                tester.getComponentFromLastRenderedPage("styleForm:context:panel:workspace")
                        .isEnabled());
        assertFalse(
                tester.getComponentFromLastRenderedPage("styleForm:context:panel:copy")
                        .isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("cancel").isEnabled());
    }
}
