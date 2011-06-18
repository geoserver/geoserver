/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.ows.util.EncodingInfo;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;


/**
 * Controller which publishes files through a web interface.
 * <p>
 * To use this controller, it should be mapped to a particular url in the url
 * mapping of the spring dispatcher servlet. Example:
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
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class FilePublisher extends AbstractController {
    /**
     * Resource loader
     */
    protected GeoServerResourceLoader loader;
    
    /**
     * Servlet resource loader
     */
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
    
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        String ctxPath = request.getContextPath();
        String reqPath = request.getRequestURI();
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
            //try loading as a servlet resource
            ServletContextResource resource = (ServletContextResource) scloader.getResource(reqPath);
            if (resource != null && resource.exists()) {
                file = resource.getFile();
            }
        }
        
        if (file == null) {
            //return a 404
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
    
            return null;
        }

        if (file.isDirectory()) {
            String uri = request.getRequestURI().toString();
            uri += uri.endsWith("/") ? "index.html" : "/index.html";
            
            response.addHeader("Location", uri);
            response.sendError(HttpServletResponse.SC_MOVED_TEMPORARILY);
            
            return null;
        }

        // set the mime if known by the servlet container, set nothing otherwise
        // (Tomcat behaves like this when it does not recognize the file format)
        String mime = getServletContext().getMimeType(file.getName());
        if (mime != null)
            response.setContentType(mime);
        
        // set the content length
        long length = file.length();
        if(length > 0 && length <= Integer.MAX_VALUE)
            response.setContentLength((int) length);
        
        // set the last modified header
        long lastModified = file.lastModified();
        if(lastModified > 0) {
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.ENGLISH);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String formatted = format.format(new Date(lastModified)) + " GMT";
            response.setHeader("Last-Modified", formatted);
        }

        // Guessing the charset (and closing the stream)
        EncodingInfo encInfo = null;
        FileInputStream input = null;
        OutputStream output = null;
        final byte[] b4 = new byte[4];
        int count = 0;
        try {
            // open the output
            input = new FileInputStream(file);
           
            // Read the first four bytes, and determine charset encoding
            count = input.read(b4);
            encInfo = XmlCharsetDetector.getEncodingName(b4, count);
            response.setCharacterEncoding(encInfo.getEncoding() != null ? encInfo.getEncoding() : "UTF-8");
            
            // send out the first four bytes read
            output = response.getOutputStream();
            output.write(b4, 0, count);
        
            // copy the content to the output
            byte[] buffer = new byte[8192];
            int n = -1;
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
            }
        } finally {
            if(output != null) output.flush();
            if(input != null) input.close();
        }

        return null;
    }
}
