package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.util.file.File;
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
        assertEquals(6, attPanel.getDataProvider().size());

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
        assertEquals(4, attPanel.getDataProvider().size());

        logout();
    }
}
