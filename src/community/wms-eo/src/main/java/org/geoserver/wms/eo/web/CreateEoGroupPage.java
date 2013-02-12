/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geoserver.wms.eo.EoCatalogBuilder;


/**
 * Wicket page to create a new WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class CreateEoGroupPage extends EoPage {

    public CreateEoGroupPage() {
        IModel<CreateEoGroupModel> model = new Model<CreateEoGroupModel>(new CreateEoGroupModel());

        // build the form
        Form<CreateEoGroupModel> paramsForm = new Form<CreateEoGroupModel>("createEoGroupForm", model);
        add(paramsForm);

        WorkspacePanel workspacePanel = new WorkspacePanel("workspacePanel", new PropertyModel<WorkspaceInfo>(model, "workspace"), 
                new ResourceModel("workspace", "Workspace"), false);
        paramsForm.add(workspacePanel);
        
        paramsForm.add(getTextParamPanel("groupName", "Name", model, true));
        paramsForm.add(getTextParamPanel("groupTitle", "Title", model, false));
        
        paramsForm.add(getDirectoryPanel("browseImageUrl", BROWSE_IMAGE.getObject() + " URL", model, true));
        
        paramsForm.add(getDirectoryPanel("bandUrl", BAND.getObject() + " URL", model, true));
        
        paramsForm.add(getTextParamPanel("parameterName", GEOPHYSICAL_PARAMETER.getObject() + " Name", model, false));
        paramsForm.add(getDirectoryPanel("parameterUrl", GEOPHYSICAL_PARAMETER.getObject() + " URL", model, false));
        
        paramsForm.add(getTextParamPanel("bitmaskName", BITMASK.getObject() + " Name", model, false)); 
        paramsForm.add(getDirectoryPanel("bitmaskUrl", BITMASK.getObject() + " URL", model, false));
        
        // cancel / submit buttons
        AjaxSubmitLink submitLink = saveLink(paramsForm);
        paramsForm.add(new BookmarkablePageLink<StorePage>("cancel", EoLayerGroupPage.class));
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }
    
    private AjaxSubmitLink saveLink(Form<CreateEoGroupModel> paramsForm) {
        return new AjaxSubmitLink("save", paramsForm) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> paramsForm) {
                super.onError(target, paramsForm);
                target.addComponent(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> paramsForm) {
                CreateEoGroupModel model = (CreateEoGroupModel) paramsForm.getModelObject();

                if (getCatalog().getLayerGroupByName(model.getWorkspace(), model.getGroupName()) != null) {
                    paramsForm.error("Layer Group '" + model.getGroupName() + "' already exists");
                }                                    
                
                if (StringUtils.isEmpty(model.getBitmaskName()) && !StringUtils.isEmpty(model.getBitmaskUrl())) {
                    paramsForm.error("Field '" + BITMASK.getObject() + " Name' is required.");
                }
                
                if (StringUtils.isEmpty(model.getParameterName()) && !StringUtils.isEmpty(model.getParameterUrl())) {
                    paramsForm.error("Field '" + GEOPHYSICAL_PARAMETER.getObject() + " Name' is required.");                    
                }
                
                // TODO check if some layer already exists
                
                if (paramsForm.hasError()) {
                    target.addComponent(paramsForm);
                } else {                
                    EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                    try {
                        builder.createEoLayerGroup(model.getWorkspace(), 
                                model.getGroupName(),
                                model.getGroupTitle(),
                                model.getBrowseImageUrl(),
                                model.getBandUrl(),
                                model.getBitmaskName(),
                                model.getBitmaskUrl(),
                                model.getParameterName(),
                                model.getParameterUrl());
                        setResponsePage(EoLayerGroupPage.class);
                    } catch (RuntimeException e) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO, e.getMessage(), e);
                        }
                        
                        paramsForm.error(e.getMessage());
                        target.addComponent(paramsForm);
                    } catch (Exception e) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.log(Level.INFO, e.getMessage(), e);
                        }
                        
                        paramsForm.error(e.getMessage());                    
                        target.addComponent(paramsForm);
                    }
                }
            }
        };
    }    
}