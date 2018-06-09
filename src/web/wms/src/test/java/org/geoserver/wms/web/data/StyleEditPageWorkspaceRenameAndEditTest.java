/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.*;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;

/** This test is extremely brittle, and doesn't play well with others */
public class StyleEditPageWorkspaceRenameAndEditTest extends GeoServerWicketTestSupport {

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

    /*
     * Test that a user can update the .sld file contents and move the style into a workspace in a single edit.
     */
    @Test
    public void testMoveWorkspaceAndEdit() throws Exception {
        // add catalog listener so we can validate the style modified event
        final boolean[] gotValidEvent = {false};
        getCatalog()
                .addListener(
                        new CatalogListener() {

                            @Override
                            public void handleAddEvent(CatalogAddEvent event)
                                    throws CatalogException {
                                // not interest, ignore this events
                            }

                            @Override
                            public void handleRemoveEvent(CatalogRemoveEvent event)
                                    throws CatalogException {
                                // not interest, ignore this events
                            }

                            @Override
                            public void handleModifyEvent(CatalogModifyEvent event)
                                    throws CatalogException {
                                // not interest, ignore this events
                            }

                            @Override
                            public void handlePostModifyEvent(CatalogPostModifyEvent event)
                                    throws CatalogException {
                                assertThat(event, notNullValue());
                                assertThat(event.getSource(), notNullValue());
                                if (!(event.getSource() instanceof StyleInfo)) {
                                    // only interested in style info events
                                    return;
                                }
                                try {
                                    // get the associated style and check that you got the correct
                                    // content
                                    StyleInfo styleInfo = (StyleInfo) event.getSource();
                                    assertThat(styleInfo, notNullValue());
                                    Style style =
                                            getCatalog().getResourcePool().getStyle(styleInfo);
                                    assertThat(style, notNullValue());
                                    assertThat(style.featureTypeStyles().size(), is(2));
                                    // ok everything looks good
                                    gotValidEvent[0] = true;
                                } catch (Exception exception) {
                                    LOGGER.log(
                                            Level.SEVERE,
                                            "Error handling catalog modified style event.",
                                            exception);
                                }
                            }

                            @Override
                            public void reloaded() {
                                // not interest, ignore this events
                            }
                        });

        edit = new StyleEditPage(styleInfoToMove);
        tester.startPage(edit);

        // Before the edit, the style should have one <FeatureTypeStyle>
        assertEquals(1, styleInfoToMove.getStyle().featureTypeStyles().size());

        FormTester form = tester.newFormTester("styleForm", false);

        // Update the workspace (select "sf" from the dropdown)
        DropDownChoice<WorkspaceInfo> typeDropDown =
                (DropDownChoice<WorkspaceInfo>)
                        tester.getComponentFromLastRenderedPage(
                                "styleForm:context:panel:workspace");

        for (int wsIdx = 0; wsIdx < typeDropDown.getChoices().size(); wsIdx++) {
            WorkspaceInfo ws = typeDropDown.getChoices().get(wsIdx);
            if ("sf".equalsIgnoreCase(ws.getName())) {
                form.select("context:panel:workspace", wsIdx);
                break;
            }
        }

        // Update the raw style contents (the new style has TWO <FeatureTypeStyle> entries).
        File styleFile = new File(getClass().getResource(STYLE_TO_MOVE_FILENAME_UPDATED).toURI());
        String updatedSld =
                IOUtils.toString(new FileReader(styleFile))
                        .replaceAll("\r\n", "\n")
                        .replaceAll("\r", "\n");
        form.setValue("styleEditor:editorContainer:editorParent:editor", updatedSld);

        // Submit the form and verify that both the new workspace and new rawStyle saved.
        form.submit();

        StyleInfo si =
                getCatalog()
                        .getStyleByName(getCatalog().getWorkspaceByName("sf"), STYLE_TO_MOVE_NAME);
        assertNotNull(si);
        assertNotNull(si.getWorkspace());
        assertEquals("sf", si.getWorkspace().getName());
        assertEquals(2, si.getStyle().featureTypeStyles().size());

        // check the correct style modified event was published
        assertThat(gotValidEvent[0], is(true));
    }
}
