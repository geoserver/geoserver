/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.eo.web;

import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.store.StoreNameValidator;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.wms.eo.EoCatalogBuilder;


/**
 * Wicket page to create a new WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoCreateGroupPage extends GeoServerSecuredPage {

    public WmsEoCreateGroupPage() {
        IModel<WmsEoCreateGroupModel> model = new Model<WmsEoCreateGroupModel>(new WmsEoCreateGroupModel());

        // build the form
        Form<WmsEoCreateGroupModel> paramsForm = new Form<WmsEoCreateGroupModel>("createEoGroupForm", model);
        add(paramsForm);
        
        TextParamPanel namePanel = new TextParamPanel("namePanel", new PropertyModel<String>(model, "name"), 
                new ResourceModel("name", "Name"), true);
        paramsForm.add(namePanel);

        WorkspacePanel workspacePanel = new WorkspacePanel("workspacePanel", new PropertyModel<WorkspaceInfo>(model, "workspace"), 
                new ResourceModel("workspace", "Workspace"), true);
        paramsForm.add(workspacePanel);

        paramsForm.add(new StoreNameValidator(workspacePanel.getFormComponent(), namePanel.getFormComponent(), null));
        
        paramsForm.add(getDirectoryPanel("browseImageUrl", "Browse Image URL", model, true));
        paramsForm.add(getDirectoryPanel("bandsUrl", "Bands URL", model, true));
        paramsForm.add(getDirectoryPanel("parametersUrl", "Parameters URL", model, false));
        paramsForm.add(getDirectoryPanel("masksUrl", "Masks URL", model, false));
                
        // cancel / submit buttons
        AjaxSubmitLink submitLink = saveLink(paramsForm);
        paramsForm.add(new BookmarkablePageLink<StorePage>("cancel", WmsEoCreateGroupPage.class));
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }
    
    private DirectoryParamPanel getDirectoryPanel(String name, String label, IModel<WmsEoCreateGroupModel> model, boolean required) {
        return new DirectoryParamPanel(name, new PropertyModel<String>(model, name), 
                new ResourceModel(name, label), required, new FileExistsValidator());
    }
    
    private AjaxSubmitLink saveLink(Form<WmsEoCreateGroupModel> paramsForm) {
        return new AjaxSubmitLink("save", paramsForm) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> paramsForm) {
                super.onError(target, paramsForm);
                target.addComponent(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> paramsForm) {
                WmsEoCreateGroupModel model = (WmsEoCreateGroupModel) paramsForm.getModelObject();
                EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                try {
                    LayerGroupInfo eoGroup = builder.createEoLayerGroup(model.getWorkspace(), model.getName(), 
                            model.getBrowseImageUrl(),
                            model.getBandsUrl(),
                            model.getMasksUrl(),
                            model.getParametersUrl());
                    
                    setResponsePage(new LayerGroupEditPage(model.getWorkspace().getName(), eoGroup.getName()));
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
        };
    }    
}