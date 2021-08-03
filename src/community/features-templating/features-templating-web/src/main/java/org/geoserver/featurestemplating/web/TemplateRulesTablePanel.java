/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import static org.geoserver.featurestemplating.web.TemplateRuleProvider.CQL_FILTER;
import static org.geoserver.featurestemplating.web.TemplateRuleProvider.NAME;
import static org.geoserver.featurestemplating.web.TemplateRuleProvider.OUTPUT_FORMAT;
import static org.geoserver.featurestemplating.web.TemplateRuleProvider.PRIORITY;
import static org.geoserver.featurestemplating.web.TemplateRuleProvider.PROFILE_FILTER;

import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.featurestemplating.configuration.TemplateLayerConfig;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class TemplateRulesTablePanel extends Panel {

    private GeoServerTablePanel<TemplateRule> table;

    private AjaxLink<Object> remove;

    private TemplateRuleConfigurationPanel configurationPanel;

    private LiveCollectionModel<TemplateRule, Set<TemplateRule>> model;

    public TemplateRulesTablePanel(String id, IModel<MetadataMap> metadataModel) {

        super(id);
        MapModel<TemplateLayerConfig> mapModelLayerConf =
                new MapModel<>(metadataModel, TemplateLayerConfig.METADATA_KEY);
        if (mapModelLayerConf.getObject() == null)
            mapModelLayerConf.setObject(new TemplateLayerConfig());
        this.model =
                LiveCollectionModel.set(
                        new PropertyModel<Set<TemplateRule>>(mapModelLayerConf, "templateRules"));
        GeoServerDataProvider<TemplateRule> dataProvider = new TemplateRuleProvider(model);
        table = new TemplateRuleTable("table", dataProvider, true);
        table.setOutputMarkupId(true);
        add(
                remove =
                        new AjaxLink<Object>("removeSelected") {
                            private static final long serialVersionUID = 2421854498051377608L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                Set<TemplateRule> rules =
                                        TemplateRulesTablePanel.this.getModel().getObject();
                                Set<TemplateRule> updated = new HashSet<>(rules);
                                updated.removeAll(table.getSelection());
                                TemplateRulesTablePanel.this.getModel().setObject(updated);
                                TemplateRulesTablePanel.this.modelChanged();
                                table.modelChanged();
                                target.add(table);
                            }
                        });
        add(table);
    }

    public class TemplateRuleTable extends GeoServerTablePanel<TemplateRule> {

        public TemplateRuleTable(
                String id, GeoServerDataProvider<TemplateRule> dataProvider, boolean selectable) {
            super(id, dataProvider, selectable);
        }

        @Override
        protected Component getComponentForProperty(
                String id,
                IModel<TemplateRule> itemModel,
                GeoServerDataProvider.Property<TemplateRule> property) {
            if (property.equals(PRIORITY)) {
                return new Label(id, PRIORITY.getModel(itemModel));
            }
            if (property.equals(NAME))
                return new SimpleAjaxLink<TemplateRule>(id, itemModel, NAME.getModel(itemModel)) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        TemplateRule rule = itemModel.getObject();
                        configurationPanel.theForm.getModel().setObject(rule);
                        configurationPanel.theForm.modelChanged();
                        String submitLabel = configurationPanel.getSubmitLabelValue("update");
                        configurationPanel.submitLabelModel.setObject(submitLabel);
                        String panelLabel = configurationPanel.getPanelLabelValue("update");
                        configurationPanel.panelLabelModel.setObject(panelLabel);
                        target.add(configurationPanel.theForm);
                        target.add(configurationPanel.submitLabel);
                        target.add(configurationPanel.panelLabel);
                    }
                };
            else if (property.equals(OUTPUT_FORMAT))
                return new Label(id, OUTPUT_FORMAT.getModel(itemModel));
            else if (property.equals(CQL_FILTER))
                return new Label(id, CQL_FILTER.getModel(itemModel));
            else if (property.equals(PROFILE_FILTER))
                return new Label(id, PROFILE_FILTER.getModel(itemModel));
            return null;
        }
    }

    public LiveCollectionModel<TemplateRule, Set<TemplateRule>> getModel() {
        return model;
    }

    public GeoServerTablePanel<TemplateRule> getTable() {
        return table;
    }

    public void setConfigurationPanel(TemplateRuleConfigurationPanel panel) {
        this.configurationPanel = panel;
    }
}
