/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * Servlet Filter that performs URL translation on content based on configured mime types.
 * <p>
 * This filter does the job of a content filtering reverse proxy, like apache2 <code>mod_html</code>
 * , but meant to be used out of the box for situations where the UI needs to be exposed through a
 * proxy server but for one reason or another the external reverse proxy is not installed or can't
 * be configured to perform URL translation on contents.
 * </p>
 * <p>
 * <h2>Init parameters</h2>
 * <ul>
 * <li><b><code>enabled</code></b>: one of <code>true</code> or <code>false</code>, defaults to
 * <code>false</code>. Indicates whether to enable this filter or not.
 * <li><b><code>mime-types</code></b>: comma separated list of java regular expressions used to
 * match the response mime type and decide whether to perform URL translation on the response
 * content or not.
 * </ul>
 * </p>
 * <p>
 * <h2>Operation</h2>
 * This Filter uses the configured {@link GeoServer#getProxyBaseUrl() proxyBaseUrl} to translate the
 * URL's found in textual content whose MIME type matches one of the regular expressions provided
 * through the <code>"mime-types"</code> filter init parameter.
 * </p>
 * <p>
 * Sample translations: given GeoServer being running in a servlet engine at
 * <code>http://localhost:8080/geoserver</code> and the <code>proxyBaseUrl</code> configured as
 * <code>http://myserver/tools/geoserver</code>:
 * <ul>
 * <li><code>"http://localhost:8080/geoserver/welcome.do"</code> gets translated as
 * <code>"http://myserver/tools/geoserver/welcome.do"</code>
 * <li><code>"/geoserver/style.css"</code> gets translated as
 * <code>"/tools/geoserver/style.css"</code>
 * </ul>
 * </p>
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.5.x
 * @source $URL:
 *         https://svn.codehaus.org/geoserver/trunk/geoserver/web/src/main/java/org/geoserver/filters
 *         /ReverseProxyFilter.java $
 */
public class ReverseProxyFilter implements Filter {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.filters");

    /**
     * Name of the filter init parameter that indicates whether the filter is enabled or disabled
     */
    private static final String ENABLED_INIT_PARAM = "enabled";

    /**
     * The name of the filter init parameter that contains the comma separated list of regular
     * expressions used to match the response mime types to translate URL's for
     */
    private static final String MIME_TYPES_INIT_PARAM = "mime-types";

    private boolean filterIsEnabled;

    /**
     * The set of Patterns used to match response mime types
     */
    private final Set<Pattern> mimeTypePatterns = new HashSet<Pattern>();

    private GeoServer geoServer;

    /**
     * Parses the <code>mime-types</code> init parameter, which is a comma separated list of regular
     * expressions used to match the response mime types to decide whether to apply the URL
     * translation on content or not.
     */
    public void init(final FilterConfig filterConfig) throws ServletException {
        final String enabledInitParam = filterConfig.getInitParameter(ENABLED_INIT_PARAM);

        this.filterIsEnabled = Boolean.valueOf(enabledInitParam).booleanValue();
        if (filterIsEnabled) {
            final String mimeTypesInitParam = filterConfig.getInitParameter(MIME_TYPES_INIT_PARAM);

            GeoServer geoServerConfig = GeoServerExtensions.bean(GeoServer.class);
            if (geoServerConfig == null) {
                throw new ServletException("No " + GeoServer.class.getName()
                        + " found, the system is either not properly "
                        + "configured or the method to get to the GeoServer "
                        + "config instance have changed!");
            }
            this.geoServer = geoServerConfig;
            if (geoServerConfig.getSettings() == null) {
                throw new ServletException(
                        "No GeoServerInfo instance found. Needed to look for the proxy base URL");
            }
            Set<Pattern> patterns = parsePatterns(geoServerConfig, mimeTypesInitParam);
            this.mimeTypePatterns.addAll(patterns);
            LOGGER.finer("Reverse Proxy Filter configured");
        } else {
            LOGGER.fine("Reverse Proxy Filter is disabled by configuration");
        }
    }

    static Set<Pattern> parsePatterns(final GeoServer geoServer, final String mimeTypesInitParam)
            throws ServletException {

        final String[] split = mimeTypesInitParam.split(",");

        LOGGER.finer("Initializing Reverse Proxy Filter");
        Set<Pattern> mimeTypePatterns = new HashSet<Pattern>();
        try {
            for (int i = 0; i < split.length; i++) {
                String mimeTypeRegExp = split[i];
                LOGGER.finest("Registering mime type regexp for reverse proxy filter: "
                        + mimeTypeRegExp);
                Pattern mimeTypePattern = Pattern.compile(mimeTypeRegExp);
                mimeTypePatterns.add(mimeTypePattern);
            }
        } catch (PatternSyntaxException e) {
            throw new ServletException("Error compiling Reverse Proxy Filter mime-types: "
                    + e.getMessage(), e);
        }
        return mimeTypePatterns;
    }

    /**
     * Uses a response wrapper to evaluate the mime type set and if it matches one of the configured
     * mime types applies URL translation from internal URL's to proxified ones.
     * <p>
     * When a matching mime type is found, the full response is cached during
     * <code>chain.doFilter</code>, and the content is assumed to be textual in the
     * <code>response.getCharacterEncoding()</code> charset. If the mime type does not match any of
     * the configured ones no translation nor response cacheing is performed.
     * <p>
     * </p>
     * The URL translation is a two-step process, done line by line from the cached content and
     * written to the actual response output stream. It first translates the
     * <code>protocol://host:port</code> section of URL's and then replaces the servlet context from
     * the server URL by the proxy base URL context. This accounts for absolute urls as well as
     * relative, root based, urls as used in javascript code and css. </p>
     */
    public void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {
        LOGGER.finer("filtering " + ((HttpServletRequest) request).getRequestURL());
        if (!filterIsEnabled || !(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        final String proxyBaseUrl = geoServer.getSettings().getProxyBaseUrl();

        if (proxyBaseUrl == null || "".equals(proxyBaseUrl)) {
            chain.doFilter(request, response);
            return;
        }

        final CacheingResponseWrapper wrapper = new CacheingResponseWrapper(
                (HttpServletResponse) response, mimeTypePatterns);

        chain.doFilter(request, wrapper);

        wrapper.flushBuffer();

        if (wrapper.isCacheing()) {
            BufferedReader reader;
            {
                byte[] cachedContent = wrapper.getCachedContent();
                String cs = wrapper.getCharacterEncoding();
                reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(
                        cachedContent), cs));
            }
            PrintWriter writer = response.getWriter();
            // the request base url (eg, http://localhost:8080/)
            final String serverBase;
            // the proxy base url (eg, http://myproxyserver/)
            final String proxyBase;
            // the request context (eg, /geoserver/)
            final String context;
            // the proxy context (eg, /tools/geoserver/)
            final String proxyContext;
            final String baseUrl;
            {
                String _baseUrl = RequestUtils.baseURL((HttpServletRequest) request);
                if (_baseUrl.endsWith("/")) {
                    _baseUrl = _baseUrl.substring(0, _baseUrl.length() - 1);
                }
                baseUrl = _baseUrl;
                final URL base = new URL(baseUrl);
                final URL proxy = new URL(proxyBaseUrl);

                serverBase = getServerBase(base);
                proxyBase = getServerBase(proxy);

                context = getContext(base);
                proxyContext = getContext(proxy);
            }

            String line;
            String translatedLine;
            LOGGER.finer("translating " + ((HttpServletRequest) request).getRequestURI());
            while ((line = reader.readLine()) != null) {
                // ugh, we need to revert any already translated URL, like in the case
                // of the server config form where the proxyBaseUrl is set. Otherwise
                // it could be mangled
                if (line.indexOf(proxyBaseUrl) != -1) {
                    translatedLine = line.replaceAll(proxyBaseUrl, baseUrl);
                } else {
                    translatedLine = line;
                }

                // now apply the translation from servlet url to proxy url
                translatedLine = translatedLine.replaceAll(serverBase, proxyBase);
                translatedLine = translatedLine.replaceAll(context, proxyContext);
                if (LOGGER.isLoggable(Level.FINE)) {
                    if (!line.equals(translatedLine)) {
                        LOGGER.finest("translated '" + line + "'");
                        LOGGER.finest("        as '" + translatedLine + "'");
                    }
                }
                writer.println(translatedLine);
            }
            writer.flush();
        }
    }

    private String getContext(URL url) {
        String context = url.getPath();
        return context.endsWith("/") ? context : context + "/";
    }

    private String getServerBase(URL url) {
        StringBuffer sb = new StringBuffer();
        sb.append(url.getProtocol()).append("://");
        sb.append(url.getHost());
        if (url.getPort() != -1) {
            sb.append(":").append(url.getPort());
        }
        sb.append("/");
        return sb.toString();
    }

    public void destroy() {
    }

    /**
     * A servlet response wrapper that caches the content if its mime type matches one of the
     * provided patterns.
     * <p>
     * Whether to cache the content or not has to be decided when {@link #setContentType(String)} is
     * called, doing the pattern matching with the provided set of regular expression patterns. So
     * after using this response wrapper, {@link #isCacheing()} indicates whether content cache was
     * done, and if so, the cached content is accessed through {@link #getCachedContent()}.
     * </p>
     * 
     * @author Gabriel Roldan (TOPP)
     * @version $Id$
     * @since 2.5.x
     * @source $URL:
     *         https://svn.codehaus.org/geoserver/trunk/geoserver/web/src/main/java/org/geoserver
     *         /filters/ReverseProxyFilter.java $
     */
    private static class CacheingResponseWrapper extends HttpServletResponseWrapper {

        private Set<Pattern> cacheingMimeTypes;

        private boolean cacheContent;

        private DeferredCacheingOutputStream outputStream;

        private PrintWriter writer;

        /**
         * @param response
         *            the wrapped response
         * @param cacheingMimeTypes
         *            the patterns to do mime type matching with to decide whether to cache content
         *            or not
         */
        public CacheingResponseWrapper(final HttpServletResponse response,
                Set<Pattern> cacheingMimeTypes) {
            super(response);
            this.cacheingMimeTypes = cacheingMimeTypes;
            // we can't know until setContentType is called
            this.cacheContent = false;
        }

        /**
         * @return whether content cacheing has been accomplished or not after the response was
         *         used.
         */
        public boolean isCacheing() {
            return cacheContent;
        }

        /**
         * @return the cached contend, as long as <code>isCacheing() == true</code>
         */
        public byte[] getCachedContent() {
            return outputStream.getCachedContent();
        }

        /**
         * Among setting the response content type, determines whether the response content should
         * be cached or not, depending on the <code>mimeType</code> matching one of the patterns or
         * not.
         */
        @Override
        public void setContentType(final String mimeType) {
            Pattern p;
            for (Iterator<Pattern> it = cacheingMimeTypes.iterator(); it.hasNext();) {
                p = it.next();
                Matcher matcher = p.matcher(mimeType);
                if (matcher.matches()) {
                    cacheContent = true;
                    break;
                }
            }
            super.setContentType(mimeType);
        }

        @Override
        public void flushBuffer() throws IOException {
            if (cacheContent) {
                if (writer != null) {
                    writer.flush();
                }
                if (outputStream != null) {
                    outputStream.flush();
                }
            } else {
                super.flushBuffer();
            }
        }

        /**
         * Waits until the first write operation to decide whether to cache the contents or not.
         * This way it tolerates calls to {@link ServletResponse#getOutputStream()} before calling
         * {@link ServletResponse#setContentType(String)}.
         * 
         * @author Gabriel Roldan
         */
        private class DeferredCacheingOutputStream extends ServletOutputStream {
            /**
             * non null iif {@link CacheingResponseWrapper#isCacheing()} == false
             */
            private ServletOutputStream actualStream;

            /**
             * non null iif {@link CacheingResponseWrapper#isCacheing()} == true
             */
            private ByteArrayOutputStream cache;

            @Override
            public void write(int b) throws IOException {
                if (isCacheing()) {
                    if (cache == null) {
                        cache = new ByteArrayOutputStream();
                    }
                    cache.write(b);
                } else {
                    if (actualStream == null) {
                        actualStream = getOutputStreamInternal();
                    }
                    actualStream.write(b);
                }
            }

            public byte[] getCachedContent() {
                if (cache == null) {
                    // the request produced no content
                    return new byte[0];
                }
                return cache.toByteArray();
            }

            @Override
            public void flush() throws IOException {
                if (actualStream != null) {
                    actualStream.flush();
                }
            }
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStream == null) {
                outputStream = new DeferredCacheingOutputStream();
            }
            return outputStream;
        }

        private ServletOutputStream getOutputStreamInternal() throws IOException {
            return super.getOutputStream();
        }

        /**
         * The default behavior of this method is to return getWriter() on the wrapped response
         * object.
         */
        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                if (cacheContent) {
                    String charset = super.getCharacterEncoding();
                    writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), charset));
                } else {
                    writer = super.getWriter();
                }
            }
            return writer;
        }
    }
}
