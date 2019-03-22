/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.WMTSStoreInfo;
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
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 * @see StoreEditPanel
 */
@SuppressWarnings("serial")
abstract class AbstractWMTSStorePage extends GeoServerSecuredPage {

    protected WorkspacePanel workspacePanel;

    private Form form;

    GeoServerDialog dialog;

    TextParamPanel capabilitiesURL;

    protected TextParamPanel usernamePanel;
    protected PasswordParamPanel password;

    protected TextParamPanel headerNamePanel;
    protected TextParamPanel headerValuePanel;

    void initUI(final WMTSStoreInfo store) {
        IModel model = new Model(store);

        add(dialog = new GeoServerDialog("dialog"));

        // build the form
        form = new Form("form", model);
        add(form);

        // name
        PropertyModel nameModel = new PropertyModel(model, "name");
        final TextParamPanel namePanel =
                new TextParamPanel(
                        "namePanel",
                        nameModel,
                        new ResourceModel("AbstractWMTSStorePage.dataSrcName", "Data Source Name"),
                        true);

        form.add(namePanel);

        // description and enabled
        form.add(
                new TextParamPanel(
                        "descriptionPanel",
                        new PropertyModel(model, "description"),
                        new ResourceModel("AbstractWMTSStorePage.description", "Description"),
                        false));
        form.add(
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
        form.add(workspacePanel);

        capabilitiesURL =
                new TextParamPanel(
                        "capabilitiesURL",
                        new PropertyModel(model, "capabilitiesURL"),
                        new ParamResourceModel("capabilitiesURL", this),
                        true);
        form.add(capabilitiesURL);

        // user name
        PropertyModel userModel = new PropertyModel(model, "username");
        usernamePanel =
                new TextParamPanel(
                        "userNamePanel",
                        userModel,
                        new ResourceModel("AbstractWMTSStorePage.userName"),
                        false);

        form.add(usernamePanel);

        // password
        PropertyModel passwordModel = new PropertyModel(model, "password");
        form.add(
                password =
                        new PasswordParamPanel(
                                "passwordPanel",
                                passwordModel,
                                new ResourceModel("AbstractWMTSStorePage.password"),
                                false));

        // http header
        PropertyModel headerNameModel = new PropertyModel(model, "headerName");
        headerNamePanel =
                new TextParamPanel(
                        "headerNamePanel",
                        headerNameModel,
                        new ResourceModel("AbstractWMTSStorePage.headerName"),
                        false);
        form.add(headerNamePanel);

        PropertyModel headerValueModel = new PropertyModel(model, "headerValue");
        headerValuePanel =
                new TextParamPanel(
                        "headerValuePanel",
                        headerValueModel,
                        new ResourceModel("AbstractWMTSStorePage.headerValue"),
                        false);
        form.add(headerValuePanel);

        // max concurrent connections
        final PropertyModel<Boolean> useHttpConnectionPoolModel =
                new PropertyModel<Boolean>(model, "useConnectionPooling");
        CheckBoxParamPanel useConnectionPooling =
                new CheckBoxParamPanel(
                        "useConnectionPoolingPanel",
                        useHttpConnectionPoolModel,
                        new ResourceModel("AbstractWMTSStorePage.useHttpConnectionPooling"));
        form.add(useConnectionPooling);

        PropertyModel<String> connectionsModel = new PropertyModel<String>(model, "maxConnections");
        final TextParamPanel maxConnections =
                new TextParamPanel(
                        "maxConnectionsPanel",
                        connectionsModel,
                        new ResourceModel("AbstractWMTSStorePage.maxConnections"),
                        true,
                        new RangeValidator<Integer>(1, 128));
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
        PropertyModel<Integer> connectTimeoutModel =
                new PropertyModel<Integer>(model, "connectTimeout");
        form.add(
                new TextParamPanel(
                        "connectTimeoutPanel",
                        connectTimeoutModel,
                        new ResourceModel("AbstractWMTSStorePage.connectTimeout"),
                        true,
                        new RangeValidator<Integer>(1, 240)));

        // read timeout
        PropertyModel<Integer> readTimeoutModel = new PropertyModel<Integer>(model, "readTimeout");
        form.add(
                new TextParamPanel(
                        "readTimeoutPanel",
                        readTimeoutModel,
                        new ResourceModel("AbstractWMTSStorePage.readTimeout"),
                        true,
                        new RangeValidator<Integer>(1, 360)));

        // cancel/submit buttons
        form.add(new BookmarkablePageLink("cancel", StorePage.class));
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
                WMTSStoreInfo info = (WMTSStoreInfo) form.getModelObject();
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
    protected abstract void onSave(WMTSStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException;

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
