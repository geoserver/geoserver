/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.opensearch.eo.security.EOAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geoserver.security.web.role.RoleNamesModel;
import org.geoserver.security.web.role.RolePaletteFormComponent;

/**
 * Panel for editing EO access limits (both collection and product), used in both the add and edit dialogs of the
 * {@link OSEOSecurityPage}
 */
public class OSEOLimitPanel extends Panel {

    public OSEOLimitPanel(String id, Model<EOAccessLimitInfo> model) {
        super(id, model);

        Form<EOAccessLimitInfo> form = new Form<>("form", model);
        add(form);

        // collection is used only for products
        WebMarkupContainer collectionContainer = createCollectionEditor(model);
        form.add(collectionContainer);

        TextArea<String> cqlFilter = createFilterEditor(model);
        form.add(cqlFilter);

        RolePaletteFormComponent roles =
                new RolePaletteFormComponent("roles", new RoleNamesModel(new PropertyModel<>(model, "roles")));
        roles.hideAddRoleLink();
        roles.getPalette().setRequired(true);
        form.add(roles);

        form.add(createFeedbackPanel());
    }

    private static TextArea<String> createFilterEditor(Model<EOAccessLimitInfo> model) {
        TextArea<String> cqlFilter = new TextArea<>("cqlFilter", new PropertyModel<>(model, "cqlFilter"));
        cqlFilter.setRequired(true);
        cqlFilter.add(new EOCQLFilterValidator(model.getObject() instanceof EOProductAccessLimitInfo));
        return cqlFilter;
    }

    private WebMarkupContainer createCollectionEditor(Model<EOAccessLimitInfo> model) {
        boolean isProduct = model.getObject() instanceof EOProductAccessLimitInfo;
        WebMarkupContainer collectionContainer = new WebMarkupContainer("collectionContainer");
        collectionContainer.setVisible(isProduct);
        DropDownChoice<String> collection =
                new DropDownChoice<>("collection", new PropertyModel<>(model, "collection"), new EOCollectionsModel());
        collection.setRequired(isProduct);
        collectionContainer.add(collection);
        return collectionContainer;
    }

    private FeedbackPanel createFeedbackPanel() {
        FeedbackPanel feedbackPanel = new FeedbackPanel("dialogFeedback");
        feedbackPanel.setOutputMarkupId(true);
        return feedbackPanel;
    }
}
