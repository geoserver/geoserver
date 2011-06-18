/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.service;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.SelectionServiceRemovalLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * A page listing data access rules, allowing for removal, addition and linking to an edit page
 */
@SuppressWarnings("serial")
public class ServiceAccessRulePage extends GeoServerSecuredPage {

    private GeoServerTablePanel<ServiceAccessRule> rules;
    
    private SelectionServiceRemovalLink removal;

    GeoServerDialog dialog;

    public ServiceAccessRulePage() {
        ServiceAccessRuleProvider provider = new ServiceAccessRuleProvider();
        add(rules = new GeoServerTablePanel<ServiceAccessRule>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<ServiceAccessRule> property) {
                if (property == ServiceAccessRuleProvider.RULEKEY) {
                    return editRuleLink(id, itemModel, property);
                }
                if (property == ServiceAccessRuleProvider.ROLES) {
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
        
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());

    }
    
    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink("addNew", NewServiceAccessRulePage.class));

        // the removal button
        header.add(removal = new SelectionServiceRemovalLink("removeSelected", rules, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    AjaxLink addRuleLink() {
        return new AjaxLink("addRule", new Model()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new NewServiceAccessRulePage());
            }

        };
    }

    Component editRuleLink(String id, IModel itemModel, Property<ServiceAccessRule> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(new EditServiceAccessRulePage((ServiceAccessRule) getDefaultModelObject()));
            }

        };
    }

}
