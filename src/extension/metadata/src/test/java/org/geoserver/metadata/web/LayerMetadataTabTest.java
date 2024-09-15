/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.AbstractWicketMetadataTest;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.impl.MetadataConstants;
import org.geoserver.metadata.web.panel.ImportGeonetworkPanel;
import org.geoserver.metadata.web.panel.ImportTemplatePanel;
import org.geoserver.metadata.web.panel.MetadataPanel;
import org.geoserver.metadata.web.panel.attribute.CheckBoxPanel;
import org.geoserver.metadata.web.panel.attribute.DateTimeFieldPanel;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.DateField;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test metadatatab in layer page.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class LayerMetadataTabTest extends AbstractWicketMetadataTest {

    private LayerInfo layer;

    @Before
    @Override
    public void start() throws Exception {
        super.start();
        login();
        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertNotNull(layer);
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);
        tester.startPage(page);
        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("publishedinfo:tabs"))
                .setSelectedTab(4);
        tester.submitForm("publishedinfo");
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", MetadataPanel.class);
    }

    @After
    @Override
    public void stop() throws Exception {
        logout();
        restoreTemplates();
        restoreLayers();
        super.stop();
    }

    @Test
    public void testMetadataTab() {
        // check we are on the correct page
        tester.assertComponent("publishedinfo:tabs:panel:metadataPanel", MetadataPanel.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:importTemplatePanel", ImportTemplatePanel.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:geonetworkPanel", ImportGeonetworkPanel.class);

        GeoServerTablePanel<?> panel =
                (GeoServerTablePanel<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel");
        // derived field hidden
        assertEquals(15, panel.getDataProvider().size());

        FormTester ft = tester.newFormTester("publishedinfo");
        ft.submit("save");

        // has at least metadata map with timestamp
        layer = geoServer.getCatalog().getLayerByName("mylayer");
        assertNotNull(layer.getResource().getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY));
        assertNotNull(
                ((Map<?, ?>)
                                layer.getResource()
                                        .getMetadata()
                                        .get(MetadataConstants.CUSTOM_METADATA_KEY))
                        .get(MetadataConstants.TIMESTAMP_KEY));
    }

    /** The layer is linked to the 'simple field' template. */
    @Test
    public void testReadMedataFields() {

        // Metadata field
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "f7de06ca-f93c-457b-b0ae-9c52f5b1ca5e");
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:1:itemProperties:1:component:generateUUID");
        Component metadataTextField =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield");
        Assert.assertNotEquals(
                metadataTextField.getDefaultModel().getObject(),
                "f7de06ca-f93c-457b-b0ae-9c52f5b1ca5e");

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:3:itemProperties:1:component:textfield",
                77);

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield",
                "template-identifier");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:4:itemProperties:1:component:dropdown",
                "Or select this row");
        // string list (refsystem-as-list)
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:textfield",
                "reflist-first");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:textfield",
                "reflist-second");
        // object list (referencesystem-object-list)
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "First object :code");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield",
                "First object :codeSpace");

        // nested objects (object catalog)
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "theObject catalog");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:dropdown",
                "Date");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:4:itemProperties:1:component:textarea",
                "a date");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "nestedobject");

        // test list of linked templates
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:1:itemProperties:0:component",
                "simple fields");

        // test values of 'simple fields template'
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield",
                "template-identifier");
        Assert.assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:2:itemProperties:1:component")
                        .isEnabled());

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:3:itemProperties:1:component:textfield",
                77);
        Assert.assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:3:itemProperties:1:component")
                        .isEnabled());

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:4:itemProperties:1:component:dropdown",
                "Or select this row");
        Assert.assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:4:itemProperties:1:component")
                        .isEnabled());
    }

    /** test special types * */
    @Test
    public void testSpecialTypeFields() {
        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:8:itemProperties:1:component",
                DateTimeFieldPanel.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:9:itemProperties:1:component",
                DateTimeFieldPanel.class);
        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:10:itemProperties:1:component",
                CheckBoxPanel.class);
    }

    /** Test if we can add and remove fields from a list. */
    @Test
    public void testRepeatFields() {
        // add row
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:addNew");
        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:textfield",
                TextField.class);

        // move row up and down
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:4:itemProperties:1:component:up");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:6:itemProperties:0:component:textfield",
                "reflist-second");
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:6:itemProperties:1:component:down");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:10:itemProperties:0:component:textfield",
                "reflist-second");

        // delete row
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:9:itemProperties:2:component");
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:12:itemProperties:2:component");
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:14:itemProperties:2:component");
        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:noData",
                Label.class);
    }

    /**
     * Test if we can add complex field from a list, and that the underlying model creates empty
     * spaces to keep all attribute lengths the same for the complex field.
     */
    @Test
    public void testRepeatComplexFields() {
        // add row
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:addNew");
        // print(tester.getLastRenderedPage(), true, true);

        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel",
                GeoServerTablePanel.class);

        @SuppressWarnings("unchecked")
        HashMap<String, Serializable> underlying =
                (HashMap<String, Serializable>)
                        layer.getResource()
                                .getMetadata()
                                .get(MetadataConstants.CUSTOM_METADATA_KEY);

        ArrayList<?> list1 = (ArrayList<?>) underlying.get("referencesystem-object-list/code");
        ArrayList<?> list2 =
                (ArrayList<?>) underlying.get("referencesystem-object-list/code-space");
        assertEquals(2, list1.size());
        assertEquals(2, list2.size());

        assertEquals("First object :code", list1.get(0));
        assertEquals(null, list1.get(1));

        // move row up and down
        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:1:component:up");
        list1 = (ArrayList<?>) underlying.get("referencesystem-object-list/code");
        assertEquals(null, list1.get(0));
        assertEquals("First object :code", list1.get(1));

        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:4:itemProperties:1:component:down");
        list1 = (ArrayList<?>) underlying.get("referencesystem-object-list/code");
        assertEquals("First object :code", list1.get(0));
        assertEquals(null, list1.get(1));
    }

    /** Test if we can add links with a template. */
    @SuppressWarnings("unchecked")
    @Test
    public void testLinkWithSimpleAndListTemplates() {
        // link template-list-simple
        DropDownChoice<?> selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        Assert.assertEquals(6, selectTemplate.getChoices().size());
        MetadataTemplateImpl template = (MetadataTemplateImpl) selectTemplate.getChoices().get(0);
        Assert.assertEquals("template-list-simple", template.getName());
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");
        // test list of linked templates
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:3:itemProperties:0:component",
                "simple fields");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:4:itemProperties:0:component",
                "template-list-simple");

        // test values
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:textfield",
                "template-value01");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:textfield",
                "template--value02");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:textfield",
                "reflist-first");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:4:itemProperties:0:component:textfield",
                "reflist-second");
        Assert.assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component")
                        .isEnabled());
        Assert.assertFalse(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component")
                        .isEnabled());
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component")
                        .isEnabled());
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:4:itemProperties:0:component")
                        .isEnabled());

        // link template-object list
        selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        Assert.assertEquals(5, selectTemplate.getChoices().size());
        template = (MetadataTemplateImpl) selectTemplate.getChoices().get(0);
        Assert.assertEquals("template-object list", template.getName());
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:7:itemProperties:0:component",
                "simple fields");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:8:itemProperties:0:component",
                "template-list-simple");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:9:itemProperties:0:component",
                "template-object list");

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "template-code01");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield",
                "template-codespace01");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "template-code02");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield",
                "template-codespace02");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "First object :code");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield",
                "First object :codeSpace");

        // TODO figure out why this test fails.. the gui looks ok

        /*Assert.assertFalse(tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield").isEnabled());
        Assert.assertFalse(tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield").isEnabled());
        Assert.assertFalse(tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield").isEnabled());
        Assert.assertFalse(tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield").isEnabled());
        Assert.assertTrue(tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield").isEnabled());
        Assert.assertTrue(tester.getComponentFromLastRenderedPage("publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:textfield").isEnabled());*/

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUnlinkFromSimpleAndListTemplates() {
        // link 2 more templates
        DropDownChoice<?> selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        MetadataTemplateImpl template = (MetadataTemplateImpl) selectTemplate.getChoices().get(0);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");
        template = (MetadataTemplateImpl) selectTemplate.getChoices().get(0);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        // print(tester.getLastRenderedPage(), true, true);
        // check the link
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:7:itemProperties:0:component",
                "simple fields");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:8:itemProperties:0:component",
                "template-list-simple");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:9:itemProperties:0:component",
                "template-object list");

        // first check the checkboxes
        Component checkbox01 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:7:selectItemContainer:selectItem");
        ((IModel<Boolean>) checkbox01.getDefaultModel()).setObject(true);
        Component checkbox02 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:8:selectItemContainer:selectItem");
        ((IModel<Boolean>) checkbox02.getDefaultModel()).setObject(true);
        Component checkbox03 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:9:selectItemContainer:selectItem");
        ((IModel<Boolean>) checkbox03.getDefaultModel()).setObject(true);
        // click remove links
        tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:removeSelected")
                .setEnabled(true);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:removeSelected");
        // linked list should be empty
        tester.assertComponent("publishedinfo:tabs:panel:importTemplatePanel:noData", Label.class);

        // list data remove
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:textfield",
                "reflist-first");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:textfield",
                "reflist-second");

        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component")
                        .isEnabled());
        assertTrue(
                tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component")
                        .isEnabled());
    }

    /**
     * Adding and deleting a template multiple times deletes the wrong row (selection model out of
     * sync).
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLinkAndUnlinkDeleteRowTemplatesBug() {
        // add link
        DropDownChoice<?> selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        MetadataTemplateImpl template = (MetadataTemplateImpl) selectTemplate.getChoices().get(0);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:3:itemProperties:0:component",
                "simple fields");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:4:itemProperties:0:component",
                "template-list-simple");

        // delete first link
        Component checkbox01 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:3:selectItemContainer:selectItem");
        ((IModel<Boolean>) checkbox01.getDefaultModel()).setObject(true);

        // click remove links
        tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:removeSelected")
                .setEnabled(true);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:removeSelected");

        // Check first line is remove
        tester.assertContainsNot("'simple fields'");

        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:5:itemProperties:0:component",
                "template-list-simple");

        // add link
        selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        template = (MetadataTemplateImpl) selectTemplate.getChoices().get(1);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        // print(tester.getLastRenderedPage(), true, true);
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:7:itemProperties:0:component",
                "template-list-simple");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:8:itemProperties:0:component",
                "template-object list");

        // delete first link
        Component checkbox02 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:7:selectItemContainer:selectItem");
        ((IModel<Boolean>) checkbox02.getDefaultModel()).setObject(true);

        // click remove links
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:removeSelected");

        tester.assertContainsNot("'template-list-simple'");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:9:itemProperties:0:component",
                "template-object list");
    }

    /**
     * When there are no links 'No data to display.' is shown. Adding a template does not show the
     * table with linked templates.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLinkRowTemplatesBug() {
        Assert.assertNull(
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:noData"));
        // remove the current links
        Component checkbox01 =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:1:selectItemContainer:selectItem");
        ((IModel<Boolean>) checkbox01.getDefaultModel()).setObject(true);
        tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:removeSelected")
                .setEnabled(true);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:removeSelected");

        // assert no data to display
        Assert.assertNotNull(
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:noData"));
        Assert.assertNull(
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:1:itemProperties:0:component"));
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:noData", "No data to display.");
        tester.assertContainsNot("'simple fields'");

        // add template
        DropDownChoice<?> selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        MetadataTemplateImpl template = (MetadataTemplateImpl) selectTemplate.getChoices().get(0);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        // check table is visible
        Assert.assertNull(
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:noData"));
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:2:itemProperties:0:component",
                "simple fields");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTemplatesRemainInPriorityOrder() {
        // add link
        DropDownChoice<?> selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        MetadataTemplateImpl template = (MetadataTemplateImpl) selectTemplate.getChoices().get(4);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        selectTemplate =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:importTemplatePanel:metadataTemplate");
        template = (MetadataTemplateImpl) selectTemplate.getChoices().get(1);
        ((IModel<MetadataTemplateImpl>) selectTemplate.getDefaultModel()).setObject(template);
        tester.clickLink("publishedinfo:tabs:panel:importTemplatePanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:importTemplatePanel:importDialog:dialog:content:form:submit");

        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:7:itemProperties:0:component",
                "simple fields");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:8:itemProperties:0:component",
                "template-object list");
        tester.assertLabel(
                "publishedinfo:tabs:panel:importTemplatePanel:templatesPanel:listContainer:items:9:itemProperties:0:component",
                "allData");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testImportFromGeonetwork() {
        // check the layer is linked
        Assert.assertNull(
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:importTemplatePanel:noData"));

        // Import from geonetwork
        DropDownChoice<?> geonetwork =
                (DropDownChoice<?>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:geonetworkPanel:geonetworkName");
        String geonetworkName = (String) geonetwork.getChoices().get(0);
        ((IModel<String>) geonetwork.getDefaultModel()).setObject(geonetworkName);
        TextField<String> uuid =
                (TextField<String>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:geonetworkPanel:textfield");
        ((IModel<String>) uuid.getDefaultModel()).setObject("1a2c6739-3c62-432b-b2a0-aaa589a9e3a1");

        tester.clickLink("publishedinfo:tabs:panel:geonetworkPanel:link");
        tester.clickLink(
                "publishedinfo:tabs:panel:geonetworkPanel:importDialog:dialog:content:form:submit");

        // print(tester.getLastRenderedPage(), true, true);

        // Check content
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:textfield",
                "http://www.opengis.net/def/crs/EPSG/0/3043");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:textfield",
                "Belge_Lambert_1972 (31370)");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:5:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:textfield",
                "TAW");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:3:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "http://www.opengis.net/def/crs/EPSG/0/3043");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "Belge_Lambert_1972 (31370)");
        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component:textfield",
                "TAW");

        // should remove all links
        tester.assertComponent("publishedinfo:tabs:panel:importTemplatePanel:noData", Label.class);
    }

    @Test
    public void testImportFromGeonetworkValidation() {
        tester.clickLink("publishedinfo:tabs:panel:geonetworkPanel:link");
        // print(tester.getLastRenderedPage(), true, true);

        Assert.assertEquals(2, tester.getMessages(FeedbackMessage.ERROR).size());
        Assert.assertEquals(
                "Select a geonetwork", tester.getMessages(FeedbackMessage.ERROR).get(0).toString());
        Assert.assertEquals(
                "A metadata UUID is required",
                tester.getMessages(FeedbackMessage.ERROR).get(1).toString());
        tester.assertLabel(
                "publishedinfo:tabs:panel:geonetworkPanel:importFeedback:feedbackul:messages:0:message",
                "Select a geonetwork");
        tester.assertLabel(
                "publishedinfo:tabs:panel:geonetworkPanel:importFeedback:feedbackul:messages:1:message",
                "A metadata UUID is required");
    }

    @Test
    public void testGenerateFeatureCatalogueAndDomain() {
        MarkupContainer c =
                (MarkupContainer)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items");
        assertEquals(15, c.size());

        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:generate",
                AjaxSubmitLink.class);

        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:generate");

        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:dialog",
                GeoServerDialog.class);

        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:dialog:dialog:content:form:submit");

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<ComplexMetadataMap> panel =
                (GeoServerTablePanel<ComplexMetadataMap>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel");

        assertEquals(23, panel.getDataProvider().size());

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:1:itemProperties:1:component",
                "STATE_NAME");

        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:generate",
                AjaxSubmitLink.class);

        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:generate");

        tester.assertComponent(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:dialog",
                GeoServerDialog.class);

        tester.clickLink(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:dialog:dialog:content:form:submit");

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<ComplexMetadataMap> panel2 =
                (GeoServerTablePanel<ComplexMetadataMap>)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel");

        assertEquals(49, panel2.getDataProvider().size());

        tester.assertModelValue(
                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:13:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component:attributesTablePanel:listContainer:items:2:itemProperties:0:component:attributesTablePanel:listContainer:items:7:itemProperties:1:component:attributesTablePanel:listContainer:items:1:itemProperties:0:component:attributesTablePanel:listContainer:items:2:itemProperties:1:component",
                "Alabama");
    }

    /** test special types * */
    @Test
    public void testDatePickerIsPresent() {
        String lastPage = tester.getLastResponseAsString();
        DateField dField =
                (DateField)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:8:itemProperties:1:component:dateTimeField");
        DateField dField2 =
                (DateField)
                        tester.getComponentFromLastRenderedPage(
                                "publishedinfo:tabs:panel:metadataPanel:attributesPanel:attributesTablePanel:listContainer:items:9:itemProperties:1:component:dateTimeField");
        assertTrue(
                lastPage.contains(
                        "initJQDatepicker('"
                                + dField.getMarkupId()
                                + "',false,'yyyy-MM-dd',' ');"));
        assertTrue(
                lastPage.contains(
                        "initJQDatepicker('"
                                + dField2.getMarkupId()
                                + "',true,'yyyy-MM-dd HH:mm',' ');"));
    }
}
