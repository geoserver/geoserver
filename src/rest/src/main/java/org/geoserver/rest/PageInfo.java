/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;

/**
 * An object which contains information about the "page" or "resource" being accessed
 * in a restlet request.
 * <p>
 * An instance of this class can be referenced by any restlet via:
 * <pre>
 * (PageDetails) request.getAttributes().get( PageDetails.KEY );
 * </pre>
 * </p>
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class PageInfo {

    /**
     * key to reference this object by
     */
    public static final String KEY = "org.geoserver.pageDetails";

    String baseURL;
    
    String rootPath;
    
    String basePath;
    
    String pagePath;
    
    /**
     * The extension of the page. 
     */
    String extension;
    
    public PageInfo() {
    }


    
    public String getBaseURL() {
        return baseURL;
    }



    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }



    public String getRootPath() {
        return rootPath;
    }



    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }



    public String getBasePath() {
        return basePath;
    }



    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }



    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getExtension() {
        return extension;
    }
    
    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    public String pageURI(String path) {
        return buildURI(pagePath, path);
    }
    
    public String rootURI(String path) {
        return buildURI(rootPath, path);
    }
    
    public String baseURI(String path) {
        return buildURI(basePath, path);
    }
    
    String buildURI(String base, String path) {
        if(path != null) {
            if(path.startsWith(".")) {
                if(base.endsWith("/"))
                    base = base.substring(1);
                path = base + path;
            } else {
                path = ResponseUtils.appendPath(base, path);
            }
        }
        
        return ResponseUtils.buildURL(baseURL, path, null, URLType.SERVICE);
    }
    
}
