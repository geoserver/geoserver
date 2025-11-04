/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.URLs;
import org.springframework.http.MediaType;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;

/**
 * Controller which publishes files through a web interface.
 *
 * <p>To use this controller, it should be mapped to a particular url in the url mapping of the spring dispatcher
 * servlet. Example:
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

    /**
     * System property to control whether or not to disable server static files with the text/html or
     * application/javascript mime types. When set to true, these mime types will be converted to text/plain. Default is
     * false.
     */
    public static final String DISABLE_STATIC_WEB_FILES = "GEOSERVER_DISABLE_STATIC_WEB_FILES";

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
    protected String getMimeType(String reqPath, String filename) {
        String mimeType = super.getMimeType(reqPath, filename);
        if (!("/index.html".equals(reqPath) || "/accessDenied.html".equals(reqPath))) {
            String lowerCaseMime = mimeType.toLowerCase();
            // force using the static web files directory for html/javascript files and only allow
            // the html/javascript mime types when the system property is set to true to mitigate
            // stored XSS vulnerabilities
            if (lowerCaseMime.contains("html") || lowerCaseMime.contains("javascript")) {
                String prop = GeoServerExtensions.getProperty(DISABLE_STATIC_WEB_FILES);
                if (Boolean.parseBoolean(prop) || !reqPath.startsWith("/www/")) {
                    return MediaType.TEXT_PLAIN_VALUE;
                }
            }
        }
        return mimeType;
    }

    @Override
    protected boolean isAttachment(String reqPath, String filename, String mime) {
        // prevent stored XSS using malicious resources with the
        // text/xml, application/xml or image/svg+xml mime types
        return mime.toLowerCase().contains("xml") || super.isAttachment(reqPath, filename, mime);
    }

    @Override
    protected URL getUrl(HttpServletRequest request, String reqPath) throws IOException {
        if ((reqPath.length() > 1) && reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1);
        }

        // load the file (do not load index.html from the data directory)
        File file = "index.html".equals(reqPath) ? null : loader.find(reqPath);

        if (file == null && scloader != null) {
            // try loading as a servlet resource
            ServletContextResource resource = (ServletContextResource) scloader.getResource(reqPath);
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
