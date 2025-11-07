/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.layerdetails;

import static org.geoserver.acl.domain.rules.CatalogMode.HIDE;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.plugin.web.accessrules.event.GrantTypeChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.LayerChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.model.DataAccessRuleEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.LayerDetailsEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.MutableLayerDetails;
import org.geoserver.acl.plugin.web.accessrules.model.MutableRule;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LayerDetailsEditPanelTest extends AclWicketTestSupport {

    private LayerDetailsEditModel model;
    private LayerDetailsEditPanel panel;
    private FormPage page;

    private AjaxRequestTarget mockTarget;

    @Before
    @Override
    public void beforeEach() throws IOException {
        super.beforeEach();
        mockTarget = mock(AjaxRequestTarget.class);
        setUpTester(new MutableRule());
    }

    private void setUpTester(MutableRule rule) {
        DataAccessRuleEditModel ruleModel = new DataAccessRuleEditModel(rule);
        model = new LayerDetailsEditModel(ruleModel);
        panel = new LayerDetailsEditPanel("panel", model);
        page = tester.startPage(createFormPage(panel));
        tester.assertNoErrorMessage();
    }

    /** Simulate a rule's {@link GrantType} change */
    private void setGrant(GrantType grant) {
        model.getRule().getObject().setAccess(grant);
        panel.onGrantTypeChangeEvent(new GrantTypeChangeEvent(grant, mockTarget));
    }

    /** Simulate a layer name change */
    private void setLayer(String layer) {
        model.getRule().getObject().setLayer(layer);
        panel.onLayerChangeEvent(new LayerChangeEvent(layer, mockTarget));
    }

    @Test
    public void testPanelVisibility() {
        assertFalse(model.isShowPanel());
        tester.assertInvisible("form:panel");

        // simulate layer change
        setLayer("layer");

        assertFalse(
                "layer selected but grantType is not allow: "
                        + model.getRule().getObject().getAccess(),
                model.isShowPanel());
        tester.assertInvisible("form:panel");

        // simulate grantType change
        setGrant(ALLOW);

        assertTrue(model.isShowPanel());
        tester.assertVisible("form:panel");

        // unselect layer
        setLayer(null);
        tester.assertInvisible("form:panel");

        // re-select layer but change granttype
        setLayer("layer2");
        setGrant(LIMIT);
        tester.assertInvisible("form:panel");

        // set grantType back to allow
        setGrant(ALLOW);
        tester.assertVisible("form:panel");
    }

    @Test
    public void testSetLayerDetailsCheckbox() {
        setGrant(ALLOW);
        setLayer("layer");
        assertTrue(
                "panel visibility should be true when ALLOW and layer are set, to see the set details checkbox",
                model.isShowPanel());
        assertFalse("panel details visibility should be false by default", model.isShowLayerDetails());
        // print(page, true, true);
        tester.assertVisible("form:panel");

        tester.assertComponent("form:panel:setLayerDetails", CheckBox.class);
        tester.assertInvisible("form:panel:detailsContainer");

        // enable the "Set layer details" check
        FormTester form = createFormTester(page);
        form.setValue("panel:setLayerDetails", true);
        tester.executeAjaxEvent("form:panel:setLayerDetails", "change");

        assertTrue(model.isShowPanel());
        assertTrue(
                "setLayerDetails model not updated",
                model.getSetLayerDetailsModel().getObject());
        assertTrue(model.isShowLayerDetails());
        super.setFormComponentValue(panel, password);
        tester.assertVisible("form:panel:detailsContainer");
    }

    @Test
    public void testAllowRuleWithNoLayerDetailsSetShowsCheckButNoDetails() {
        // set up an initial rule of type ALLOW and layer set but with no details
        MutableRule rule = new MutableRule();
        rule.setAccess(ALLOW);
        rule.setLayer("layer");
        assertTrue(rule.getLayerDetails().isNew());

        setUpTester(rule);

        // panel visible
        assertTrue(model.isShowPanel());
        tester.assertVisible("form:panel");

        // Set layer details check unchecked
        tester.assertModelValue("form:panel:setLayerDetails", false);

        // detailsContainer invisible
        assertFalse(model.isShowLayerDetails());
        tester.assertInvisible("form:panel:detailsContainer");
    }

    @Test
    public void testSetLayerDetailsCheckboxAutoEnabledIfRuleHasLayerDetails() {
        // set up an initial rule with layer details already present
        MutableRule rule = new MutableRule();
        rule.setAccess(ALLOW);
        rule.setLayer("layer");
        assertTrue(rule.getLayerDetails().isNew());
        rule.getLayerDetails().setCatalogMode(HIDE);
        rule.getLayerDetails().setCqlFilterWrite("testprop > 1");
        assertFalse(rule.getLayerDetails().isNew());

        setUpTester(rule);
        assertTrue(model.isShowLayerDetails());
        tester.assertVisible("form:panel");
        tester.assertVisible("form:panel:detailsContainer");
        tester.assertModelValue("form:panel:setLayerDetails", true);
    }

    @Ignore("revisit")
    @Test
    public void testSubmitForDefaultInstance() {
        FormTester form = createFormTester(page);
        //    	form.setValue("panel:detailsContainer:layerType", "VECTOR");
        form.setValue("panel:detailsContainer:styles:defaultStyle", "anyStyle");
        form.setValue("panel:detailsContainer:styles:allowedStyles", "style1,style2,style3");
        form.setValue("panel:detailsContainer:cqlFilters:cqlFilterRead", "prop1 = 1");
        form.setValue("panel:detailsContainer:cqlFilters:cqlFilterWrite", "prop2 = 2");
        form.submit();

        tester.assertNoErrorMessage();
        MutableLayerDetails updated = model.getModel().getObject();
        assertEquals("anyStyle", updated.getDefaultStyle());
        assertEquals(Set.of("style1", "style2", "style3"), updated.getAllowedStyles());
        assertEquals("prop1 = 1", updated.getCqlFilterRead());
        assertEquals("prop2 = 2", updated.getCqlFilterWrite());
    }
}
