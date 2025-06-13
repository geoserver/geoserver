/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import static org.geoserver.featurestemplating.web.schema.SchemaRuleProvider.CQL_FILTER;
import static org.geoserver.featurestemplating.web.schema.SchemaRuleProvider.NAME;
import static org.geoserver.featurestemplating.web.schema.SchemaRuleProvider.OUTPUT_FORMAT;
import static org.geoserver.featurestemplating.web.schema.SchemaRuleProvider.PRIORITY;
import static org.geoserver.featurestemplating.web.schema.SchemaRuleProvider.PROFILE_FILTER;

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
import org.geoserver.featurestemplating.configuration.schema.SchemaLayerConfig;
import org.geoserver.featurestemplating.configuration.schema.SchemaRule;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class SchemaRulesTablePanel extends Panel {

    private GeoServerTablePanel<SchemaRule> table;

    private AjaxLink<Object> remove;

    private SchemaRuleConfigurationPanel configurationPanel;

    private LiveCollectionModel<SchemaRule, Set<SchemaRule>> model;

    public SchemaRulesTablePanel(String id, IModel<MetadataMap> metadataModel) {

        super(id);
        MapModel<SchemaLayerConfig> mapModelLayerConf = new MapModel<>(metadataModel, SchemaLayerConfig.METADATA_KEY);
        Object object = mapModelLayerConf.getObject();
        if (!(object instanceof SchemaLayerConfig)) {
            mapModelLayerConf.setObject(new SchemaLayerConfig());
        }
        this.model = LiveCollectionModel.set(new PropertyModel<Set<SchemaRule>>(mapModelLayerConf, "schemaRules"));
        GeoServerDataProvider<SchemaRule> dataProvider = new SchemaRuleProvider(model);
        table = new SchemaRuleTable("table", dataProvider, true);
        table.setOutputMarkupId(true);
        add(
                remove = new AjaxLink<Object>("removeSelected") {
                    private static final long serialVersionUID = 2421854498051377608L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Set<SchemaRule> rules =
                                SchemaRulesTablePanel.this.getModel().getObject();
                        Set<SchemaRule> updated = new HashSet<>(rules);
                        updated.removeAll(table.getSelection());
                        SchemaRulesTablePanel.this.getModel().setObject(updated);
                        SchemaRulesTablePanel.this.modelChanged();
                        table.modelChanged();
                        target.add(table);
                    }
                });
        add(table);
    }

    public class SchemaRuleTable extends GeoServerTablePanel<SchemaRule> {

        public SchemaRuleTable(String id, GeoServerDataProvider<SchemaRule> dataProvider, boolean selectable) {
            super(id, dataProvider, selectable);
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<SchemaRule> itemModel, GeoServerDataProvider.Property<SchemaRule> property) {
            if (property.equals(PRIORITY)) {
                return new Label(id, PRIORITY.getModel(itemModel));
            }
            if (property.equals(NAME))
                return new SimpleAjaxLink<SchemaRule>(id, itemModel, NAME.getModel(itemModel)) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        SchemaRule rule = itemModel.getObject();
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
            else if (property.equals(OUTPUT_FORMAT)) return new Label(id, OUTPUT_FORMAT.getModel(itemModel));
            else if (property.equals(CQL_FILTER)) return new Label(id, CQL_FILTER.getModel(itemModel));
            else if (property.equals(PROFILE_FILTER)) return new Label(id, PROFILE_FILTER.getModel(itemModel));
            return null;
        }
    }

    public LiveCollectionModel<SchemaRule, Set<SchemaRule>> getModel() {
        return model;
    }

    public GeoServerTablePanel<SchemaRule> getTable() {
        return table;
    }

    public void setConfigurationPanel(SchemaRuleConfigurationPanel panel) {
        this.configurationPanel = panel;
    }
}
