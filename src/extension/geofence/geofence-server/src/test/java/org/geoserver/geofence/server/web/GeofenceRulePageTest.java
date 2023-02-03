/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.server.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.core.model.LayerDetails;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.LayerType;
import org.geoserver.geofence.core.model.enums.SpatialFilterType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layergroup.LayerGroupBaseTest;
import org.junit.Test;

public class GeofenceRulePageTest extends LayerGroupBaseTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        login();
        super.onSetUp(testData);
    }

    @Test
    public void testAddLayerGroupRule() {
        GeofenceRulesModel model = new GeofenceRulesModel();
        ShortRule shortRule = model.newRule();
        shortRule.setAccess(GrantType.LIMIT);
        tester.startPage(new GeofenceRulePage(shortRule, model));
        tester.assertRenderedPage(GeofenceRulePage.class);
        FormTester ft = tester.newFormTester("form");
        Form form = ft.getForm();
        DropDownChoice layer = (DropDownChoice) form.get("tabs:panel:layer");
        // test that with no workspace set global layer groups are present in the dropdown choice
        assertTrue(layer.getChoices().size() > 0);
        ft.select("tabs:panel:layer", 0);
        ft.select("tabs:panel:catalogMode", 0);
        ft.select("tabs:panel:spatialFilterType", 1);
        // according to a wicket user forum post, when setting a value after dropdown select
        // a new form test is needed otherwise the value will not be set.
        ft = tester.newFormTester("form");
        ft.setValue(
                "tabs:panel:allowedArea", "SRID=4326;POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))");
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        ft.submit("save");
        tester.assertNoErrorMessage();
        RuleAdminService adminService =
                (RuleAdminService) GeoServerApplication.get().getBean("ruleAdminService");
        ShortRule rule = adminService.getRuleByPriority(0L);
        assertEquals("lakes", rule.getLayer());
        assertNotNull(rule.getAccess().name());
        Rule fullRule = adminService.get(rule.getId());
        RuleLimits limits = fullRule.getRuleLimits();
        assertEquals(CatalogMode.HIDE, limits.getCatalogMode());
        assertEquals(SpatialFilterType.CLIP, limits.getSpatialFilterType());
        assertNotNull(limits.getAllowedArea());
    }

    @Test
    public void testLayerTypeIsAutomaticallySet() {
        RuleAdminService adminService =
                (RuleAdminService) GeoServerApplication.get().getBean("ruleAdminService");
        Rule rule = new Rule();
        rule.setPriority(9999L);
        rule.setWorkspace("cite");
        rule.setLayer("BasicPolygons");
        rule.setAccess(GrantType.ALLOW);
        long ruleId = adminService.insert(rule);
        LayerDetails layerDetails = new LayerDetails();
        adminService.setDetails(ruleId, layerDetails);
        GeofenceRulesModel model = new GeofenceRulesModel();
        ShortRule ruleModel = new ShortRule(rule);
        try {
            login();
            tester.startPage(new GeofenceRulePage(ruleModel, model));
            tester.clickLink("form:tabs:tabs-container:tabs:1:link");
            FormTester ft = tester.newFormTester("form");
            ft.setValue("tabs:panel:layerDetailsCheck", true);
            tester.executeAjaxEvent("form:tabs:panel:layerDetailsCheck", "change");
            tester.assertModelValue(
                    "form:tabs:panel:layerDetailsContainer:layerType", LayerType.VECTOR);
            Component availableStyles =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:allowedStyles");
            Component defaultStyle =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:defaultStyle");
            Component cqlFilterRead =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:cqlFilterRead");
            Component cqlFilterWrite =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:cqlFilterWrite");
            Component allowedArea =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:allowedArea");
            // checks component are present and enabled
            assertTrue(availableStyles.isEnabled());
            assertTrue(defaultStyle.isEnabled());
            assertTrue(cqlFilterRead.isEnabled());
            assertTrue(cqlFilterWrite.isEnabled());
            assertTrue(allowedArea.isEnabled());

            @SuppressWarnings("unchecked")
            DropDownChoice<SpatialFilterType> spatialFilterType =
                    (DropDownChoice<SpatialFilterType>)
                            tester.getComponentFromLastRenderedPage(
                                    "form:tabs:panel:layerDetailsContainer:spatialFilterType");

            assertTrue(spatialFilterType.isEnabled());
            assertEquals(SpatialFilterType.INTERSECT, spatialFilterType.getModelObject());
        } finally {
            deleteRule(ruleId);
            logout();
        }
    }

    @Test
    public void testLayerTypeIsAutomaticallySet2() {
        RuleAdminService adminService =
                (RuleAdminService) GeoServerApplication.get().getBean("ruleAdminService");
        Rule rule = new Rule();
        rule.setPriority(9999L);
        rule.setLayer("lakes");
        rule.setAccess(GrantType.ALLOW);
        long ruleId = adminService.insert(rule);
        LayerDetails layerDetails = new LayerDetails();
        adminService.setDetails(ruleId, layerDetails);
        GeofenceRulesModel model = new GeofenceRulesModel();
        ShortRule ruleModel = new ShortRule(rule);
        try {
            login();
            tester.startPage(new GeofenceRulePage(ruleModel, model));
            tester.clickLink("form:tabs:tabs-container:tabs:1:link");
            FormTester ft = tester.newFormTester("form");
            ft.setValue("tabs:panel:layerDetailsCheck", true);
            tester.executeAjaxEvent("form:tabs:panel:layerDetailsCheck", "change");
            tester.assertModelValue(
                    "form:tabs:panel:layerDetailsContainer:layerType", LayerType.LAYERGROUP);
            Component availableStyles =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:allowedStyles");
            Component defaultStyle =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:defaultStyle");
            Component cqlFilterRead =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:cqlFilterRead");
            Component cqlFilterWrite =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:cqlFilterWrite");
            Component allowedArea =
                    tester.getComponentFromLastRenderedPage(
                            "form:tabs:panel:layerDetailsContainer:allowedArea");

            assertFalse(availableStyles.isEnabled());
            assertFalse(defaultStyle.isEnabled());
            assertFalse(cqlFilterRead.isEnabled());
            assertFalse(cqlFilterWrite.isEnabled());
            assertTrue(allowedArea.isEnabled());

            @SuppressWarnings("unchecked")
            DropDownChoice<SpatialFilterType> spatialFilterType =
                    (DropDownChoice<SpatialFilterType>)
                            tester.getComponentFromLastRenderedPage(
                                    "form:tabs:panel:layerDetailsContainer:spatialFilterType");

            assertTrue(spatialFilterType.isEnabled());
            assertEquals(SpatialFilterType.INTERSECT, spatialFilterType.getModelObject());
        } finally {
            deleteRule(ruleId);
            logout();
        }
    }

    private void deleteRule(Long id) {
        if (id != null) {
            RuleAdminService adminService =
                    (RuleAdminService) GeoServerApplication.get().getBean("ruleAdminService");
            adminService.delete(id);
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        /** Dispose Services */
        this.testData = new SystemTestData();

        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}",
                    e);
        }
    }
}
