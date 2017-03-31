/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for freemarker templates.
 * 
 * <ul>
 * <li>templates</li>
 * <li>templates/{template}.ftl</li>
 * <li>workspaces/{workspace}/templates</li>
 * <li>workspaces/{workspace}/templates/{template>.ftl</li>
 * <li>workspaces/{workspace}/datastores/{store}/templates</li>
 * <li>workspaces/{workspace}/datastores/{store}/templates/{template}.ftl</li>
 * <li>workspaces/{workspace}/datastores/{store}/featuretypes/{ft}/templates</li>
 * <li>workspaces/{workspace}/datastores/{store}/teaturetypes/{ft}/templates/{template}.ftl</li>
 * <li>workspaces/{workspace}/coveragestores/{store}/coverage/{ft}/templates</li>
 * <li>workspaces/{workspace}/coveragestores/{store}/coverage/{ft}/templates/{template}.ftl</li>
 * </ul>
 * @author Jody Garnett (Boundless)
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class TemplateController extends CatalogController {
    
    private GeoServerResourceLoader resources;
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");
    
    @Autowired
    public TemplateController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
        resources = catalog.getResourceLoader();
    }
    
    /**
     * Template definition.
     * 
     * @return Template definition
     */
    @DeleteMapping (
        value = {
            "/templates/{template}",
            "/workspaces/{workspace}/templates/{template}",
            "/workspaces/{workspace}/datastores/{store}/templates/{template}",
            "/workspaces/{workspace}/datastores/{store}/featuretypes/{type}/templates/{template}",
            "/workspaces/{workspace}/coveragestores/{store}/templates/{template}",
            "/workspaces/{workspace}/coveragestores/{store}/coverages/{type}/templates/{template}",
        }
    )
    public void templateDelete(
            HttpServletResponse response,
            @PathVariable(required = false) String workspace,
            @PathVariable(required = false) String store,
            @PathVariable(required = false) String type,
            @PathVariable String template
            ){
        String filename = template+"."+MEDIATYPE_FTL_EXTENSION;
        String path = Paths.path(path(workspace, store, type ), filename);
        Resource resource = resources.get(path);
        
        if( resource.getType() != Type.RESOURCE ){
            throw new ResourceNotFoundException("Template not found: '"+path+"'");
        }
        boolean removed = resource.delete();
        if (!removed) {
            throw new RestException("Template '" + path + "' not removed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Template definition.
     * 
     * @return Template Definitin
     */
    @GetMapping(
        value = {
            "/templates/{template}",
            "/workspaces/{workspace}/templates/{template}",
            "/workspaces/{workspace}/datastores/{store}/templates/{template}",
            "/workspaces/{workspace}/datastores/{store}/featuretypes/{type}/templates/{template}",
            "/workspaces/{workspace}/coveragestores/{store}/templates/{template}",
            "/workspaces/{workspace}/coveragestores/{store}/coverages/{type}/templates/{template}",
        },
        produces = {
           MEDIATYPE_FTL_VALUE // text/plain
       }
    )
    public void templateGet(
            HttpServletResponse response,
            @PathVariable(required = false) String workspace,
            @PathVariable(required = false) String store,
            @PathVariable(required = false) String type,
            @PathVariable String template
            ){
        String filename = template+"."+MEDIATYPE_FTL_EXTENSION;
        String path = Paths.path(path(workspace, store, type ), filename);
        Resource resource = resources.get(path);
        
        if( resource.getType() != Type.RESOURCE ){
            throw new ResourceNotFoundException("Template not found: '"+path+"'");
        }
        byte[] bytes;
        try {
            bytes = resource.getContents();
            
            response.setContentType(MEDIATYPE_FTL_VALUE);
            response.setContentLength(bytes.length);
            
            try( ServletOutputStream output = response.getOutputStream() ){
                output.write(bytes);
                output.flush();
            }
        } catch (IOException problem) {
            throw new RestException(problem.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,problem);
        }
    }

    /**
     * All templates as JSON, XML or HTML.
     * 
     * @return All templates
     */
    @PutMapping(
        value = {
            "/templates/{template}",
            "/workspaces/{workspace}/templates/{template}",
            "/workspaces/{workspace}/datastores/{store}/templates/{template}",
            "/workspaces/{workspace}/datastores/{store}/featuretypes/{type}/templates/{template}",
            "/workspaces/{workspace}/coveragestores/{store}/templates/{template}",
            "/workspaces/{workspace}/coveragestores/{store}/coverages/{type}/templates/{template}",
        },
        consumes = {MEDIATYPE_FTL_VALUE,MediaType.TEXT_PLAIN_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public void templatePut(
            HttpServletRequest request,
            @PathVariable(required = false) String workspace,
            @PathVariable(required = false) String store,
            @PathVariable(required = false) String type,
            @PathVariable String template
            ){
        String filename = template + "." + MEDIATYPE_FTL_EXTENSION;
        String path = path(workspace, store, type);
        Resource directory = resources.get(path);
    
        Resource resource = fileUpload(directory, filename, request);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT template: " + resource.path());
        }
    }

    //
    // List Templates
    //
    // These endpoints return a list of FreeMarkerTemplateInfo, that is converted to the appropriate output.
    //
    /**
     * All templates as JSON, XML or HTML.
     * 
     * @return All templates
     */
    @GetMapping(
            value = {
                "/templates",
                "/workspaces/{workspace}/templates",
                "/workspaces/{workspace}/datastores/{store}/templates",
                "/workspaces/{workspace}/datastores/{store}/featuretypes/{type}/templates",
                "/workspaces/{workspace}/coveragestores/{store}/templates",
                "/workspaces/{workspace}/coveragestores/{store}/coverages/{type}/templates",
            },
            produces = {
                MediaType.TEXT_HTML_VALUE, // this is the default
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE
            })
    public RestWrapper<TemplateInfo> templatesGet(
            @PathVariable(required = false) String workspace,
            @PathVariable(required = false) String store,
            @PathVariable(required = false) String type){
        String path = path(workspace, store, type );
        Resource directory = resources.get(path);
        switch( directory.getType() ){
        case RESOURCE:
        case UNDEFINED:
            throw new ResourceNotFoundException("Directory not found: '"+path+"'"); 
        default:
            List<Resource> files = Resources.list(directory, new Resources.ExtensionFilter("FTL"), false);
            List<TemplateInfo> list = new ArrayList<TemplateInfo>();
            for (Resource file : files) {
                list.add(new TemplateInfo(file));
            }
            return wrapList(list, TemplateInfo.class);
        }
    }
    /**
     * Verifies mime type and use {@link RESTUtil
     * @param directory
     * @param filename
     * @param request
     * @return
     */
    private Resource fileUpload(Resource directory, String filename, HttpServletRequest request) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT file: mimetype=" + request.getContentType() + ", path="
                    + directory.path());
        }
        try {
            Resource upload = RESTUtils.handleBinUpload(filename, directory, false, request);
            return upload;
        } catch (IOException problem) {
            throw new RestException(problem.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
                    problem);
        }
    }

    /**
     * Construct "get directory path"
     * @param workspace Workspace, optional
     * @param store DataStore or Coverage store, requires workspace
     * @param type FeatureType or Coverage, requires store
     * @return template path
     */
    public static String path(String workspace, String store, String type) {
        List<String> path = new ArrayList<String>();
        path.add("workspaces");
        
        if (workspace != null) {
            path.add(workspace);
            if (store != null) {
                path.add(store);
                if (type != null) {
                    path.add(type);
                }
            }
        }
        return Paths.path( path.toArray(new String[] {}) );
    }
    
}
