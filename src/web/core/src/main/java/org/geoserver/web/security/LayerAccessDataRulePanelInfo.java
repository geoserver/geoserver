/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.util.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.catalog.*;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.publish.CommonPublishedEditTabPanelInfo;

public class LayerAccessDataRulePanelInfo extends CommonPublishedEditTabPanelInfo {

    private static final long serialVersionUID = -2621468069548681109L;

    @Override
    public ListModel<DataAccessRuleInfo> createOwnModel(
            final IModel<? extends PublishedInfo> layerModel, final boolean isNew) {
        CatalogInfo info = layerModel.getObject();
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        String workspaceName = manager.getWorkspaceName(info);
        String layerName = manager.getLayerName(info);
        Set<String> authorities = manager.getAvailableRoles();
        Set<DataAccessRule> rules = manager.getResourceRule(workspaceName, info);
        List<DataAccessRuleInfo> modelRules =
                manager.mapTo(rules, authorities, workspaceName, layerName);
        return new ListModel<>(modelRules);
    }

    @Override
    public boolean supports(PublishedInfo pi) {
        return getPublishedInfoClass().isAssignableFrom(pi.getClass())
                && AccessDataRuleInfoManager.canAccess();
    }
}
