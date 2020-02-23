/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.util.EncodingInfo;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geotools.util.URLs;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Controller which publishes files through a web interface from the classpath
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
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractURLPublisher extends AbstractController {

    protected ModelAndView handleRequestInternal(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        URL url = getUrl(request);

        // if not found return a 404
        if (url == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        File file = URLs.urlToFile(url);
        if (file != null && file.exists() && file.isDirectory()) {
            String uri = request.getRequestURI();
            uri += uri.endsWith("/") ? "index.html" : "/index.html";

            response.addHeader("Location", uri);
            response.sendError(HttpServletResponse.SC_MOVED_TEMPORARILY);

            return null;
        }

        if (file != null && checkNotModified(request, file.lastModified())) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return null;
        }

        // set the mime if known by the servlet container, set nothing otherwise
        // (Tomcat behaves like this when it does not recognize the file format)
        String mime =
                Optional.ofNullable(getServletContext())
                        .map(sc -> sc.getMimeType(new File(url.getFile()).getName()))
                        .orElse(null);
        if (mime != null) {
            response.setContentType(mime);
        }

        // set the content length and content type
        URLConnection connection = null;
        InputStream input = null;
        try {
            connection = url.openConnection();
            long length = connection.getContentLength();
            if (length > 0 && length <= Integer.MAX_VALUE) {
                response.setContentLength((int) length);
            }

            long lastModified = connection.getLastModified();
            if (lastModified > 0) {
                response.setHeader("Last-Modified", lastModified(lastModified));
            }

            // Guessing the charset (and closing the stream)
            EncodingInfo encInfo = null;
            OutputStream output = null;
            final byte[] b4 = new byte[4];
            int count = 0;
            // open the output
            input = connection.getInputStream();

            // Read the first four bytes, and determine charset encoding
            count = input.read(b4);
            encInfo = XmlCharsetDetector.getEncodingName(b4, count);
            response.setCharacterEncoding(
                    encInfo.getEncoding() != null ? encInfo.getEncoding() : "UTF-8");

            // count < 1 -> empty file
            if (count > 0) {
                // send out the first four bytes read
                output = response.getOutputStream();
                output.write(b4, 0, count);

                // copy the content to the output
                byte[] buffer = new byte[8192];
                int n = -1;
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            }
        } finally {
            if (input != null) input.close();
        }

        return null;
    }

    private boolean checkNotModified(HttpServletRequest request, long timeStamp) {
        Enumeration headers = request.getHeaders("If-Modified-Since");
        String header =
                headers != null && headers.hasMoreElements()
                        ? headers.nextElement().toString()
                        : null;
        if (header != null && header.length() > 0) {
            long ifModSinceSeconds = lastModified(header);
            // the HTTP header has second precision
            long timeStampSeconds = 1000 * (timeStamp / 1000);
            return ifModSinceSeconds >= timeStampSeconds;
        }
        return false;
    }

    static String lastModified(long timeStamp) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(new Date(timeStamp)) + " GMT";
    }

    static long lastModified(String timeStamp) {
        long ifModifiedSince = Long.MIN_VALUE;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.ENGLISH);
            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            ifModifiedSince = fmt.parse(timeStamp).getTime();
        } catch (ParseException pe) {
            // dang
        }
        // the HTTP header has second precision
        return 1000 * (ifModifiedSince / 1000);
    }

    /** Retrieves the resource URL from the specified request */
    protected abstract URL getUrl(HttpServletRequest request) throws IOException;
}
