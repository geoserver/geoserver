/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.data.DataUtilities;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * Controller which publishes styles through a web interface.
 * <p>
 * To use this controller, it should be mapped to a particular url in the url mapping of the spring dispatcher servlet.
 * 
 * @author Alex Goudine, Boundless
 */
public class StylePublisher extends AbstractURLPublisher {
    /**
     * Resource loader
     */
    protected Catalog catalog;

    /**
     * Servlet resource loader
     */
    protected ServletContextResourceLoader scloader;

    /**
     * Creates the new file publisher.
     * 
     * @param loader The loader used to locate files.
     */
    public StylePublisher(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    protected URL getUrl(HttpServletRequest request) throws IOException {
        String ctxPath = request.getContextPath();
        String reqPath = request.getRequestURI();
        reqPath = URLDecoder.decode(reqPath, "UTF-8");
        reqPath = reqPath.substring(ctxPath.length());

        if ((reqPath.length() > 1) && reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        Resource resource;

        if (reqPath.startsWith("styles/")) {
           
            String[] tokens = reqPath.split("/");
            int count = tokens.length;
            GeoServerResourceLoader resources = catalog.getResourceLoader();
            GeoServerDataDirectory data = new GeoServerDataDirectory(resources);
            if (count == 2) {
                // global style
                String stylePath = tokens[1];
                resource = data.getStyles(stylePath);
            } else if (count == 3) {
                // workspaced style
                String temp = "workspaces/" + tokens[1] + "/styles/" + tokens[2];
                String wsName = tokens[1];
                String stylePath = tokens[2];

                WorkspaceInfo workspace = catalog.getWorkspaceByName(wsName);
                if (workspace == null) {
                    throw new IllegalArgumentException("Workspace not found");
                }
                resource = data.getStyles(workspace, stylePath);
                if (resource.getType() == Type.UNDEFINED) {
                    // workspace style not found, try the global styles directory as a fallback
                    resource = data.getStyles(stylePath);
                }
            } else {
                throw new IllegalStateException("Unexpected number of tokens in reqest path.");
            }
            
            switch (resource.getType()) {
            case RESOURCE:
                return DataUtilities.fileToURL(resource.file());
            case DIRECTORY:
            case UNDEFINED:
            default:
                return null;
            }
        }
        return null;
    }
}
