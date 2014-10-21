/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;


public class FreemarkerTemplateResource extends StoreFileResource {

    public static final String MEDIATYPE_FTL_EXTENSION = "ftl";
    public static final MediaType MEDIATYPE_FTL = new MediaType("text/plain");
    static {
        MediaTypes.registerExtension(MEDIATYPE_FTL_EXTENSION, MEDIATYPE_FTL);
    }

    public FreemarkerTemplateResource(Request request, Response response, Catalog catalog) {
        super(request, response, catalog);
    }

    @Override
    public boolean allowGet() {
        return true;
    }
    
    @Override
    public boolean allowPut() {
        return true;
    }
    
    @Override
    public boolean allowDelete() {
        return true;
    }
    
    @Override
    public boolean allowPost() {
        return false;
    }

    @Override    
    public void handleGet() {   
        getResponse().setEntity(new FileRepresentation(getTemplateFile(), MEDIATYPE_FTL, 0));
    }
    
    @Override    
    public void handlePut() {
        doFileUpload();
        getResponse().setStatus(Status.SUCCESS_CREATED);
    }
    
    @Override    
    public void handleDelete() {
        if (getTemplateFile().delete()) {
            getResponse().setStatus(Status.SUCCESS_OK);
        } else {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
        }
    }    
        
    private File doFileUpload() {
        try {
            getResponse().setStatus(Status.SUCCESS_ACCEPTED);
            File directory = catalog.getResourceLoader().findOrCreateDirectory(getDirectoryPath(getRequest()));

            if (LOGGER.isLoggable(Level.INFO)) {
                MediaType mediaType = getRequest().getEntity().getMediaType();
                LOGGER.info("PUT file: mimetype=" + mediaType + ", path=" + directory.getAbsolutePath());
            }
            
            return RESTUtils.handleBinUpload(getAttribute("template") + "." + MEDIATYPE_FTL_EXTENSION, directory, false, getRequest());
        } catch (IOException e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        }
    }
    
    private File getTemplateFile() {
        try {
            File directory = catalog.getResourceLoader().find(getDirectoryPath(getRequest()));
            File templateFile = catalog.getResourceLoader().find(directory, 
                    getAttribute("template") + "." + MEDIATYPE_FTL_EXTENSION);        
            if (templateFile == null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info("File not found: " + getDirectoryPathAsString(getRequest()) + "/" + 
                            getAttribute("template") + "." + MEDIATYPE_FTL_EXTENSION);
                }
                
                throw new RestletException("File Not Found", Status.CLIENT_ERROR_NOT_FOUND);            
            } else {
                return templateFile;
            }
        } catch (IOException e) {
            throw new RestletException(e.getMessage(), Status.CLIENT_ERROR_NOT_FOUND, e);            
        }        
    }

    public static String getDirectoryPathAsString(Request request) {
        StringBuilder buff = new StringBuilder();
        for (String path : getDirectoryPath(request)) {
            buff.append("/").append(path);
        }
        return buff.toString();
    }
    
    /*
     * templates
     * templates/<template>.ftl
     * workspaces/<workspace>/templates
     * workspaces/<workspace>/templates/<template>.ftl
     * workspaces/<workspace>/datastores/<store>/templates
     * workspaces/<workspace>/datastores/<store>/templates/<template>.ftl
     * workspaces/<workspace>/datastores/<store>/featuretypes/<ft>/templates
     * workspaces/<workspace>/datastores/<store>/teaturetypes/<ft>/templates/<template>.ftl
     */
    public static String[] getDirectoryPath(Request request) {
        String workspace = RESTUtils.getAttribute(request, "workspace");
        String datastore = RESTUtils.getAttribute(request, "datastore");
        String featureType = RESTUtils.getAttribute(request, "featuretype");

        String coveragestore = RESTUtils.getAttribute(request, "coveragestore");
        String coverage = RESTUtils.getAttribute(request, "coverage");

        List<String> path = new ArrayList<String>();
        path.add("workspaces");
        
        if (workspace != null) {
            path.add(workspace);
            if (datastore != null) {
                path.add(datastore);
                if (featureType != null) {
                    path.add(featureType);                    
                }
            } else if (coveragestore != null) {
                path.add(coveragestore);   
                if (coverage != null) {
                    path.add(coverage);
                }
            }
        }
        
        return path.toArray(new String[] {});
    }
}
