/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import java.io.IOException;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.web.panel.LinkedLayersPanel;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test template page.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class TemplatePageTest extends AbstractWicketMetadataTest {

    @Before
    public void before() throws IOException {
        login();
        // Load the page
        MetadataTemplate allData = templateService.findByName("allData");
        MetadataTemplatePage page =
                new MetadataTemplatePage(
                        new ListModel<>(templateService.list()), new Model<>(allData));
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatePage.class);
    }

    @After
    public void after() throws IOException {
        logout();
        restoreTemplates();
    }

    @Test
    public void testPage() {
        // print(tester.getLastRenderedPage(), true, true);

        tester.assertModelValue("form:name", "allData");
        tester.assertModelValue("form:description", "All fields");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPageNewSave() throws IOException {
        MetadataTemplatePage page =
                new MetadataTemplatePage(new ListModel<>(templateService.list()));
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatePage.class);

        ((IModel<String>) tester.getComponentFromLastRenderedPage("form:name").getDefaultModel())
                .setObject("newtemplate");
        ((IModel<String>)
                        tester.getComponentFromLastRenderedPage("form:description")
                                .getDefaultModel())
                .setObject("description");
        ((IModel<String>)
                        tester.getComponentFromLastRenderedPage(
                                        "form:metadataTabs:panel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield")
                                .getDefaultModel())
                .setObject("new identifier value");

        tester.clickLink("form:save");

        MetadataTemplate template = templateService.findByName("newtemplate");
        Assert.assertEquals("description", template.getDescription());
        Assert.assertEquals(
                "new identifier value", template.getMetadata().get("identifier-single"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPageSave() throws IOException {
        tester.assertModelValue("form:description", "All fields");

        ((IModel<String>)
                        tester.getComponentFromLastRenderedPage("form:description")
                                .getDefaultModel())
                .setObject("description update");
        ((IModel<String>)
                        tester.getComponentFromLastRenderedPage(
                                        "form:metadataTabs:panel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield")
                                .getDefaultModel())
                .setObject("new identifier value");

        tester.assertModelValue("form:description", "description update");

        tester.clickLink("form:save");

        MetadataTemplate template = templateService.findByName("allData");
        Assert.assertEquals("description update", template.getDescription());
        Assert.assertEquals(
                "new identifier value", template.getMetadata().get("identifier-single"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPageCancel() throws IOException {
        tester.assertModelValue("form:description", "All fields");

        ((IModel<String>)
                        tester.getComponentFromLastRenderedPage("form:description")
                                .getDefaultModel())
                .setObject("description update");

        tester.assertModelValue("form:description", "description update");

        tester.clickLink("form:cancel");

        MetadataTemplate template = templateService.findByName("allData");
        Assert.assertEquals("All fields", template.getDescription());
    }

    @Test
    public void testPageValidation() throws IOException {
        // Load the page
        MetadataTemplatePage page =
                new MetadataTemplatePage(new ListModel<>(templateService.list()));
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatePage.class);

        tester.assertModelValue("form:name", null);

        tester.clickLink("form:save");

        Assert.assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        Assert.assertEquals(
                "Field 'Name' is required.",
                tester.getMessages(FeedbackMessage.ERROR).get(0).toString());
        tester.assertLabel(
                "topFeedback:feedbackul:messages:0:message", "Field &#039;Name&#039; is required.");
    }

    @Test
    public void testLayerTab() {
        // Load the page
        MetadataTemplatePage page =
                new MetadataTemplatePage(new ListModel<>(templateService.list()));
        tester.startPage(page);
        tester.assertRenderedPage(MetadataTemplatePage.class);
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("form:metadataTabs"))
                .setSelectedTab(1);
        tester.submitForm("form");
        tester.assertComponent("form:metadataTabs:panel", LinkedLayersPanel.class);
        tester.assertComponent("form:metadataTabs:panel:layersTable", GeoServerTablePanel.class);
    }
}
