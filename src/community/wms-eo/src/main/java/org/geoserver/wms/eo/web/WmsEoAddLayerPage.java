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
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.wms.eo.EoCatalogBuilder;
import org.geoserver.wms.eo.EoLayerType;


/**
 * Wicket page to add a new layer to a WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoAddLayerPage extends EoPage {

    public WmsEoAddLayerPage() {
        IModel<WmsEoAddLayerModel> model = new Model<WmsEoAddLayerModel>(new WmsEoAddLayerModel());

        // build the form
        Form<WmsEoAddLayerModel> paramsForm = new Form<WmsEoAddLayerModel>("addLayerForm", model);
        add(paramsForm);
        
        LayerGroupPanel groupPanel = new LayerGroupPanel("groupPanel", new PropertyModel<LayerGroupInfo>(model, "group"), 
                new ResourceModel("group", "WMS-EO Group"), true, new LayerGroupInfoFilter() {
                    @Override
                    public boolean accept(LayerGroupInfo group) {
                        return LayerGroupInfo.Mode.EO.equals(group.getMode());
                    }            
        });
        paramsForm.add(groupPanel);

        paramsForm.add(getTextParamPanel("parameterLayerName",  GEOPHYSICAL_PARAMETER.getObject() + " Layer Name", model, false));
        paramsForm.add(getDirectoryPanel("parameterUrl", GEOPHYSICAL_PARAMETER.getObject() + " URL", model, false));
        paramsForm.add(getTextParamPanel("bitmaskLayerName", BITMASK.getObject() + " Layer Name", model, false));        
        paramsForm.add(getDirectoryPanel("bitmaskUrl", BITMASK.getObject() + " URL", model, false));
                
        // cancel / submit buttons
        AjaxSubmitLink submitLink = saveLink(paramsForm);
        paramsForm.add(new BookmarkablePageLink<StorePage>("cancel", WmsEoAddLayerPage.class));
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }
    
    private AjaxSubmitLink saveLink(Form<WmsEoAddLayerModel> paramsForm) {
        return new AjaxSubmitLink("save", paramsForm) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> paramsForm) {
                super.onError(target, paramsForm);
                target.addComponent(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> paramsForm) {
                WmsEoAddLayerModel model = (WmsEoAddLayerModel) paramsForm.getModelObject();
                LayerGroupInfo group = model.getGroup();
                
                EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                
                if (StringUtils.isEmpty(model.getBitmaskLayerName()) && !StringUtils.isEmpty(model.getBitmaskUrl())) {
                    paramsForm.error("Field '" + BITMASK.getObject() + " Layer Name' is required.");
                }
                
                if (StringUtils.isEmpty(model.getParameterLayerName()) && !StringUtils.isEmpty(model.getParameterUrl())) {
                    paramsForm.error("Field '" + GEOPHYSICAL_PARAMETER.getObject() + " Layer Name' is required.");                    
                }
                
                if (paramsForm.hasError()) {
                    target.addComponent(paramsForm);
                } else {                
                    try {
                        // load layers in group
                        group = getCatalog().getLayerGroupByName(group.getWorkspace(), group.getName());
                        
                        LayerInfo layer = builder.createEoMosaicLayer(group.getWorkspace(), model.getBitmaskLayerName(), 
                                EoLayerType.BITMASK, model.getBitmaskUrl());
                        if (layer != null) {
                            group.getLayers().add(layer);
                            group.getStyles().add(layer.getDefaultStyle());
                        }
                        
                        layer = builder.createEoMosaicLayer(group.getWorkspace(), model.getParameterLayerName(), 
                                EoLayerType.GEOPHYSICAL_PARAMETER, model.getParameterUrl());                    
                        if (layer != null) {
                            group.getLayers().add(layer);
                            group.getStyles().add(layer.getDefaultStyle());
                        }
        
                        CatalogBuilder catalogBuilder = new CatalogBuilder(getCatalog());
                        catalogBuilder.calculateLayerGroupBounds(group);
                        
                        getCatalog().save(group);
                        
                        Map<String,String> parameters = new HashMap<String,String>();
                        parameters.put(LayerGroupEditPage.WORKSPACE, group.getWorkspace().getName());
                        parameters.put(LayerGroupEditPage.GROUP, group.getName());
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
            }
        };
    }    
}