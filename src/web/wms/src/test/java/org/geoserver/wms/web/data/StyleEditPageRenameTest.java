/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.*;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.*;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

/** These tests are quite brittle, and don't play well with others */
public class StyleEditPageRenameTest extends GeoServerWicketTestSupport {

    StyleInfo buildingsStyle;
    StyleEditPage edit;

    private static final String STYLE_TO_MOVE_NAME = "testStyle";
    private static final String STYLE_TO_MOVE_FILENAME = "testMoveStyle.sld";
    private static final String STYLE_TO_MOVE_FILENAME_UPDATED = "testMoveStyleUpdated.sld";
    StyleInfo styleInfoToMove;

    @Before
    public void setUp() throws Exception {
        Catalog catalog = getCatalog();
        login();

        buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        if (buildingsStyle == null) {
            // undo the rename performed in one of the test methods
            StyleInfo si = catalog.getStyleByName("BuildingsNew");
            if (si != null) {
                si.setName(MockData.BUILDINGS.getLocalPart());
                catalog.save(si);
            }
            buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        }

        edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
        styleInfoToMove = catalog.getStyleByName("testStyle");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle(
                STYLE_TO_MOVE_NAME, STYLE_TO_MOVE_FILENAME, this.getClass(), getCatalog());
    }

    // Test that a user can non-destructively move the style out of a workspace.
    @Test
    public void testMoveFromWorkspace() throws Exception {
        // Move into sf
        Catalog catalog = getCatalog();
        StyleInfo si = catalog.getStyleByName(STYLE_TO_MOVE_NAME);
        si.setWorkspace(catalog.getWorkspaceByName("sf"));
        catalog.save(si);

        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        // verify move to workspace was successful
        assertEquals(
                Resource.Type.UNDEFINED, dataDir.get("styles/" + STYLE_TO_MOVE_FILENAME).getType());
        assertEquals(
                Resource.Type.RESOURCE,
                dataDir.get("workspaces/sf/styles/" + STYLE_TO_MOVE_FILENAME).getType());

        // test moving back to default workspace using the UI
        edit = new StyleEditPage(si);
        tester.startPage(edit);

        // Before the edit, the style should have one <FeatureTypeStyle> and be in the sf workspace
        assertEquals(1, si.getStyle().featureTypeStyles().size());
        assertEquals("sf", si.getWorkspace().getName());

        FormTester form = tester.newFormTester("styleForm", false);

        // Update the workspace (select "sf" from the dropdown)
        DropDownChoice<WorkspaceInfo> typeDropDown =
                (DropDownChoice<WorkspaceInfo>)
                        tester.getComponentFromLastRenderedPage(
                                "styleForm:context:panel:workspace");

        form.setValue("context:panel:workspace", "");

        // Submit the form and verify that both the new workspace and new rawStyle saved.
        form.submit();

        si = getCatalog().getStyleByName(STYLE_TO_MOVE_NAME);
        assertNotNull(si);
        assertNull(si.getWorkspace());

        // verify move out of the workspace was successful
        assertEquals(
                Resource.Type.RESOURCE, dataDir.get("styles/" + STYLE_TO_MOVE_FILENAME).getType());
        assertEquals(
                Resource.Type.UNDEFINED,
                dataDir.get("workspaces/sf/styles/" + STYLE_TO_MOVE_FILENAME).getType());
    }

    @Test
    public void testGenerateTemplateFrenchLocale() throws Exception {
        final Session session = tester.getSession();
        try {
            session.clear();
            session.setLocale(Locale.FRENCH);

            StyleEditPage edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            // print(tester.getLastRenderedPage(), true, true);

            // test the copy style link
            tester.newFormTester("styleForm").select("context:panel:templates", 1);
            tester.executeAjaxEvent("styleForm:context:panel:templates", "onchange");
            Component generateLink =
                    tester.getComponentFromLastRenderedPage("styleForm:context:panel:generate");
            tester.executeAjaxEvent(generateLink, "onClick");
            // check single quote in the message has been escaped
            assertTrue(tester.getLastResponseAsString().contains("l\\'éditeur"));
        } finally {
            session.clear();
            session.setLocale(Locale.getDefault());
        }
    }

    @Test
    public void testCopyStyleFrenchLocale() throws Exception {
        final Session session = tester.getSession();
        try {
            session.clear();
            session.setLocale(Locale.FRENCH);

            StyleEditPage edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            // print(tester.getLastRenderedPage(), true, true);

            // test the copy style link
            tester.newFormTester("styleForm").select("context:panel:existingStyles", 1);
            tester.executeAjaxEvent("styleForm:context:panel:existingStyles", "onchange");
            Component copyLink =
                    tester.getComponentFromLastRenderedPage("styleForm:context:panel:copy");
            tester.executeAjaxEvent(copyLink, "onClick");
            // check single quote in the message has been escaped
            assertTrue(tester.getLastResponseAsString().contains("l\\'éditeur"));
        } finally {
            session.clear();
            session.setLocale(Locale.getDefault());
        }
    }
}
