package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
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
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs"))
                .setSelectedTab(4);
        tester.submitForm("publishedinfo");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", TabbedPanel.class);
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);

        GeoServerTablePanel<AttributeConfiguration> attPanel =
                (GeoServerTablePanel<AttributeConfiguration>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel");
        assertEquals(7, attPanel.getDataProvider().size());

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:1:link");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);
        attPanel =
                (GeoServerTablePanel<AttributeConfiguration>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel");
        assertEquals(3, attPanel.getDataProvider().size());

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:2:link");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);
        attPanel =
                (GeoServerTablePanel<AttributeConfiguration>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel");
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
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs"))
                .setSelectedTab(4);
        tester.submitForm("publishedinfo");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", TabbedPanel.class);
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);

        FormTester formTester = tester.newFormTester("publishedinfo");
        formTester.setValue(
                "tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:textfield",
                "new-value");

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:1:link");

        formTester.submit("save");

        logout();

        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertEquals(
                "new-value",
                ((Map<String, Object>) layer.getResource().getMetadata().get("custom"))
                        .get("extra-text"));
    }

    @Test
    public void testMultiTabField() throws IOException {

        login();
        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertNotNull(layer);
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs"))
                .setSelectedTab(4);
        tester.submitForm("publishedinfo");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", TabbedPanel.class);
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel:panel", MetadataPanel.class);

        assertEquals(
                "extra-text",
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:0:component")
                        .getDefaultModelObject());

        FormTester formTester = tester.newFormTester("publishedinfo");
        formTester.setValue(
                "tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:textfield",
                "new-value");

        tester.clickLink("publishedinfo:tabs:panel:metadataPanel:tabs-container:tabs:2:link");

        assertEquals(
                "extra-text",
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:0:component")
                        .getDefaultModelObject());
        assertEquals(
                "new-value",
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:panel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:textfield")
                        .getDefaultModelObject());
    }
}
