/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * Collection of utilities for GeoServer web application components.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class WebUtils {

    static final Logger LOGGER = Logging.getLogger(WebUtils.class);
    
    /**
     * Utility method for localizing strings using Wicket i18n subsystem. Useful if your model
     * needs to be localized and you don't have access to a Component instance.
     * Use with care, in most cases you should be able to localize your messages directly in 
     * pages or components.
     * @param key
     * @param model
     * @param params
     * @return
     */
    public static String localize(String key, IModel model, Object... params) {
        StringResourceModel rm = new StringResourceModel(key, null, model, params);
        rm.setLocalizer(GeoServerApplication.get().getResourceSettings().getLocalizer());
        return rm.getString();
    }

    /**
     * Returns a resource stream based on a freemarker template.
     * <p>
     * 
     * </p>
     * 
     * @param c
     *                The component being marked up.
     * @param model
     *                The template model to pass to the freemarker template.
     * 
     * @return The resource stream.
     */
    public static IResourceStream getFreemakerMarkupStream(Component c,
            TemplateModel model) {
        return new FreemarkerResourceStream(c.getClass(), model);
    }

    static class FreemarkerResourceStream implements IResourceStream {

        Class clazz;

        TemplateModel model;

        String templateName;

        Configuration cfg;

        FreemarkerResourceStream(Class clazz, TemplateModel model) {
            this.clazz = clazz;
            this.model = model;

            templateName = clazz.getSimpleName() + ".ftl";

            cfg = new Configuration();
            cfg.setClassForTemplateLoading(clazz, "");
        }

        public String getContentType() {
            return "text/html";
        }

        public InputStream getInputStream()
                throws ResourceStreamNotFoundException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                Template t = cfg.getTemplate(templateName);
                t.process(model, new OutputStreamWriter(output));

                return new ByteArrayInputStream(output.toByteArray());
            } catch (IOException e) {
                throw (ResourceStreamNotFoundException) new ResourceStreamNotFoundException(
                        "Could not find template for: " + clazz).initCause(e);
            } catch (TemplateException e) {
                throw (ResourceStreamNotFoundException) new ResourceStreamNotFoundException(
                        "Error in tempalte for: " + clazz).initCause(e);
            }
        }

        public Locale getLocale() {
            return cfg.getLocale();
        }

        public void setLocale(Locale locale) {
            cfg.setLocale(locale);
        }

        public long length() {
            return -1;
        }

        public Time lastModifiedTime() {
            Object source;
            try {
                source = cfg.getTemplateLoader().findTemplateSource(
                        templateName);
            } catch (IOException e) {
                // TODO: log this
                return null;
            }

            if (source != null) {
                long modified = cfg.getTemplateLoader().getLastModified(source);
                return Time.valueOf(modified);
            }

            return null;
        }

        public void close() throws IOException {
        }
    }

}
