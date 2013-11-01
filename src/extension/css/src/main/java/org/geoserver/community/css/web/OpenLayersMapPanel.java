/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.web.GeoServerApplication;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

class OpenLayersMapPanel extends Panel implements IHeaderContributor {
    static final Logger LOGGER  = Logging.getLogger(OpenLayersMapPanel.class);
    final static Configuration templates;
    
    static {
        templates = new Configuration();
        templates.setClassForTemplateLoading(OpenLayersMapPanel.class, "");
        templates.setObjectWrapper(new DefaultObjectWrapper());
    }

    final Random rand = new Random();
    final ReferencedEnvelope bbox;
    final ResourceInfo resource;
    final StyleInfo style;

    public OpenLayersMapPanel(String id, LayerInfo layer, StyleInfo style) {
        super(id);
        this.resource = layer.getResource();
        this.bbox = resource.getLatLonBoundingBox();
        this.style = style;
        setOutputMarkupId(true);
        
        try {
            ensureLegendDecoration();
        } catch(IOException e) {
            LOGGER.log(Level.WARNING, "Failed to put legend layout file in the data directory, the legend decoration will not appear", e);
        }
    } 

    private void ensureLegendDecoration() throws IOException {
        GeoServerDataDirectory dd = GeoServerApplication.get().getBeanOfType(GeoServerDataDirectory.class);
        File layouts = dd.findOrCreateDir("layouts");
        File legend = new File(layouts, "css-legend.xml");
        if(!legend.exists()) {
            String legendLayout = IOUtils.toString(OpenLayersMapPanel.class.getResourceAsStream("css-legend.xml"));
            FileUtils.writeStringToFile(legend, legendLayout);
        }
        
    }

    public void renderHead(IHeaderResponse header) {
        try {
            renderHeaderCss(header);
            renderHeaderScript(header);
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        } catch (TemplateException e) {
            throw new WicketRuntimeException(e);
        }
    }

    private void renderHeaderCss(IHeaderResponse header) 
        throws IOException, TemplateException 
    {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("id", getMarkupId());
        Template template = templates.getTemplate("ol-style.ftl");
        StringWriter css = new java.io.StringWriter();
        template.process(context, css);
        header.renderString(css.toString());
    }

    private void renderHeaderScript(IHeaderResponse header) 
        throws IOException, TemplateException 
    {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("minx", bbox.getMinX());
        context.put("miny", bbox.getMinY());
        context.put("maxx", bbox.getMaxX());
        context.put("maxy", bbox.getMaxY());
        context.put("id", getMarkupId());
        context.put("layer", resource.getPrefixedName());
        context.put("style", style.getName());
        if (style.getWorkspace() != null) {
          context.put("styleWorkspace", style.getWorkspace().getName());
        }
        context.put("cachebuster", rand.nextInt());
        context.put("resolution", Math.max(bbox.getSpan(0), bbox.getSpan(1)) / 256.0);
        Template template = templates.getTemplate("ol-load.ftl");
        StringWriter script = new java.io.StringWriter();
        template.process(context, script);
        header.renderJavascriptReference("../openlayers/OpenLayers.js");
        header.renderOnLoadJavascript(script.toString());
    }

    public String getUpdateCommand() throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("id", getMarkupId());
        context.put("cachebuster", rand.nextInt());

        Template template = templates.getTemplate("ol-update.ftl");
        StringWriter script = new java.io.StringWriter();
        template.process(context, script);
        return script.toString();
    }
}
