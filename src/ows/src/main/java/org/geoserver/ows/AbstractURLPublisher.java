/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import org.geoserver.ows.util.EncodingInfo;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geotools.util.URLs;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Controller which publishes files through a web interface from the classpath
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
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractURLPublisher extends AbstractController {

    protected boolean replaceWindowsFileSeparator = false;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String reqPath = getRequestPath(request);
        URL url = getUrl(request, reqPath);

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

        String filename = new File(url.getFile()).getName();
        String mime = getMimeType(reqPath, filename);
        response.setContentType(mime);
        String dispositionType = isAttachment(reqPath, filename, mime) ? "attachment" : "inline";
        response.setHeader(
                "Content-Disposition",
                ContentDisposition.builder(dispositionType)
                        .filename(filename)
                        .build()
                        .toString());

        // set the content length and content type
        URLConnection connection = url.openConnection();
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
        final byte[] b4 = new byte[4];
        int count = 0;
        try (InputStream input = connection.getInputStream()) {
            // Read the first four bytes, and determine charset encoding
            count = input.read(b4);
            encInfo = XmlCharsetDetector.getEncodingName(b4, count);
            response.setCharacterEncoding(encInfo.getEncoding() != null ? encInfo.getEncoding() : "UTF-8");

            // count < 1 -> empty file
            if (count > 0) {
                // send out the first four bytes read
                @SuppressWarnings("PMD.CloseResource") // managed by servlet container
                OutputStream output = response.getOutputStream();
                output.write(b4, 0, count);

                // copy the content to the output
                byte[] buffer = new byte[8192];
                int n = -1;
                while ((n = input.read(buffer)) != -1) {
                    output.write(buffer, 0, n);
                }
            }
        }

        return null;
    }

    private boolean checkNotModified(HttpServletRequest request, long timeStamp) {
        Enumeration headers = request.getHeaders("If-Modified-Since");
        String header = headers != null && headers.hasMoreElements()
                ? headers.nextElement().toString()
                : null;
        if (header != null && !header.isEmpty()) {
            long ifModSinceSeconds = lastModified(header);
            // the HTTP header has second precision
            long timeStampSeconds = 1000 * (timeStamp / 1000);
            return ifModSinceSeconds >= timeStampSeconds;
        }
        return false;
    }

    private String getRequestPath(HttpServletRequest request) throws IOException {
        String reqPath = URLDecoder.decode(request.getRequestURI(), "UTF-8")
                .substring(request.getContextPath().length());
        if (this.replaceWindowsFileSeparator) {
            reqPath = reqPath.replace(File.separatorChar, '/');
        }
        if (Arrays.stream(reqPath.split("/")).anyMatch(".."::equals)) {
            throw new IllegalArgumentException("Contains invalid '..' path: " + reqPath);
        }
        return reqPath;
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

    /** Can be overridden to replace specific mime types to mitigate potential XSS issues with certain resources */
    protected String getMimeType(String reqPath, String filename) {
        // set the mime if known by the servlet container, otherwise default to
        // application/octet-stream to mitigate potential cross-site scripting
        return Optional.ofNullable(getServletContext())
                .map(sc -> sc.getMimeType(filename))
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /**
     * Can be overridden to set the Content-Disposition: attachment to mitigate potential XSS issues with certain
     * resources
     */
    protected boolean isAttachment(String reqPath, String filename, String mime) {
        return false;
    }

    /** Retrieves the resource URL from the specified request */
    protected abstract URL getUrl(HttpServletRequest request, String reqPath) throws IOException;
}
