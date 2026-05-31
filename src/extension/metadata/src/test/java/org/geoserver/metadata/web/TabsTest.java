package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.AbstractMetadataTest;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.util.IOUtils;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TabsTest extends AbstractWicketMetadataTest {

    private static final String ATTRIBUTES_TABLE =
            "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel";

    @BeforeClass
    public static void configureTabs() throws Exception {
        IOUtils.copy(
                AbstractMetadataTest.class.getResourceAsStream("metadata-tabs.yaml"),
                new File(metadata, "metadata-tabs.yaml"));
    }

    @AfterClass
    public static void unconfigureTabs() throws Exception {
        new File(metadata, "metadata-tabs.yaml").delete();
    }

    private LayerInfo layer;

    @Test
    @SuppressWarnings("unchecked")
    public void testTabs() throws IOException {

        login();
        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertNotNull(layer);
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        navigateToMetadataTab();

        GeoServerTablePanel<AttributeConfiguration> attPanel =
                (GeoServerTablePanel<AttributeConfiguration>) tester.getComponentFromLastRenderedPage(ATTRIBUTES_TABLE);
        assertEquals(7, attPanel.getDataProvider().size());

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:1:link");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);
        attPanel =
                (GeoServerTablePanel<AttributeConfiguration>) tester.getComponentFromLastRenderedPage(ATTRIBUTES_TABLE);
        assertEquals(3, attPanel.getDataProvider().size());

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:2:link");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);
        attPanel =
                (GeoServerTablePanel<AttributeConfiguration>) tester.getComponentFromLastRenderedPage(ATTRIBUTES_TABLE);
        assertEquals(5, attPanel.getDataProvider().size());

        logout();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSave() throws IOException {

        login();
        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertNotNull(layer);
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        navigateToMetadataTab();

        FormTester formTester = tester.newFormTester("publishedinfo");
        formTester.setValue(formRowPath("extra-text") + ":itemProperties:1:component:textfield", "new-value");

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:1:link");

        formTester.submit("save");

        logout();

        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertEquals(
                "new-value",
                ((Map<String, Object>) layer.getResource().getMetadata().get("custom")).get("extra-text"));
    }

    @Test
    public void testMultiTabField() throws IOException {

        login();
        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertNotNull(layer);
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        navigateToMetadataTab();

        assertEquals(
                "extra-text",
                tester.getComponentFromLastRenderedPage(rowPath("extra-text") + ":itemProperties:0:component")
                        .getDefaultModelObject());

        FormTester formTester = tester.newFormTester("publishedinfo");
        formTester.setValue(formRowPath("extra-text") + ":itemProperties:1:component:textfield", "new-value");

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:2:link");

        assertEquals(
                "extra-text",
                tester.getComponentFromLastRenderedPage(rowPath("extra-text") + ":itemProperties:0:component")
                        .getDefaultModelObject());
        assertEquals(
                "new-value",
                tester.getComponentFromLastRenderedPage(rowPath("extra-text") + ":itemProperties:1:component:textfield")
                        .getDefaultModelObject());
    }

    /**
     * Returns the page relative path of the table row showing the given attribute. The row is found by attribute key,
     * so the test does not depend on how Wicket numbers the repeater items.
     */
    private String rowPath(String attributeKey) {
        MarkupContainer items =
                (MarkupContainer) tester.getComponentFromLastRenderedPage(ATTRIBUTES_TABLE + ":listContainer:items");
        for (Component item : items) {
            Object model = item.getDefaultModelObject();
            if (model instanceof AttributeConfiguration
                    && attributeKey.equals(((AttributeConfiguration) model).getKey())) {
                return item.getPageRelativePath();
            }
        }
        throw new AssertionError("No table row found for attribute " + attributeKey);
    }

    /** Same as {@link #rowPath}, but relative to the publishedinfo form. */
    private String formRowPath(String attributeKey) {
        return rowPath(attributeKey).substring("publishedinfo:".length());
    }
}
