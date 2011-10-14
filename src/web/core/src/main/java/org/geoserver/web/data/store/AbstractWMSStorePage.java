/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.WMSStoreInfo;
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

    private Form form;
    
    GeoServerDialog dialog;

    TextParamPanel capabilitiesURL;

    protected TextParamPanel usernamePanel;
    
    protected PasswordParamPanel password;
    
    void initUI(final WMSStoreInfo store) {
        IModel model = new Model(store);
        
        add(dialog = new GeoServerDialog("dialog"));

        // build the form
        form = new Form("form", model);
        add(form);

        // name
        PropertyModel nameModel = new PropertyModel(model, "name");
        final TextParamPanel namePanel = new TextParamPanel("namePanel", nameModel,
                new ResourceModel("AbstractWMSStorePage.dataSrcName", "Data Source Name"), true);

        form.add(namePanel);

        // description and enabled
        form.add(new TextParamPanel("descriptionPanel", new PropertyModel(model,
                "description"), new ResourceModel("AbstractWMSStorePage.description", "Description"), false));
        form.add(new CheckBoxParamPanel("enabledPanel", new PropertyModel(model, "enabled"),
                new ResourceModel("enabled", "Enabled")));
        // a custom converter will turn this into a namespace url
        workspacePanel = new WorkspacePanel("workspacePanel",
                new PropertyModel(model, "workspace"), new ResourceModel("workspace", "Workspace"),
                true);
        form.add(workspacePanel);
        
        capabilitiesURL = new TextParamPanel("capabilitiesURL", new PropertyModel(model, "capabilitiesURL"),
                new ParamResourceModel("capabilitiesURL", AbstractWMSStorePage.this), true);
        form.add(capabilitiesURL);

        // user name
        PropertyModel userModel = new PropertyModel(model, "username");
        usernamePanel = new TextParamPanel("userNamePanel", userModel, new ResourceModel(
                "AbstractWMSStorePage.userName"), false);

        form.add(usernamePanel);

        // password
        PropertyModel passwordModel = new PropertyModel(model, "password");
        form.add(password = new PasswordParamPanel("passwordPanel", passwordModel, new ResourceModel(
                "AbstractWMSStorePage.password"), false));
        
        // max concurrent connections
        PropertyModel connectionsModel = new PropertyModel(model, "maxConnections");
        if (store.getMaxConnections() <= 0) {
            connectionsModel.setObject(Integer.valueOf(6));
        }
        form.add(new TextParamPanel("maxConnectionsPanel", connectionsModel, new ResourceModel(
                "AbstractWMSStorePage.maxConnections"), false));
        
        // cancel/submit buttons
        form.add(new BookmarkablePageLink("cancel", StorePage.class));
        form.add(saveLink());
        form.setDefaultButton(saveLink());

        // feedback panel for error messages
        form.add(new FeedbackPanel("feedback"));

        StoreNameValidator storeNameValidator = new StoreNameValidator(workspacePanel
                .getFormComponent(), namePanel.getFormComponent(), store.getId());
        form.add(storeNameValidator);
    }

    private AjaxSubmitLink saveLink() {
        return new AjaxSubmitLink("save", form) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.addComponent(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                form.process();
                WMSStoreInfo info = (WMSStoreInfo) form.getModelObject();
                try {
                    onSave(info, target);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.addComponent(form);
                }
            }
        };
    }

    /**
     * Template method for subclasses to take the appropriate action when the coverage store page
     * "save" button is pressed.
     * 
     * @param info
     *            the StoreInfo to save
     * @throws IllegalArgumentException
     *             with an appropriate error message if the save action can't be successfully
     *             performed
     */
    protected abstract void onSave(WMSStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException;

    protected void clone(final WMSStoreInfo source, WMSStoreInfo target) {
        target.setDescription(source.getDescription());
        target.setEnabled(source.isEnabled());
        target.setName(source.getName());
        target.setType(source.getType());
        target.setCapabilitiesURL(source.getCapabilitiesURL());
        target.setWorkspace(source.getWorkspace());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setMaxConnections(source.getMaxConnections());
    }

    
    
}
