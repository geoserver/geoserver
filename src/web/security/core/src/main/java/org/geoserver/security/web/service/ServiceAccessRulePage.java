/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** A page listing data access rules, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class ServiceAccessRulePage extends AbstractSecurityPage {

    private GeoServerTablePanel<ServiceAccessRule> rules;

    private SelectionServiceRemovalLink removal;

    public ServiceAccessRulePage() {

        ServiceAccessRuleProvider provider = new ServiceAccessRuleProvider();
        add(
                rules =
                        new GeoServerTablePanel<ServiceAccessRule>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<ServiceAccessRule> itemModel,
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
                                target.add(removal);
                            }
                        });
        rules.setOutputMarkupId(true);

        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(
                new BookmarkablePageLink<NewServiceAccessRulePage>(
                        "addNew", NewServiceAccessRulePage.class));

        // the removal button
        header.add(removal = new SelectionServiceRemovalLink("removeSelected", rules, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component editRuleLink(String id, IModel itemModel, Property<ServiceAccessRule> property) {
        return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditServiceAccessRulePage((ServiceAccessRule) getDefaultModelObject()));
            }
        };
    }
}
