/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaRule;
import org.geoserver.featurestemplating.configuration.schema.SchemaTypeTemplateDAOListener;
import org.geoserver.web.publish.PublishedEditTabPanel;

public class SchemaRulesTabPanel extends PublishedEditTabPanel<LayerInfo> {

    public SchemaRuleConfigurationPanel configurationPanel;
    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    public SchemaRulesTabPanel(String id, IModel<LayerInfo> model) {
        super(id, model);
        LayerInfo li = model.getObject();
        ResourceInfo ri = li.getResource();
        if (!(ri instanceof FeatureTypeInfo)) {
            configurationPanel.setEnabled(false);
        }
        SchemaInfoDAO infoDao = SchemaInfoDAO.get();
        SchemaTypeTemplateDAOListener listener = new SchemaTypeTemplateDAOListener((FeatureTypeInfo) ri);
        infoDao.addTemplateListener(listener);
        PropertyModel<ResourceInfo> resource = new PropertyModel<>(model, "resource");
        PropertyModel<MetadataMap> metadata = new PropertyModel<>(resource, "metadata");
        SchemaRulesTablePanel tablePanel = new SchemaRulesTablePanel("schemaRules", metadata);
        tablePanel.setOutputMarkupId(true);
        add(tablePanel);
        configurationPanel = new SchemaRuleConfigurationPanel(
                "schemaRuleConfiguration", new CompoundPropertyModel<>(new SchemaRule()), false, li);
        configurationPanel.setTemplateRuleTablePanel(tablePanel);
        configurationPanel.setOutputMarkupId(true);
        tablePanel.setConfigurationPanel(configurationPanel);
        add(configurationPanel);
    }

    @Override
    public void beforeSave() {
        super.beforeSave();
    }

    @Override
    public void save() throws IOException {
        SchemaRule ruleModel = configurationPanel.schemaRuleModel.getObject();
        Set<SchemaRule> rules =
                new HashSet<>(configurationPanel.tablePanel.getModel().getObject());
        rules.add(ruleModel);
        configurationPanel.tablePanel.getModel().setObject(rules);

        super.save();
    }
}
