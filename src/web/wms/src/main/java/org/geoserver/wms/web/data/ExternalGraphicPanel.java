/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;

/**
 * Allows setting the data for using an ExternalImage
 * 
 * 
 */
@SuppressWarnings("serial")
public class ExternalGraphicPanel extends Panel {

    private TextField onlineResource;
    private TextField format;
    private TextField width;
    private TextField height;
    private Label noLegend;
    private WebMarkupContainer table;
    private GeoServerAjaxFormLink autoFill;
    private GeoServerAjaxFormLink show;
    private GeoServerAjaxFormLink hide;
   

    /**
     * @param id
     * @param model Must return a {@link ResourceInfo}
     */
    public ExternalGraphicPanel(String id,  final CompoundPropertyModel<StyleInfo> styleModel, Form styleForm) {
        super(id, styleModel);
        
        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);
        
        table = new WebMarkupContainer("list");
        table.setOutputMarkupId(true);
        
        onlineResource = new TextField("onlineResource", styleModel.bind("legend.onlineResource"));
        onlineResource.add(new UrlValidator());
        onlineResource.setOutputMarkupId(true);
        table.add(onlineResource);
        
        // add the autofill button
        autoFill = new GeoServerAjaxFormLink("autoFill", styleForm) {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                onlineResource.processInput();
                if (onlineResource.getModelObject() != null) {
                    try {
                        URL url = new URL(onlineResource.getModelObject().toString());
                        URLConnection conn = url.openConnection();
                        format.setModelValue(conn.getContentType());
                        BufferedImage image = ImageIO.read(conn.getInputStream());
                        width.setModelValue("" + image.getWidth());
                        height.setModelValue("" + image.getHeight());
                    } catch (Exception e) {
                    }
                }

                target.addComponent(format);
                target.addComponent(width);
                target.addComponent(height);
            }
        };
        
        table.add(autoFill);

        format = new TextField("format", styleModel.bind("legend.format"));
        format.setOutputMarkupId(true);
        table.add(format);

        width = new TextField("width", styleModel.bind("legend.width"), Integer.class);
        width.add(NumberValidator.minimum(0));
        width.setOutputMarkupId(true);
        table.add(width);

        height = new TextField("height", styleModel.bind("legend.height"), Integer.class);
        height.add(NumberValidator.minimum(0));
        height.setOutputMarkupId(true);
        table.add(height);
        
        container.add(table);
        
        show = new GeoServerAjaxFormLink("show", styleForm) {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                updateVisibility(true);    
                target.addComponent(container);                
            }
        };
        container.add(show);            
        
        hide = new GeoServerAjaxFormLink("hide", styleForm) {
            @SuppressWarnings("deprecation")
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                onlineResource.setModelObject("");
                onlineResource.processInput();
                format.setModelObject("");                
                width.setModelObject("0");
                height.setModelObject("0");
                target.addComponent(onlineResource);
                target.addComponent(format);
                target.addComponent(width);
                target.addComponent(height);                  
                target.addComponent(container);
                updateVisibility(false);  
            }
        };
        container.add(hide);
                
        String url = styleModel.getObject().getLegend().getOnlineResource();
        boolean visible = url != null && !url.isEmpty();           
        updateVisibility(visible);
        
    }

    private void updateVisibility(boolean b) {        
        table.setVisible(b); 
        autoFill.setVisible(b);
        hide.setVisible(b);
        show.setVisible(!b);
    }
}
