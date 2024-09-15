/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test templates page.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class TemplatesPageTest extends AbstractWicketMetadataTest {

    @Before
    @Override
    public void start() throws Exception {
        super.start();
        login();
        // Make sure the catalog is loaded
        LayerInfo layer = geoServer.getCatalog().getLayers().get(0);
        new ResourceConfigurationPage(layer, false);

        // Load the page
        tester.startPage(new MetadataTemplatesPage());
        tester.assertRenderedPage(MetadataTemplatesPage.class);
    }

    @After
    @Override
    public void stop() throws Exception {
        restoreLayers();
        restoreTemplates();
        logout();
        super.stop();
    }

    @Test
    public void testPage() {

        // print(tester.getLastRenderedPage(), true, true);

        tester.assertComponent("addNew", AjaxLink.class);
        tester.assertComponent("removeSelected", AjaxLink.class);
        tester.assertComponent("templatesPanel", GeoServerTablePanel.class);
        // Check content of the table
        // first row
        tester.assertLabel(
                "templatesPanel:listContainer:items:1:itemProperties:1:component:link:label",
                "simple fields");
        tester.assertLabel(
                "templatesPanel:listContainer:items:1:itemProperties:2:component",
                "Only basic fields");
        // other rows
        tester.assertLabel(
                "templatesPanel:listContainer:items:2:itemProperties:1:component:link:label",
                "template-list-simple");
        tester.assertLabel(
                "templatesPanel:listContainer:items:3:itemProperties:1:component:link:label",
                "template-object list");
        tester.assertLabel(
                "templatesPanel:listContainer:items:4:itemProperties:1:component:link:label",
                "template-nested-object");
        tester.assertLabel(
                "templatesPanel:listContainer:items:5:itemProperties:1:component:link:label",
                "object-field");
        // lastrow
        tester.assertLabel(
                "templatesPanel:listContainer:items:6:itemProperties:1:component:link:label",
                "allData");
        tester.assertLabel(
                "templatesPanel:listContainer:items:6:itemProperties:2:component", "All fields");
    }

    @Test
    public void testNewNavigation() throws Exception {

        tester.clickLink("addNew");

        tester.assertRenderedPage(MetadataTemplatePage.class);
        // print(tester.getLastRenderedPage(), true, true);

        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:description", TextField.class);

        tester.assertComponent("form:metadataTabs:panel", MetadataPanel.class);
    }

    @Test
    public void testEditNavigation() throws Exception {
        // print(tester.getLastRenderedPage(), true, true);
        // Navigate to first template
        tester.clickLink("templatesPanel:listContainer:items:1:itemProperties:1:component:link");

        tester.assertRenderedPage(MetadataTemplatePage.class);
        // print(tester.getLastRenderedPage(), true, true);

        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:description", TextField.class);

        tester.assertModelValue("form:name", "simple fields");
        tester.assertModelValue("form:description", "Only basic fields");

        tester.assertComponent("form:metadataTabs:panel", MetadataPanel.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDelete() throws Exception {

        // select first template
        ((IModel<Boolean>)
                        tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items:2:selectItemContainer:selectItem")
                                .getDefaultModel())
                .setObject(true);
        // select third templete
        ((IModel<Boolean>)
                        tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items:3:selectItemContainer:selectItem")
                                .getDefaultModel())
                .setObject(true);
        // delete
        tester.getComponentFromLastRenderedPage("removeSelected").setEnabled(true);
        tester.clickLink("removeSelected");

        // print(tester.getLastRenderedPage(), true, true);
        tester.assertComponent("dialog:dialog:content:form:userPanel", MultiLineLabel.class);
        tester.clickLink("dialog:dialog:content:form:submit");

        // Check update content of the table)
        assertEquals(
                5,
                ((MarkupContainer)
                                tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items"))
                        .size());

        tester.clickLink("save");

        assertEquals(5, templateService.list().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteWarning() throws Exception {
        // select first template
        ((IModel<Boolean>)
                        tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items:1:selectItemContainer:selectItem")
                                .getDefaultModel())
                .setObject(true);
        // select third template
        ((IModel<Boolean>)
                        tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items:3:selectItemContainer:selectItem")
                                .getDefaultModel())
                .setObject(true);
        // delete
        tester.getComponentFromLastRenderedPage("removeSelected").setEnabled(true);
        tester.clickLink("removeSelected");

        // Check content of the table is the same
        assertEquals(
                7,
                ((MarkupContainer)
                                tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items"))
                        .size());

        // print(tester.getLastRenderedPage(), true, true);
        Assert.assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        Assert.assertEquals(
                "Template 'simple fields' is not deleted. Linked to layers: topp:mylayer",
                tester.getMessages(FeedbackMessage.ERROR).get(0).toString());
        tester.assertLabel(
                "topFeedback:feedbackul:messages:0:message",
                "Template &#039;simple fields&#039; is not deleted. Linked to layers: topp:mylayer");
    }

    @Test
    public void testIncreasePriority() throws Exception {
        // template-nested-object
        tester.clickLink("templatesPanel:listContainer:items:4:itemProperties:0:component:up:link");

        // Check update content of the table
        tester.assertLabel(
                "templatesPanel:listContainer:items:8:itemProperties:1:component:link:label",
                "simple fields");
        tester.assertLabel(
                "templatesPanel:listContainer:items:9:itemProperties:1:component:link:label",
                "template-list-simple");
        tester.assertLabel(
                "templatesPanel:listContainer:items:10:itemProperties:1:component:link:label",
                "template-nested-object");
        tester.assertLabel(
                "templatesPanel:listContainer:items:11:itemProperties:1:component:link:label",
                "template-object list");
        tester.assertLabel(
                "templatesPanel:listContainer:items:12:itemProperties:1:component:link:label",
                "object-field");
        tester.assertLabel(
                "templatesPanel:listContainer:items:13:itemProperties:1:component:link:label",
                "allData");
        tester.assertLabel(
                "templatesPanel:listContainer:items:14:itemProperties:1:component:link:label",
                "norepeat");

        tester.clickLink("save");

        assertEquals("template-nested-object", templateService.list().get(2).getName());

        tester.startPage(new MetadataTemplatesPage());

        // template-nested-object again
        tester.clickLink("templatesPanel:listContainer:items:3:itemProperties:0:component:up:link");

        // Check update content of the table
        tester.assertLabel(
                "templatesPanel:listContainer:items:8:itemProperties:1:component:link:label",
                "simple fields");
        tester.assertLabel(
                "templatesPanel:listContainer:items:9:itemProperties:1:component:link:label",
                "template-nested-object");
        tester.assertLabel(
                "templatesPanel:listContainer:items:10:itemProperties:1:component:link:label",
                "template-list-simple");
        tester.assertLabel(
                "templatesPanel:listContainer:items:11:itemProperties:1:component:link:label",
                "template-object list");
        tester.assertLabel(
                "templatesPanel:listContainer:items:12:itemProperties:1:component:link:label",
                "object-field");
        tester.assertLabel(
                "templatesPanel:listContainer:items:13:itemProperties:1:component:link:label",
                "allData");
        tester.assertLabel(
                "templatesPanel:listContainer:items:14:itemProperties:1:component:link:label",
                "norepeat");

        tester.clickLink("cancel");

        assertEquals("template-nested-object", templateService.list().get(2).getName());

        tester.startPage(new MetadataTemplatesPage());

        // Check cancelled content of the table
        tester.assertLabel(
                "templatesPanel:listContainer:items:1:itemProperties:1:component:link:label",
                "simple fields");
        tester.assertLabel(
                "templatesPanel:listContainer:items:2:itemProperties:1:component:link:label",
                "template-list-simple");
        tester.assertLabel(
                "templatesPanel:listContainer:items:3:itemProperties:1:component:link:label",
                "template-nested-object");
        tester.assertLabel(
                "templatesPanel:listContainer:items:4:itemProperties:1:component:link:label",
                "template-object list");
        tester.assertLabel(
                "templatesPanel:listContainer:items:5:itemProperties:1:component:link:label",
                "object-field");
        tester.assertLabel(
                "templatesPanel:listContainer:items:6:itemProperties:1:component:link:label",
                "allData");
        tester.assertLabel(
                "templatesPanel:listContainer:items:7:itemProperties:1:component:link:label",
                "norepeat");
    }

    @Test
    public void testDecreasePriority() throws Exception {
        // template-nested-object
        tester.clickLink(
                "templatesPanel:listContainer:items:4:itemProperties:0:component:down:link");
        tester.clickLink(
                "templatesPanel:listContainer:items:12:itemProperties:0:component:down:link");
        // print(tester.getLastRenderedPage(), true, true);
        // Check update content of the table
        tester.assertLabel(
                "templatesPanel:listContainer:items:15:itemProperties:1:component:link:label",
                "simple fields");
        tester.assertLabel(
                "templatesPanel:listContainer:items:16:itemProperties:1:component:link:label",
                "template-list-simple");
        tester.assertLabel(
                "templatesPanel:listContainer:items:17:itemProperties:1:component:link:label",
                "template-object list");
        tester.assertLabel(
                "templatesPanel:listContainer:items:18:itemProperties:1:component:link:label",
                "object-field");
        tester.assertLabel(
                "templatesPanel:listContainer:items:19:itemProperties:1:component:link:label",
                "allData");
        tester.assertLabel(
                "templatesPanel:listContainer:items:20:itemProperties:1:component:link:label",
                "template-nested-object");
        tester.assertLabel(
                "templatesPanel:listContainer:items:21:itemProperties:1:component:link:label",
                "norepeat");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCopy() throws Exception {

        // select first template
        ((IModel<Boolean>)
                        tester.getComponentFromLastRenderedPage(
                                        "templatesPanel:listContainer:items:2:selectItemContainer:selectItem")
                                .getDefaultModel())
                .setObject(true);
        // copy
        tester.getComponentFromLastRenderedPage("copySelected").setEnabled(true);
        tester.clickLink("copySelected");

        tester.assertRenderedPage(MetadataTemplatePage.class);

        tester.assertModelValue("form:name", null);
        tester.assertModelValue("form:description", "List of basic fields");
    }
}
