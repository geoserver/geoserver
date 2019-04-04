/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.coverage.grid.io.AbstractGridFormat;

/**
 * Supports coverage store configuration
 *
 * @author Andrea Aime
 * @see StoreEditPanel
 */
@SuppressWarnings("serial")
abstract class AbstractCoverageStorePage extends GeoServerSecuredPage {

    protected WorkspacePanel workspacePanel;

    private Form paramsForm;

    void initUI(final CoverageStoreInfo store) {
        AbstractGridFormat format = store.getFormat();
        if (format == null) {
            String msg = "Coverage Store factory not found";
            msg =
                    (String)
                            new ResourceModel(
                                            "CoverageStoreEditPage.cantGetCoverageStoreFactory",
                                            msg)
                                    .getObject();
            throw new IllegalArgumentException(msg);
        }

        IModel<CoverageStoreInfo> model = new Model<CoverageStoreInfo>(store);

        // build the form
        paramsForm = new Form("rasterStoreForm", model);
        add(paramsForm);

        // the format description labels
        paramsForm.add(new Label("storeType", format.getName()));
        paramsForm.add(new Label("storeTypeDescription", format.getDescription()));

        // name
        PropertyModel nameModel = new PropertyModel(model, "name");
        final TextParamPanel namePanel =
                new TextParamPanel(
                        "namePanel",
                        nameModel,
                        new ResourceModel(
                                "AbstractCoverageStorePage.dataSrcName", "Data Source Name"),
                        true);

        paramsForm.add(namePanel);

        // description and enabled
        paramsForm.add(
                new TextParamPanel(
                        "descriptionPanel",
                        new PropertyModel(model, "description"),
                        new ResourceModel("AbstractCoverageStorePage.description", "Description"),
                        false));
        paramsForm.add(
                new CheckBoxParamPanel(
                        "enabledPanel",
                        new PropertyModel(model, "enabled"),
                        new ResourceModel("enabled", "Enabled")));
        // a custom converter will turn this into a namespace url
        workspacePanel =
                new WorkspacePanel(
                        "workspacePanel",
                        new PropertyModel(model, "workspace"),
                        new ResourceModel("workspace", "Workspace"),
                        true);
        paramsForm.add(workspacePanel);

        final StoreEditPanel storeEditPanel;
        {
            /*
             * Here's where the extension point is applied in order to give extensions a chance to
             * provide custom behavior/components for the coverage form other than the default
             * single "url" input field
             */
            GeoServerApplication app = getGeoServerApplication();
            storeEditPanel =
                    StoreExtensionPoints.getStoreEditPanel(
                            "parametersPanel", paramsForm, store, app);
        }
        paramsForm.add(storeEditPanel);

        // cancel/submit buttons
        paramsForm.add(new BookmarkablePageLink<StorePage>("cancel", StorePage.class));
        paramsForm.add(saveLink());
        paramsForm.setDefaultButton(saveLink());

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));

        StoreNameValidator storeNameValidator =
                new StoreNameValidator(
                        workspacePanel.getFormComponent(),
                        namePanel.getFormComponent(),
                        store.getId());
        paramsForm.add(storeNameValidator);
    }

    private AjaxSubmitLink saveLink() {
        return new AjaxSubmitLink("save", paramsForm) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.add(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                CoverageStoreInfo info = (CoverageStoreInfo) form.getModelObject();
                try {
                    onSave(info, target);
                } catch (IllegalArgumentException e) {
                    paramsForm.error(e.getMessage());
                    target.add(paramsForm);
                }
            }
        };
    }

    /**
     * Template method for subclasses to take the appropriate action when the coverage store page
     * "save" button is pressed.
     *
     * @param info the StoreInfo to save
     * @throws IllegalArgumentException with an appropriate error message if the save action can't
     *     be successfully performed
     */
    protected abstract void onSave(CoverageStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException;

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
