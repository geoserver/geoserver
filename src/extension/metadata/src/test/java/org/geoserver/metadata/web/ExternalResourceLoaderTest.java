/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Locale;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.file.File;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.metadata.web.resource.WicketResourceResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the ExternalResourceLoader.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class ExternalResourceLoaderTest extends AbstractWicketMetadataTest {

    private Locale originalLocale;

    @Before
    public void before() throws IOException {
        login();

        originalLocale = Session.get().getLocale();

        // Load the page
        MetadataTemplatesPage page = new MetadataTemplatesPage();
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatesPage.class);
    }

    @After
    public void after() throws Exception {
        logout();

        Session.get().setLocale(originalLocale);
    }

    @Test
    public void testExternalResourceLoader() throws IOException {
        File metadata = new File(DATA_DIRECTORY.getDataDirectoryRoot(), "metadata");
        WicketResourceResourceLoader loader =
                new WicketResourceResourceLoader(Files.asResource(metadata), "metadata.properties");

        String actual =
                loader.loadStringResource(
                        tester.getLastRenderedPage(), "metadata.generated.form.identifier-single");
        Assert.assertEquals("identifier single field", actual);

        Session.get().setLocale(new Locale("nl"));
        actual =
                loader.loadStringResource(
                        tester.getLastRenderedPage(), "metadata.generated.form.identifier-single");
        Assert.assertEquals("identifier single field", actual);
    }

    @Test
    public void testExternalResourceLoaderDutch() throws IOException {
        Session.get().setLocale(new Locale("nl"));
        File metadata = new File(DATA_DIRECTORY.getDataDirectoryRoot(), "metadata");
        WicketResourceResourceLoader loader =
                new WicketResourceResourceLoader(Files.asResource(metadata), "metadata.properties");

        String actual =
                loader.loadStringResource(
                        tester.getLastRenderedPage(), "metadata.generated.form.number-field");
        Assert.assertEquals("Getal veld", actual);
    }

    @Test
    public void testLocalizationLabels() {
        Session.get().setLocale(new Locale("nl"));

        LayerInfo layer = geoServer.getCatalog().getLayerByName("mylayer");
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs"))
                .setSelectedTab(4);
        tester.submitForm("publishedinfo");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", MetadataPanel.class);

        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:0:component",
                "identifier single field");
        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:3:itemProperties:0:component",
                "Getal veld");
        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:0:component",
                "the refsystem as list field");
        tester.assertLabel(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:6:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component",
                "Het code veld");

        @SuppressWarnings("unchecked")
        DropDownChoice<String> choice =
                (DropDownChoice<String>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:4:itemProperties:1:component:dropdown");
        assertEquals(
                "The Final Choice", choice.getChoiceRenderer().getDisplayValue("the-final-choice"));
    }
}
