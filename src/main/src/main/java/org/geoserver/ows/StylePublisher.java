/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
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
import org.geotools.util.URLs;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * Controller which publishes styles through a web interface.
 *
 * <p>To use this controller, it should be mapped to a particular url in the url mapping of the
 * spring dispatcher servlet.
 *
 * @author Alex Goudine, Boundless
 */
public class StylePublisher extends AbstractURLPublisher {
    /** Resource loader */
    protected Catalog catalog;

    /** Servlet resource loader */
    protected ServletContextResourceLoader scloader;

    /** Creates the new file publisher. */
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

        if (reqPath.startsWith("styles/") && (reqPath.length() > 7)) {
            Resource resource = null;
            GeoServerResourceLoader resources = catalog.getResourceLoader();
            GeoServerDataDirectory data = new GeoServerDataDirectory(resources);
            String stylePath = reqPath.substring(7);
            int slash = stylePath.indexOf('/');
            if ((slash > -1) && (stylePath.length() > (slash + 1))) {
                // workspaced style
                String wsName = stylePath.substring(0, slash);
                WorkspaceInfo workspace = catalog.getWorkspaceByName(wsName);
                if (workspace != null) {
                    String wsStylePath = stylePath.substring(slash + 1);
                    resource = data.getStyles(workspace, wsStylePath);
                    if (resource.getType() == Type.UNDEFINED) {
                        // workspace style not found, try the global styles directory as a fallback
                        resource = data.getStyles(wsStylePath);
                    }
                }
            }
            if ((resource == null) || (resource.getType() == Type.UNDEFINED)) {
                // global style
                resource = data.getStyles(stylePath);
            }

            switch (resource.getType()) {
                case RESOURCE:
                    return URLs.fileToUrl(resource.file());
                case DIRECTORY:
                case UNDEFINED:
                default:
                    return null;
            }
        }
        return null;
    }
}
