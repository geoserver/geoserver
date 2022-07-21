/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Supports coverage store configuration
 *
 * @author Andrea Aime
 * @see StoreEditPanel
 */
@SuppressWarnings("serial")
abstract class AbstractWMSStorePage extends GeoServerSecuredPage {

    protected WorkspacePanel workspacePanel;

    private Form<WMSStoreInfo> form;

    GeoServerDialog dialog;

    TextParamPanel<String> capabilitiesURL;

    protected TextParamPanel<String> usernamePanel;

    protected PasswordParamPanel password;

    void initUI(final WMSStoreInfo store) {
        IModel<WMSStoreInfo> model = new Model<>(store);

        add(dialog = new GeoServerDialog("dialog"));

        // build the form
        form = new Form<>("form", model);
        add(form);

        // name
        PropertyModel<String> nameModel = new PropertyModel<>(model, "name");
        final TextParamPanel<String> namePanel =
                new TextParamPanel<>(
                        "namePanel",
                        nameModel,
                        new ResourceModel("AbstractWMSStorePage.dataSrcName", "Data Source Name"),
                        true);

        form.add(namePanel);

        // description and enabled
        form.add(
                new TextParamPanel<>(
                        "descriptionPanel",
                        new PropertyModel<>(model, "description"),
                        new ResourceModel("AbstractWMSStorePage.description", "Description"),
                        false));
        form.add(
                new CheckBoxParamPanel(
                        "enabledPanel",
                        new PropertyModel<>(model, "enabled"),
                        new ResourceModel("enabled", "Enabled")));

        form.add(
                new CheckBoxParamPanel(
                        "disableOnConnFailurePanel",
                        new PropertyModel<>(model, "disableOnConnFailure"),
                        new ResourceModel(
                                "AbstractWMSStorePage.disableOnConnFailure",
                                "Autodisable on connection failure")));

        // a custom converter will turn this into a namespace url
        workspacePanel =
                new WorkspacePanel(
                        "workspacePanel",
                        new PropertyModel<>(model, "workspace"),
                        new ResourceModel("workspace", "Workspace"),
                        true);
        form.add(workspacePanel);

        capabilitiesURL =
                new TextParamPanel<>(
                        "capabilitiesURL",
                        new PropertyModel<>(model, "capabilitiesURL"),
                        new ParamResourceModel("capabilitiesURL", this),
                        true);
        form.add(capabilitiesURL);

        // user name
        usernamePanel =
                new TextParamPanel<>(
                        "userNamePanel",
                        new PropertyModel<>(model, "username"),
                        new ResourceModel("AbstractWMSStorePage.userName"),
                        false);

        form.add(usernamePanel);

        // password
        form.add(
                password =
                        new PasswordParamPanel(
                                "passwordPanel",
                                new PropertyModel<>(model, "password"),
                                new ResourceModel("AbstractWMSStorePage.password"),
                                false));

        // max concurrent connections
        final PropertyModel<Boolean> useHttpConnectionPoolModel =
                new PropertyModel<>(model, "useConnectionPooling");
        CheckBoxParamPanel useConnectionPooling =
                new CheckBoxParamPanel(
                        "useConnectionPoolingPanel",
                        useHttpConnectionPoolModel,
                        new ResourceModel("AbstractWMSStorePage.useHttpConnectionPooling"));
        form.add(useConnectionPooling);

        PropertyModel<Integer> connectionsModel = new PropertyModel<>(model, "maxConnections");
        final TextParamPanel<Integer> maxConnections =
                new TextParamPanel<>(
                        "maxConnectionsPanel",
                        connectionsModel,
                        new ResourceModel("AbstractWMSStorePage.maxConnections"),
                        true,
                        new RangeValidator<>(1, 128));
        maxConnections.setOutputMarkupId(true);
        maxConnections.setEnabled(useHttpConnectionPoolModel.getObject());
        form.add(maxConnections);

        useConnectionPooling
                .getFormComponent()
                .add(
                        new OnChangeAjaxBehavior() {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                boolean enabled = useHttpConnectionPoolModel.getObject();
                                maxConnections.setEnabled(enabled);
                                target.add(maxConnections);
                            }
                        });

        // connect timeout
        PropertyModel<Integer> connectTimeoutModel = new PropertyModel<>(model, "connectTimeout");
        form.add(
                new TextParamPanel<>(
                        "connectTimeoutPanel",
                        connectTimeoutModel,
                        new ResourceModel("AbstractWMSStorePage.connectTimeout"),
                        true,
                        new RangeValidator<>(1, 240)));

        // read timeout
        PropertyModel<Integer> readTimeoutModel = new PropertyModel<>(model, "readTimeout");
        form.add(
                new TextParamPanel<>(
                        "readTimeoutPanel",
                        readTimeoutModel,
                        new ResourceModel("AbstractWMSStorePage.readTimeout"),
                        true,
                        new RangeValidator<>(1, 360)));

        // cancel/submit buttons
        form.add(new BookmarkablePageLink<>("cancel", StorePage.class));
        form.add(saveLink());
        form.setDefaultButton(saveLink());

        // feedback panel for error messages
        form.add(new FeedbackPanel("feedback"));

        StoreNameValidator storeNameValidator =
                new StoreNameValidator(
                        workspacePanel.getFormComponent(),
                        namePanel.getFormComponent(),
                        store.getId());
        form.add(storeNameValidator);
    }

    private AjaxSubmitLink saveLink() {
        return new AjaxSubmitLink("save", form) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.add(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                form.process(this);
                WMSStoreInfo info = (WMSStoreInfo) form.getModelObject();
                try {
                    onSave(info, target);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.add(form);
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
    protected abstract void onSave(WMSStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException;

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
