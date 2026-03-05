/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

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
import org.geoserver.featurestemplating.configuration.FeatureTypeTemplateDAOListener;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.web.publish.PublishedEditTabPanel;

public class TemplateRulesTabPanel extends PublishedEditTabPanel<LayerInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(TemplateRulesTabPanel.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    public TemplateRuleConfigurationPanel configurationPanel;
    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    public TemplateRulesTabPanel(String id, IModel<LayerInfo> model) {
        super(id, model);
        LayerInfo li = model.getObject();
        ResourceInfo ri = li.getResource();
        if (!(ri instanceof FeatureTypeInfo)) {
            configurationPanel.setEnabled(false);
        }
        TemplateInfoDAO infoDao = TemplateInfoDAO.get();
        FeatureTypeTemplateDAOListener listener = new FeatureTypeTemplateDAOListener((FeatureTypeInfo) ri);
        infoDao.addTemplateListener(listener);
        PropertyModel<ResourceInfo> resource = new PropertyModel<>(model, "resource");
        PropertyModel<MetadataMap> metadata = new PropertyModel<>(resource, "metadata");
        TemplateRulesTablePanel tablePanel = new TemplateRulesTablePanel("rulesTable", metadata);
        tablePanel.setOutputMarkupId(true);
        add(tablePanel);
        configurationPanel = new TemplateRuleConfigurationPanel(
                "ruleConfiguration", new CompoundPropertyModel<>(new TemplateRule()), false, li);
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
        TemplateRule ruleModel = configurationPanel.templateRuleModel.getObject();
        Set<TemplateRule> rules =
                new HashSet<>(configurationPanel.tablePanel.getModel().getObject());
        rules.add(ruleModel);
        configurationPanel.tablePanel.getModel().setObject(rules);

        super.save();
    }
}
