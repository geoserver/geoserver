/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.istyle;

import java.io.StringWriter;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerApplication;
import org.geotools.geometry.jts.ReferencedEnvelope;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

public class OpenLayersMapPanel extends Panel implements IHeaderContributor {

    /**
     * freemarker template configuration
     */
    final static Configuration config;
    static {
        config = new Configuration();
        config.setClassForTemplateLoading(OpenLayersMapPanel.class, "");
    }
    
    final Random rand = new Random();
    
    LayerInfo layer;
    StyleInfo style;
    
    public OpenLayersMapPanel(String id, LayerInfo layer) {
        super(id);
        setOutputMarkupId(true);
        this.layer = layer;
        this.style = layer.getDefaultStyle();
    }
    
    public LayerInfo getCurrentLayer() {
        return layer;
    }
    
    public StyleInfo getCurrentStyle() {
        return style;
    }
    
    @Override
    public void renderHead(IHeaderResponse response) {
        try {
            //render css
            SimpleHash model = new SimpleHash();
            model.put("markupId", getMarkupId());
            response.render(StringHeaderItem.forString(renderTemplate("OL-css.ftl", model)));
            
            //TODO: point back to GeoServer
            response.render(JavaScriptReferenceHeaderItem.forUrl("http://openlayers.org/api/OpenLayers.js"));  
            
            model.put("layers", layer.getName());
            model.put("styles", style.getName());
            
            HttpServletRequest req = GeoServerApplication.get().servletRequest();
            model.put("geoserver", ResponseUtils.baseURL(req));
            
            bbox(layer, model);
            
            //render
            model.put("ran", rand.nextInt());
            response.render(OnLoadHeaderItem.forScript(renderTemplate("OL-onload.ftl", model)));
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }
    
    
    public void update(LayerInfo layer, StyleInfo style, AjaxRequestTarget target) {
        layer = layer != null ? layer : this.layer;
        
        if (style == null) {
            if (!layer.equals(this.layer))  {
                //no style specified, and the layer was changed
                style = layer.getDefaultStyle();
            }
            else {
                //no style specified and layer did not change, do not change style
            }
        }
        
        try {
            SimpleHash model = new SimpleHash();
            model.put("markupId", getMarkupId());
            model.put("layers", layer.getName());
            model.put("styles", style.getName());
            bbox(layer, model);
            model.put("ran", rand.nextInt());
            model.put("layerChanged", !layer.equals(this.layer));
            
            HttpServletRequest req = GeoServerApplication.get().servletRequest();
            model.put("geoserver", ResponseUtils.baseURL(req));
            
            target.appendJavaScript(renderTemplate("OL-update.ftl", model));
            
            this.layer = layer;
            this.style = style;
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }
    
    void bbox(LayerInfo layer, SimpleHash model) throws Exception {
        ReferencedEnvelope bbox = layer.getResource().boundingBox();
        String srs = layer.getResource().getSRS();
        if ( bbox == null ) {
            bbox = layer.getResource().getLatLonBoundingBox();
            srs = "EPSG:4326";
        }
        if ( bbox == null ) {
            bbox = new ReferencedEnvelope(-180,-90,180,90, null);
            srs = "EPSG:4326";
        }
        
        model.put("minX", bbox.getMinX());
        model.put("minY", bbox.getMinY());
        model.put("maxX", bbox.getMaxX());
        model.put("maxY", bbox.getMaxY());
        model.put("srs", srs);
        model.put("res", Math.max(bbox.getHeight(),bbox.getWidth())/256d);        
    }
    
    String renderTemplate(String t, Object model) throws Exception {
        Template template = config.getTemplate(t);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }
    
}
