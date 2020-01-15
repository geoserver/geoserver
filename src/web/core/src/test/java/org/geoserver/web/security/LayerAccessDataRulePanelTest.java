package org.geoserver.web.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class LayerAccessDataRulePanelTest extends GeoServerWicketTestSupport {

    IModel<LayerInfo> layerModel;

    @Before
    public void setUpInternal() {
        LayerInfo layerInfo = getCatalog().getLayerByName(getLayerId(MockData.BUILDINGS));
        layerModel = new Model<LayerInfo>(layerInfo);
    }

    @Test
    public void testPageLoad() throws IOException {
        LayerAccessDataRulePanelInfo info = new LayerAccessDataRulePanelInfo();
        ListModel<DataAccessRuleInfo> own = info.createOwnModel(layerModel, false);
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = -5907648151984337786L;

                            public Component buildComponent(final String id) {
                                return new LayerAccessDataRulePanel(id, layerModel, own);
                            }
                        }));

        tester.assertComponent("form:panel", LayerAccessDataRulePanel.class);
        tester.assertComponent("form:panel:dataAccessPanel", AccessDataRulePanel.class);
        tester.assertComponent(
                "form:panel:dataAccessPanel:listContainer", WebMarkupContainer.class);
        tester.assertComponent(
                "form:panel:dataAccessPanel:listContainer:selectAll", CheckBox.class);
        tester.assertComponent("form:panel:dataAccessPanel:listContainer:rules", ListView.class);
    }

    @Test
    public void testPageLoadWithExistingRules() throws IOException {
        LayerAccessDataRulePanelInfo info = new LayerAccessDataRulePanelInfo();
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        Set<String> roles = manager.getAvailableRoles();
        String wsName = layerModel.getObject().getResource().getStore().getWorkspace().getName();
        String layerName = layerModel.getObject().getName();
        List<DataAccessRuleInfo> rules = new ArrayList<DataAccessRuleInfo>();
        roles.forEach(
                r -> {
                    DataAccessRuleInfo rule = new DataAccessRuleInfo(r, layerName, wsName);
                    rule.setRead(true);
                    rules.add(rule);
                });
        Set<DataAccessRule> newRules = manager.mapFrom(rules, roles, wsName, layerName, false);
        manager.saveRules(new HashSet<DataAccessRule>(), newRules);
        try {
            ListModel<DataAccessRuleInfo> own = info.createOwnModel(layerModel, false);
            tester.startPage(
                    new FormTestPage(
                            new ComponentBuilder() {
                                private static final long serialVersionUID = -5907648151984337786L;

                                public Component buildComponent(final String id) {
                                    return new LayerAccessDataRulePanel(id, layerModel, own);
                                }
                            }));

            tester.assertComponent("form:panel", LayerAccessDataRulePanel.class);
            tester.assertComponent("form:panel:dataAccessPanel", AccessDataRulePanel.class);
            tester.assertComponent(
                    "form:panel:dataAccessPanel:listContainer", WebMarkupContainer.class);
            tester.assertComponent(
                    "form:panel:dataAccessPanel:listContainer:selectAll", CheckBox.class);
            tester.assertComponent(
                    "form:panel:dataAccessPanel:listContainer:rules", ListView.class);
            for (int i = 0; i < roles.size(); i++) {
                CheckBox checkBox =
                        (CheckBox)
                                tester.getComponentFromLastRenderedPage(
                                        "form:panel:dataAccessPanel:listContainer:rules:"
                                                + i
                                                + ":read");
                if (checkBox.getId().equals("read")) assertTrue(checkBox.getModelObject());
                else assertFalse(checkBox.getModelObject());
            }
        } finally {
            manager.saveRules(newRules, new HashSet<>());
        }
    }

    @Test
    public void testSaveRules() throws IOException {
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        String wsName = layerModel.getObject().getResource().getStore().getWorkspace().getName();
        try {
            LayerAccessDataRulePanelInfo info = new LayerAccessDataRulePanelInfo();
            ListModel<DataAccessRuleInfo> own = info.createOwnModel(layerModel, false);
            FormTestPage form =
                    new FormTestPage(
                            new ComponentBuilder() {
                                private static final long serialVersionUID = -5907648151984337786L;

                                public Component buildComponent(final String id) {
                                    return new LayerAccessDataRulePanel(id, layerModel, own);
                                }
                            });
            assertTrue(manager.getResourceRule(wsName, layerModel.getObject()).isEmpty());
            tester.startPage(form);
            tester.assertComponent("form:panel", LayerAccessDataRulePanel.class);
            own.getObject().forEach(r -> r.setRead(true));
            LayerAccessDataRulePanel panel =
                    (LayerAccessDataRulePanel)
                            tester.getComponentFromLastRenderedPage("form:panel");
            panel.save();
            assertTrue(manager.getResourceRule(wsName, layerModel.getObject()).size() == 1);
        } finally {
            manager.removeAllResourceRules(wsName, layerModel.getObject());
            assertTrue(manager.getResourceRule(wsName, layerModel.getObject()).isEmpty());
        }
    }
}
