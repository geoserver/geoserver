/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
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
        Resource directory = catalog.getResourceLoader().get(
                Paths.path(FreemarkerTemplateResource.getDirectoryPath(getRequest())));        
        List<Resource> files = Resources.list(directory, new Resources.ExtensionFilter("FTL"), false);
        
        List<FreemarkerTemplateInfo> list = new ArrayList<FreemarkerTemplateInfo>();
        for (Resource file : files) {
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
