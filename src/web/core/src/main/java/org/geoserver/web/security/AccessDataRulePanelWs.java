/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.impl.DataAccessRule;

public class AccessDataRulePanelWs extends Panel {

    private static final long serialVersionUID = -5609090679199229976L;

    private IModel<List<DataAccessRuleInfo>> ownModel;

    private String workspaceName;

    private String layerName;

    private AccessDataRuleListView dataAccessView;

    private WorkspaceInfo info;

    WebMarkupContainer listContainer;

    public AccessDataRulePanelWs(
            String id, IModel<WorkspaceInfo> model, IModel<List<DataAccessRuleInfo>> ownModel) {
        super(id, model);
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        this.info = model.getObject();
        this.workspaceName = manager.getWorkspaceName(info);
        this.layerName = manager.getLayerName(info);
        this.ownModel = ownModel;
        listContainer = new WebMarkupContainer("listContainer");
        listContainer.setOutputMarkupId(true);
        dataAccessView = new AccessDataRuleListView("rules", ownModel, true);
        dataAccessView.setOutputMarkupId(true);
        ownModel.setObject(dataAccessView.getList());
        listContainer.add(dataAccessView);
        listContainer.add(selectAllCheckbox());
        add(listContainer);
    }

    public void save() throws IOException {
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        Set<DataAccessRule> rules;
        Set<String> roles = manager.getAvailableRoles();
        rules = manager.getWorkspaceDataAccessRules(workspaceName);

        Set<DataAccessRule> news =
                manager.mapFrom(ownModel.getObject(), roles, workspaceName, layerName);
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

                        // allow subclasses to play on this change as well
                    }
                });
        return sa;
    }
}
