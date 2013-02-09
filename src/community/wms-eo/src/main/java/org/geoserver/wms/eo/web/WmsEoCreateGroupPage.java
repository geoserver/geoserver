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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.PageParameters;
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
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.store.StoreNameValidator;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geoserver.wms.eo.EoCatalogBuilder;


/**
 * Wicket page to create a new WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoCreateGroupPage extends EoPage {

    public WmsEoCreateGroupPage() {
        IModel<WmsEoCreateGroupModel> model = new Model<WmsEoCreateGroupModel>(new WmsEoCreateGroupModel());

        // build the form
        Form<WmsEoCreateGroupModel> paramsForm = new Form<WmsEoCreateGroupModel>("createEoGroupForm", model);
        add(paramsForm);

        WorkspacePanel workspacePanel = new WorkspacePanel("workspacePanel", new PropertyModel<WorkspaceInfo>(model, "workspace"), 
                new ResourceModel("workspace", "Workspace"), true);
        paramsForm.add(workspacePanel);
        
        TextParamPanel namePanel = getTextParamPanel("name", "Group Name", model, true);
        paramsForm.add(namePanel);

        TextParamPanel outlineLayerNamePanel = getTextParamPanel("outlineLayerName", OUTLINE.getObject() + " Layer Name", model, false);
        addLayerPanels(paramsForm, workspacePanel, outlineLayerNamePanel, null);
        
        TextParamPanel browseLayerNamePanel = getTextParamPanel("productLayerName", PRODUCT.getObject() + " Layer Name", model, false);
        addLayerPanels(paramsForm, workspacePanel, browseLayerNamePanel, getDirectoryPanel("productUrl", PRODUCT.getObject() + " URL", model, true));
        
        TextParamPanel bandsLayerNamePanel = getTextParamPanel("bandLayerName", BAND.getObject() + " Layer Name", model, false);        
        addLayerPanels(paramsForm, workspacePanel, bandsLayerNamePanel, getDirectoryPanel("bandUrl", BAND.getObject() + " URL", model, true));
        
        TextParamPanel parametersLayerNamePanel = getTextParamPanel("parameterLayerName", GEOPHYSICAL_PARAMETER.getObject() + " Layer Name", model, false);        
        addLayerPanels(paramsForm, workspacePanel, parametersLayerNamePanel, getDirectoryPanel("parameterUrl", GEOPHYSICAL_PARAMETER.getObject() + " URL", model, false));
        
        TextParamPanel masksLayerNamePanel = getTextParamPanel("bitmaskLayerName", BITMASK.getObject() + " Layer Name", model, false); 
        addLayerPanels(paramsForm, workspacePanel, masksLayerNamePanel, getDirectoryPanel("bitmaskUrl", BITMASK.getObject() + " URL", model, false));
        
        // cancel / submit buttons
        AjaxSubmitLink submitLink = saveLink(paramsForm);
        paramsForm.add(new BookmarkablePageLink<StorePage>("cancel", WmsEoCreateGroupPage.class));
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }
        
    private void addLayerPanels(Form<WmsEoCreateGroupModel> paramsForm, WorkspacePanel workspacePanel, TextParamPanel layerNamePanel, DirectoryParamPanel directoryPanel) {
        paramsForm.add(layerNamePanel);
        if (directoryPanel != null) {
            paramsForm.add(directoryPanel);
        }
        
        paramsForm.add(new StoreNameValidator(workspacePanel.getFormComponent(), layerNamePanel.getFormComponent(), null, false));
        paramsForm.add(new NewLayerNameValidator(workspacePanel.getFormComponent(), layerNamePanel.getFormComponent(), false));        
    }
    
    private String getParameter(String value, String defaultIfNull) {
        if (StringUtils.isEmpty(value)) {
            return defaultIfNull;
        } else {
            return value;
        }
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

                if (getCatalog().getLayerGroupByName(model.getWorkspace(), model.getName()) != null) {
                    paramsForm.error("Layer Group '" + model.getName() + "' already exists in workspace '" + model.getWorkspace().getName() + "'");
                    target.addComponent(paramsForm);                    
                    return;
                }
                
                String groupName = model.getName();
                String outlineLayerName = getParameter(model.getOutlineLayerName(), groupName + " " + OUTLINE.getObject());
                String productLayerName = getParameter(model.getProductLayerName(), groupName + " " + PRODUCT.getObject());
                String bandLayerName = getParameter(model.getBandLayerName(), groupName + " " + BAND.getObject());
                String bitmaskLayerName = getParameter(model.getBitmaskLayerName(), groupName + " " + BITMASK.getObject());
                String parameterLayerName = getParameter(model.getParameterLayerName(), groupName + " " + GEOPHYSICAL_PARAMETER.getObject());
                EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                try {
                    LayerGroupInfo eoGroup = builder.createEoLayerGroup(model.getWorkspace(), 
                            groupName,
                            outlineLayerName,
                            productLayerName,
                            model.getProductUrl(),
                            bandLayerName,
                            model.getBandUrl(),
                            bitmaskLayerName,
                            model.getBitmaskUrl(),
                            parameterLayerName,
                            model.getParameterUrl());
                    
                    Map<String,String> parameters = new HashMap<String,String>();
                    parameters.put(LayerGroupEditPage.WORKSPACE, model.getWorkspace().getName());
                    parameters.put(LayerGroupEditPage.GROUP, eoGroup.getName());
                    setResponsePage(new LayerGroupEditPage(new PageParameters(parameters)));
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