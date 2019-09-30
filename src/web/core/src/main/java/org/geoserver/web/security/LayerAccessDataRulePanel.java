/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.publish.PublishedEditTabPanel;

public class LayerAccessDataRulePanel extends PublishedEditTabPanel<PublishedInfo> {
    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    private AccessDataRulePanelPublished dataAccessPanel;

    public LayerAccessDataRulePanel(
            String id,
            IModel<? extends PublishedInfo> model,
            IModel<List<DataAccessRuleInfo>> ownModel) {
        super(id, model);
        add(dataAccessPanel = new AccessDataRulePanelPublished("dataAccessPanel", model, ownModel));
    }

    @Override
    public void save() throws IOException {
        dataAccessPanel.save();
    }
}
