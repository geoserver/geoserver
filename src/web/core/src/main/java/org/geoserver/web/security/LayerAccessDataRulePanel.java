/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.publish.PublishedEditTabPanel;

public class LayerAccessDataRulePanel extends PublishedEditTabPanel<PublishedInfo> {
    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    private AccessDataRulePanel dataAccessPanel;

    private IModel<List<DataAccessRuleInfo>> ownModel;

    private IModel<? extends PublishedInfo> model;

    public LayerAccessDataRulePanel(
            String id,
            IModel<? extends PublishedInfo> model,
            IModel<List<DataAccessRuleInfo>> ownModel) {
        super(id, model);
        this.model = model;
        this.ownModel = ownModel;
        add(dataAccessPanel = new AccessDataRulePanel("dataAccessPanel", model, ownModel));
    }

    @Override
    public void save() throws IOException {
        dataAccessPanel.save();
    }

    public IModel<List<DataAccessRuleInfo>> getOwnModel() {
        return ownModel;
    }

    public void setOwnModel(IModel<List<DataAccessRuleInfo>> ownModel) {
        this.ownModel = ownModel;
    }

    public void reloadOwnModel() {
        LayerGroupInfo group = (LayerGroupInfo) model.getObject();
        AccessDataRuleInfoManager infoManager = new AccessDataRuleInfoManager();
        String wsName = group.getWorkspace() != null ? group.getWorkspace().getName() : null;
        Set<DataAccessRule> rules = infoManager.getResourceRule(wsName, group);
        ownModel.setObject(
                infoManager.mapTo(rules, infoManager.getAvailableRoles(), wsName, group.getName()));
    }
}
