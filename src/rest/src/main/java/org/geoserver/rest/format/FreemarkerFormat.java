/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.resource.Representation;

import freemarker.template.Configuration;

/**
 * A read-only format which uses a Freemarker template for output.
 * <p>
 * This class is a thin wrapper around {@link TemplateRepresentation}.
 * </p>
 * 
 * @author David Winslow <dwinslow@openplans.org>
 */
public class FreemarkerFormat extends DataFormat {
    /**
     * the freemarker configuration.
     */
    private Configuration myConfig;
    /**
     * the name of the template to execute.
     */
    private String myTemplateFileName;

    /**
     * Set up a new FreemarkerFormat
     *
     * @param templateName the filename of the template to use
     * @param c a Class object to use for retrieving the template resource
     * @param type the MediaType of the result
     */
    public FreemarkerFormat(String templateName, Class c, MediaType type){
        super(type);
        myTemplateFileName = templateName;
        myConfig = new Configuration();
        myConfig.setClassForTemplateLoading(c, "");
    }

    @Override
    public Object toObject(Representation representation) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Representation toRepresentation(Object object) {
        return new TemplateRepresentation(myTemplateFileName, myConfig, object, mediaType);
    }

}
