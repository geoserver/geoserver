/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.Map;

import org.geoserver.catalog.ResourceInfo;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

public class ResourceHTMLFormat extends CatalogFreemarkerHTMLFormat {

    public ResourceHTMLFormat(Class clazz, Request request, Response response, Resource resource) {
        super(clazz, request, response, resource);
    }

    @Override
    protected Configuration createConfiguration(Object data, Class clazz) {
        Configuration cfg = super.createConfiguration(data, clazz);
        cfg.setObjectWrapper( 
            new ObjectToMapWrapper<ResourceInfo>(ResourceInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, ResourceInfo object) {
                    try {
                        properties.put( "boundingBox", object.boundingBox() );
                    } 
                    catch (Exception e) {
                        throw new RuntimeException( e );
                    }
                }
            }      
        );
        return cfg;
        
    }
}
