/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.data;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.SelectionDataRuleRemovalLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * A page listing data access rules, allowing for removal, addition and linking to an edit page
 */
@SuppressWarnings("serial")
public class DataAccessRulePage extends GeoServerSecuredPage {

    private GeoServerTablePanel<DataAccessRule> rules;

    private SelectionDataRuleRemovalLink removal;

    GeoServerDialog dialog;

    public DataAccessRulePage() {
        DataAccessRuleProvider provider = new DataAccessRuleProvider();
        add(rules = new GeoServerTablePanel<DataAccessRule>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<DataAccessRule> property) {
                if (property == DataAccessRuleProvider.RULEKEY) {
                    return editRuleLink(id, itemModel, property);
                }
                if (property == DataAccessRuleProvider.ROLES) {
                    return new Label(id, property.getModel(itemModel));
                }
                throw new RuntimeException("Uknown property " + property);
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(rules.getSelection().size() > 0);
                target.addComponent(removal);
            }
        });

        rules.setOutputMarkupId(true);
        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    Component editRuleLink(String id, IModel itemModel, Property<DataAccessRule> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(new EditDataAccessRulePage((DataAccessRule) getDefaultModelObject()));
            }

        };
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewDataAccessRulePage.class));

        // the removal button
        header.add(removal = new SelectionDataRuleRemovalLink("removeSelected", rules, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }
}
