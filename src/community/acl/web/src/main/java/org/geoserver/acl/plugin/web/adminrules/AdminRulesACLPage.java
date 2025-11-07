/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceServerAdminPage)
 */
package org.geoserver.acl.plugin.web.adminrules;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geoserver.acl.plugin.web.adminrules.model.AdminRuleEditModel;
import org.geoserver.acl.plugin.web.adminrules.model.AdminRulesTableDataProvider;
import org.geoserver.acl.plugin.web.adminrules.model.MutableAdminRule;
import org.geoserver.acl.plugin.web.components.RulesTablePanel;
import org.geoserver.web.GeoServerSecuredPage;

@SuppressWarnings("serial")
public class AdminRulesACLPage extends GeoServerSecuredPage {

    private AdminRulesTableDataProvider dataProvider;

    private RulesTablePanel<MutableAdminRule> rulesPanel;

    private AjaxLink<Object> removeLink;

    public AdminRulesACLPage() {
        dataProvider = new AdminRulesTableDataProvider();

        add(rulesPanel = rulesTablePanel());
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        header.add(addNewLink());
        header.add(removeLink = removeLink());
        return header;
    }

    private AjaxLink<Object> removeLink() {
        AjaxLink<Object> removeLink = new AjaxLink<>("removeSelected") {
            public @Override void onClick(AjaxRequestTarget target) {
                dataProvider.remove(rulesPanel.getSelection());
                rulesPanel.clearSelection();
                target.add(rulesPanel);
            }
        };
        removeLink.setOutputMarkupId(true);
        removeLink.setEnabled(false);
        return removeLink;
    }

    private AjaxLink<Object> addNewLink() {
        return new AjaxLink<>("addNew") {
            public @Override void onClick(AjaxRequestTarget target) {
                setResponsePage(new AdminRuleEditPage(new AdminRuleEditModel()));
            }
        };
    }

    private RulesTablePanel<MutableAdminRule> rulesTablePanel() {
        RulesTablePanel<MutableAdminRule> panel = new RulesTablePanel<>("rulesPanel", dataProvider);
        panel.setOnDrop((moved, target) -> {
            dataProvider.onDrop(moved, target);
            doReturn(AdminRulesACLPage.class);
        });
        panel.setOnSelectionUpdate(target -> {
            removeLink.setEnabled(!rulesPanel.getSelection().isEmpty());
            target.add(removeLink);
        });
        panel.setOnEdit(rule -> {
            setResponsePage(new AdminRuleEditPage(new AdminRuleEditModel(rule)));
        });
        return panel;
    }
}
