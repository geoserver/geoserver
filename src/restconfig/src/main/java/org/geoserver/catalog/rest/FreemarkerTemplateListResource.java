/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;


public class FreemarkerTemplateListResource extends AbstractCatalogListResource {

    private Catalog catalog;

    
    protected FreemarkerTemplateListResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, FreemarkerTemplateInfo.class, catalog);
        this.catalog = catalog;
    }
    
    
    @Override
    public boolean allowGet() {
        return true;
    }
    
    @Override
    public boolean allowPut() {
        return false;
    }
    
    @Override
    public boolean allowDelete() {
        return false;
    }
    
    @Override
    public boolean allowPost() {
        return false;
    }
    
    @Override    
    protected String getItemName(XStreamPersister xp) {
        return "template";
    }
        
    @Override
    protected Collection<FreemarkerTemplateInfo> handleListGet() throws Exception {
        File directory = catalog.getResourceLoader().find(FreemarkerTemplateResource.getDirectoryPath(getRequest()));        
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".ftl");
            }            
        });
        
        List<FreemarkerTemplateInfo> list = new ArrayList<FreemarkerTemplateInfo>();
        for (File file : files) {
            list.add(new FreemarkerTemplateInfo(file));
        }
        return list;
    }
    
    @Override
    String href(String link, DataFormat format) {
        return getPageInfo().getBaseURL() + getPageInfo().getRootPath() +
                FreemarkerTemplateResource.getDirectoryPathAsString(getRequest()) + "/templates/" + link;
    }
}
