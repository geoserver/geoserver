package org.geoserver.web.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
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
    public void testPageLoad() {
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
        tester.assertComponent("form:panel:dataAccessPanel", AccessDataRulePanelPublished.class);
        tester.assertComponent(
                "form:panel:dataAccessPanel:listContainer", WebMarkupContainer.class);
        tester.assertComponent(
                "form:panel:dataAccessPanel:listContainer:selectAll", CheckBox.class);
        tester.assertComponent("form:panel:dataAccessPanel:listContainer:rules", ListView.class);
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
