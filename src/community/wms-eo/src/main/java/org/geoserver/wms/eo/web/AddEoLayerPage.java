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
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wms.eo.EoCatalogBuilder;


/**
 * Wicket page to add a new layer to a WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class AddEoLayerPage extends EoPage {

    public AddEoLayerPage(LayerGroupInfo group) {
        IModel<AddEoLayerModel> model = new Model<AddEoLayerModel>(new AddEoLayerModel(group));

        // build the form
        Form<AddEoLayerModel> paramsForm = new Form<AddEoLayerModel>("addLayerForm", model);
        add(paramsForm);
        
        EoLayerGroupPanel groupPanel = new EoLayerGroupPanel("groupPanel", new PropertyModel<LayerGroupInfo>(model, "group"), 
                new ResourceModel("group", "WMS-EO Group"), true, EoLayerGroupProviderFilter.INSTANCE);
        paramsForm.add(groupPanel);

        paramsForm.add(getTextParamPanel("parameterName",  GEOPHYSICAL_PARAMETER.getObject() + " Name", model, false));
        paramsForm.add(getDirectoryPanel("parameterUrl", GEOPHYSICAL_PARAMETER.getObject() + " URL", model, false));
        paramsForm.add(getTextParamPanel("bitmaskName", BITMASK.getObject() + " Name", model, false));        
        paramsForm.add(getDirectoryPanel("bitmaskUrl", BITMASK.getObject() + " URL", model, false));
                
        // cancel / submit buttons
        AjaxSubmitLink submitLink = saveLink(paramsForm);
        paramsForm.add(new BookmarkablePageLink<EoLayerGroupPage>("cancel", EoLayerGroupPage.class));
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }
    
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
                    paramsForm.error("Field '" + BITMASK.getObject() + " Name' is required.");
                }
                
                if (StringUtils.isEmpty(model.getParameterName()) && !StringUtils.isEmpty(model.getParameterUrl())) {
                    paramsForm.error("Field '" + GEOPHYSICAL_PARAMETER.getObject() + " Name' is required.");                    
                }
                
                if (paramsForm.hasError()) {
                    target.addComponent(paramsForm);
                } else {                
                    try {
                        // load layers in group
                        group = getCatalog().getLayerGroupByName(group.getWorkspace(), group.getName());
                        
                        LayerInfo layer = builder.createEoMasksLayer(group.getWorkspace(), group.getName(), model.getBitmaskName(), model.getBitmaskUrl());
                        if (layer != null) {
                            group.getLayers().add(layer);
                            group.getStyles().add(layer.getDefaultStyle());
                        }
                        
                        layer = builder.createEoParametersLayer(group.getWorkspace(), group.getName(), model.getParameterName(), model.getParameterUrl());                    
                        if (layer != null) {
                            group.getLayers().add(layer);
                            group.getStyles().add(layer.getDefaultStyle());
                        }
        
                        CatalogBuilder catalogBuilder = new CatalogBuilder(getCatalog());
                        catalogBuilder.calculateLayerGroupBounds(group);
                        
                        getCatalog().save(group);
                        
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