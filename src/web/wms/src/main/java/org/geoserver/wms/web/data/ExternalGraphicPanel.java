/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.NumberValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.h2.util.Resources;

/**
 * Allows setting the data for using an ExternalImage
 * 
 * 
 */
@SuppressWarnings("serial")
public class ExternalGraphicPanel extends Panel {

    private TextField<String> onlineResource;
    private TextField<String> format;
    private TextField<String> width;
    private TextField<String> height;
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
        
        IModel<String> bind = styleModel.bind("legend.onlineResource");
        onlineResource = new TextField<String>("onlineResource", bind );
        onlineResource.add(new StringValidator(){
            final List<String> EXTENSIONS = Arrays.asList(new String[]{"png","gif","jpeg","jpg"});
            
            protected void onValidate(IValidatable<String> input) {
                String value = input.getValue();
                int last = value == null ? -1 : value.lastIndexOf('.');
                if (last == -1 || !EXTENSIONS.contains( value.substring(last + 1).toLowerCase() ) ){
                    ValidationError error = new ValidationError();
                    error.setMessage( "Not an image" );
                    error.addMessageKey("nonImage");
                    input.error(error);
                    return;
                }
                URI uri = null;
                try {
                    uri = new URI(value);
                } catch (URISyntaxException e1) {
                    // Unable to check if absolute
                } 
                if( uri != null && uri.isAbsolute()){
                    try {
                        String baseUrl = baseURL(onlineResource.getForm());
                        if( !value.startsWith(baseUrl)){
                            onlineResource.warn("Recommend use of styles directory at "+baseUrl);
                        }
                        URL url = uri.toURL();
                        URLConnection conn = url.openConnection();                        
                        if("text/html".equals(conn.getContentType())){
                            ValidationError error = new ValidationError();
                            error.setMessage("Unable to access image");
                            error.addMessageKey("imageUnavailable");
                            input.error(error);
                            return; // error message back!
                        }
                    } catch (MalformedURLException e) {
                        ValidationError error = new ValidationError();
                        error.setMessage("Unable to access image");
                        error.addMessageKey("imageUnavailable");
                        input.error(error);
                    } catch (IOException e) {
                        ValidationError error = new ValidationError();
                        error.setMessage("Unable to access image");
                        error.addMessageKey("imageUnavailable");
                        input.error(error);
                    }
                    return; // no further checks possible
                }
                else {
                    GeoServerResourceLoader resources = GeoServerApplication.get().getResourceLoader();
                    try {
                        File styles = resources.find("styles");
                        String[] path = value.split(File.separator);
                        File test = resources.find(styles, path);
                        if (test == null) {
                            ValidationError error = new ValidationError();
                            error.setMessage("File not found in styles directory");
                            error.addMessageKey("imageNotFound");
                            input.error(error);
                        }
                    } catch (IOException e) {
                        ValidationError error = new ValidationError();
                        error.setMessage("File not found in styles directory");
                        error.addMessageKey("imageNotFound");
                        input.error(error);
                    }
                }
            }
        });
        onlineResource.setOutputMarkupId(true);
        table.add(onlineResource);
        
        // add the autofill button
        autoFill = new GeoServerAjaxFormLink("autoFill", styleForm) {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                onlineResource.processInput();
                if (onlineResource.getModelObject() != null) {
                    URL url = null;
                    try {
                        String baseUrl = baseURL(form);                       
                        String external = onlineResource.getModelObject().toString();
                        
                        URI uri = new URI( external );
                        if( uri.isAbsolute() ){
                            url = uri.toURL();
                            if( !external.startsWith(baseUrl)){
                                form.warn( "Recommend use of styles directory at "+baseUrl);
                            }
                        }
                        else {
                            url = new URL( baseUrl + "styles/"+external );
                        }
                        
                        URLConnection conn = url.openConnection();                        
                        if("text/html".equals(conn.getContentType())){
                            form.error("Unable to access url");
                            return; // error message back!
                        }
                        
                        format.setModelValue(conn.getContentType());
                        BufferedImage image = ImageIO.read(conn.getInputStream());
                        width.setModelValue("" + image.getWidth());
                        height.setModelValue("" + image.getHeight());
                    } catch (FileNotFoundException notFound ){
                        form.error( "Unable to access "+url);
                    } catch (Exception e) {
                        e.printStackTrace();
                        form.error( "Recommend use of styles directory at "+e);
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
    
    /**
     * Lookup base URL using provied form
     * @param form
     * @see ResponseUtils
     * @return baseUrl
     */
    protected String baseURL(Form form) {
        WebRequest request = (WebRequest) form.getRequest();
        HttpServletRequest httpServletRequest;
        httpServletRequest = ((WebRequest) request).getHttpServletRequest();
        String baseUrl = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (StringUtils.isEmpty(baseUrl)) {
            GeoServer gs = GeoServerApplication.get().getGeoServer();
            baseUrl = gs.getGlobal().getSettings().getProxyBaseUrl();
            if (StringUtils.isEmpty(baseUrl)) {
                return ResponseUtils.baseURL(httpServletRequest);
            }
        }
        return baseUrl;
    }

    private void updateVisibility(boolean b) {        
        table.setVisible(b); 
        autoFill.setVisible(b);
        hide.setVisible(b);
        show.setVisible(!b);
    }
}
