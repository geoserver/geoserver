/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wms.eo.EoCatalogBuilder;
import org.geotools.util.logging.Logging;


/**
 * Wicket panel to add a new layer to a WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public abstract class AddEoLayerPanel extends Panel {

    private static final Logger LOGGER = Logging.getLogger(AddEoLayerPanel.class);
    
    public AddEoLayerPanel(String id, LayerGroupInfo group) {
        super(id);
        
        IModel<AddEoLayerModel> model = new Model<AddEoLayerModel>(new AddEoLayerModel(group));

        // build the form
        Form<AddEoLayerModel> paramsForm = new Form<AddEoLayerModel>("addLayerForm", model);
        add(paramsForm);
        
        paramsForm.add(EoPage.getTextParamPanel("parameterName",  EoPage.GEOPHYSICAL_PARAMETER.getObject() + " Name", model, false));
        paramsForm.add(EoPage.getDirectoryPanel("parameterUrl", EoPage.GEOPHYSICAL_PARAMETER.getObject() + " URL", model, false));
        paramsForm.add(EoPage.getTextParamPanel("bitmaskName", EoPage.BITMASK.getObject() + " Name", model, false));        
        paramsForm.add(EoPage.getDirectoryPanel("bitmaskUrl", EoPage.BITMASK.getObject() + " URL", model, false));
                
        // cancel / submit buttons
        
        AjaxSubmitLink submitLink = saveLink(paramsForm);
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);
        
        paramsForm.add(new AjaxLink("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onCancel(target);
            }
        });
        
        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }
    
    protected abstract void onCancel(AjaxRequestTarget target);
    
    protected abstract void onSubmit(AjaxRequestTarget target, LayerInfo maskLayer, LayerInfo paramsLayer);
    
    private AjaxSubmitLink saveLink(Form<AddEoLayerModel> paramsForm) {
        return new AjaxSubmitLink("save", paramsForm) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> paramsForm) {
                super.onError(target, paramsForm);
                target.addComponent(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> paramsForm) {
                AddEoLayerModel model = (AddEoLayerModel) paramsForm.getModelObject();
                LayerGroupInfo group = model.getGroup();
                
                EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                
                if (StringUtils.isEmpty(model.getBitmaskName()) && !StringUtils.isEmpty(model.getBitmaskUrl())) {
                    paramsForm.error("Field '" + EoPage.BITMASK.getObject() + " Name' is required.");
                }
                
                if (StringUtils.isEmpty(model.getParameterName()) && !StringUtils.isEmpty(model.getParameterUrl())) {
                    paramsForm.error("Field '" + EoPage.GEOPHYSICAL_PARAMETER.getObject() + " Name' is required.");                    
                }
                
                if (paramsForm.hasError()) {
                    target.addComponent(paramsForm);
                } else {                
                    try {
                        LayerInfo maskLayer = builder.createEoMasksLayer(group.getWorkspace(), group.getName(), model.getBitmaskName(), model.getBitmaskUrl());                        
                        LayerInfo paramsLayer = builder.createEoParametersLayer(group.getWorkspace(), group.getName(), model.getParameterName(), model.getParameterUrl());                    
        
                        AddEoLayerPanel.this.onSubmit(target, maskLayer, paramsLayer);                        
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
    
    protected Catalog getCatalog() {
        return getGeoServerApplication().getCatalog();
    }
    
    private GeoServerApplication getGeoServerApplication() {
        return (GeoServerApplication) getApplication();
    }    
}