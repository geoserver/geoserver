/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.URLs;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * Controller which publishes files through a web interface.
 *
 * <p>To use this controller, it should be mapped to a particular url in the url mapping of the
 * spring dispatcher servlet. Example:
 *
 * <pre>
 * <code>
 *   &lt;bean id="filePublisher" class="org.geoserver.ows.FilePublisher"/&gt;
 *   &lt;bean id="dispatcherMappings"
 *      &lt;property name="alwaysUseFullPath" value="true"/&gt;
 *      class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping"&gt;
 *      &lt;property name="mappings"&gt;
 *        &lt;prop key="/schemas/** /*.xsd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/schemas/** /*.dtd"&gt;filePublisher&lt;/prop&gt;
 *        &lt;prop key="/styles/*"&gt;filePublisher&lt;/prop&gt;
 *      &lt;/property&gt;
 *   &lt;/bean&gt;
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class FilePublisher extends AbstractURLPublisher {
    /** Resource loader */
    protected GeoServerResourceLoader loader;

    /** Servlet resource loader */
    protected ServletContextResourceLoader scloader;

    /**
     * Creates the new file publisher.
     *
     * @param loader The loader used to locate files.
     */
    public FilePublisher(GeoServerResourceLoader loader) {
        this.loader = loader;
    }

    @Override
    protected void initServletContext(ServletContext servletContext) {
        this.scloader = new ServletContextResourceLoader(servletContext);
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

        // sigh, in order to serve the file we have to open it 2 times
        // 1) to determine its mime type
        // 2) to determine its encoding and really serve it
        // we can't coalish 1) because we don't have a way to give jmimemagic the bytes at the
        // beginning of the file without disabling extension quick matching

        // load the file
        File file = loader.find(reqPath);

        if (file == null && scloader != null) {
            // try loading as a servlet resource
            ServletContextResource resource =
                    (ServletContextResource) scloader.getResource(reqPath);
            if (resource != null && resource.exists()) {
                file = resource.getFile();
            }
        }

        if (file != null) {
            return URLs.fileToUrl(file);
        } else {
            return null;
        }
    }
}
