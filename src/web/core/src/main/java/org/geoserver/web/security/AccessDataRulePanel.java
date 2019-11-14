/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.IOException;
import java.util.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.*;
import org.geoserver.security.impl.DataAccessRule;

public class AccessDataRulePanel extends Panel {

    private static final long serialVersionUID = -5609090679199229976L;

    private IModel<List<DataAccessRuleInfo>> ownModel;

    private String workspaceName;

    private String layerName;

    private CatalogInfo info;

    private AccessDataRuleListView dataAccessView;

    WebMarkupContainer listContainer;

    public AccessDataRulePanel(
            String id,
            IModel<? extends CatalogInfo> model,
            IModel<List<DataAccessRuleInfo>> ownModel) {
        super(id, model);
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        this.info = model.getObject();
        this.workspaceName = manager.getWorkspaceName(info);
        this.layerName = manager.getLayerName(info);
        this.ownModel = ownModel;
        listContainer = new WebMarkupContainer("listContainer");
        listContainer.setOutputMarkupId(true);
        boolean isWs = WorkspaceInfo.class.isAssignableFrom(model.getObject().getClass());
        dataAccessView = new AccessDataRuleListView("rules", ownModel, isWs);

        Label label = new Label("adminTh", new ResourceModel("admin"));
        if (!isWs) label.setVisible(false);
        listContainer.add(label);
        listContainer.add(selectAllCheckbox());
        dataAccessView.setOutputMarkupId(true);
        ownModel.setObject(dataAccessView.getList());
        listContainer.add(dataAccessView);
        add(listContainer);
    }

    public void save() throws IOException {
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        Set<DataAccessRule> rules;
        Set<String> roles = manager.getAvailableRoles();
        rules = manager.getResourceRule(workspaceName, info);
        boolean globalLayerGroup =
                info instanceof LayerGroupInfo && workspaceName == null ? true : false;
        Set<DataAccessRule> news =
                manager.mapFrom(
                        ownModel.getObject(), roles, workspaceName, layerName, globalLayerGroup);
        manager.saveRules(rules, news);
    }

    CheckBox selectAllCheckbox() {
        CheckBox sa =
                new CheckBox(
                        "selectAll", new PropertyModel<Boolean>(this, "dataAccessView.selectAll"));
        sa.setOutputMarkupId(true);
        sa.add(
                new AjaxFormComponentUpdatingBehavior("click") {

                    private static final long serialVersionUID = 1154921156065269691L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // select all the checkboxes
                        dataAccessView.setSelection();

                        // update table and the checkbox itself
                        target.add(getComponent());
                        target.add(listContainer);
                    }
                });
        return sa;
    }
}
