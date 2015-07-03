/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.resource.PropertiesFactory.IPropertiesLoader;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IFixedLocationResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.time.Time;
import org.geotools.util.logging.Logging;

/**
 * A custom resource stream locator which supports loading i18n properties files on a single file
 * per module basis. It also works around https://issues.apache.org/jira/browse/WICKET-2534
 */
public class GeoServerResourceStreamLocator extends ResourceStreamLocator {
    public static Logger LOGGER = Logging.getLogger("org.geoserver.web");

    static Pattern GS_PROPERTIES = Pattern.compile("GeoServerApplication.*.properties");

    static Pattern GS_LOCAL_I18N = Pattern.compile("org/geoserver/.*(\\.properties|\\.xml)]");

    @SuppressWarnings( { "unchecked", "serial" })
    public IResourceStream locate(Class clazz, String path) {
        int i = path.lastIndexOf("/");
        if (i != -1) {
            String p = path.substring(i + 1);
            if (GS_PROPERTIES.matcher(p).matches()) {
                try {
                    // process the classpath for property files
                    Enumeration<URL> urls = getClass().getClassLoader().getResources(p);

                    // build up a single properties file
                    Properties properties = new Properties();

                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();

                        InputStream in = url.openStream();
                        properties.load(in);
                        in.close();
                    }

                    // transform the properties to a stream
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    properties.store(out, "");

                    return new AbstractResourceStream() {
                        public InputStream getInputStream() throws ResourceStreamNotFoundException {
                            return new ByteArrayInputStream(out.toByteArray());
                        }

                        public void close() throws IOException {
                            out.close();
                        }
                    };
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "", e);
                }
            } else if (GS_LOCAL_I18N.matcher(path).matches()) {
                return null;
            } else if (path.matches("org/geoserver/.*" + clazz.getName() + ".*_.*.html")) {
                return null;
            }
        }

        return super.locate(clazz, path);
    }

    /**
     * Search the the resource my means of the various classloaders available
     * 
     * @param clazz
     * @param path
     * @return resource stream
     */
    protected IResourceStream locateByClassLoader(final Class clazz, final String path) {
        ClassLoader classLoader = null;
        if (clazz != null) {
            classLoader = clazz.getClassLoader();
        }

        if (classLoader == null) {
            // use context classloader when no specific classloader is set
            // (package resources for instance)
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        if (classLoader == null) {
            // use Wicket classloader when no specific classloader is set
            classLoader = getClass().getClassLoader();
        }

        // Try loading path using classloader
        final URL url = classLoader.getResource(path);
        if (url != null) {
            return new UrlResourceStream(url);
        }
        return null;
    }
    
    /*
     * Licensed to the Apache Software Foundation (ASF) under one or more
     * contributor license agreements.  See the NOTICE file distributed with
     * this work for additional information regarding copyright ownership.
     * The ASF licenses this file to You under the Apache License, Version 2.0
     * (the "License"); you may not use this file except in compliance with
     * the License.  You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /**
     * This is a modified version of Wicket own {@link UrlResourceStream} that has
     * been modified to work around https://issues.apache.org/jira/browse/WICKET-2534 without
     * having to upgrade to Wicket 1.4
     * 
     * @see org.apache.wicket.util.resource.IResourceStream
     * @see org.apache.wicket.util.watch.IModifiable
     * @author Jonathan Locke
     */
    static class UrlResourceStream extends AbstractResourceStream implements
            IFixedLocationResourceStream {
        private static final long serialVersionUID = 1L;

        /** Resource stream. */
        private transient InputStream inputStream;

        /** The URL to this resource. */
        private final URL url;

        /**
         * the handle to the file if it is a file resource
         */
        private File file;

        /** Length of stream. */
        private int contentLength;

        /** Content type for stream. */
        private String contentType;

        /** Last known time the stream was last modified. */
        private long lastModified;

        /**
         * Construct.
         * 
         * @param url
         *            URL of resource
         */
        public UrlResourceStream(final URL url) {
            // Save URL
            this.url = url;
            URLConnection connection = null;
            try {
                connection = url.openConnection();
                contentLength = connection.getContentLength();
                contentType = connection.getContentType();
                try {
                    file = new File(new URI(url.toExternalForm()));
                } catch (Exception ex) {
                    LOGGER.fine("cannot convert url: " + url + " to file (" + ex.getMessage()
                            + "), falling back to the inputstream for polling");
                }
                if (file != null && !file.exists()) {
                    file = null;
                }
            } catch (IOException ex) {
                // It should be impossible to get here or the original URL
                // couldn't have been constructed. But we re-throw with details
                // anyway.
                final IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
                        "Invalid URL parameter " + url);
                illegalArgumentException.initCause(ex);
                throw illegalArgumentException;
            } finally {
                // if applicable, disconnect
                if (connection != null) {
                    if (connection instanceof HttpURLConnection) {
                        ((HttpURLConnection) connection).disconnect();
                    } else {
                        try {
                            connection.getInputStream().close();
                        } catch (Exception ex) {
                            // ignore
                        }
                    }
                }
            }
        }

        /**
         * Closes this resource.
         * 
         * @throws IOException
         */
        public void close() throws IOException {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        }

        /**
         * @return The content type of this resource, such as "image/jpeg" or "text/html"
         */
        public String getContentType() {
            testContentType();
            return contentType;
        }

        /**
         * Method to test the content type on null or unknown. if this is the case the content type
         * is tried to be resolved throw the servlet context
         */
        private void testContentType() {
            if (contentType == null || contentType.indexOf("unknown") != -1) {
                Application application = Application.get();
                if (application instanceof WebApplication) {
                    // TODO Post 1.2: General: For non webapplication another method
                    // should be implemented (getMimeType on application?)
                    contentType = ((WebApplication) application).getServletContext().getMimeType(
                            url.getFile());
                    if (contentType == null) {
                        contentType = URLConnection.getFileNameMap().getContentTypeFor(
                                url.getFile());
                    }
                } else {
                    contentType = URLConnection.getFileNameMap().getContentTypeFor(url.getFile());
                }
            }
        }

        /**
         * @return A readable input stream for this resource.
         * @throws ResourceStreamNotFoundException
         */
        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            if (inputStream == null) {
                try {
                    inputStream = url.openStream();
                } catch (IOException e) {
                    throw new ResourceStreamNotFoundException("Resource " + url
                            + " could not be opened", e);
                }
            }

            return inputStream;
        }

        /**
         * @return The URL to this resource (if any)
         */
        public URL getURL() {
            return url;
        }

        /**
         * @see org.apache.wicket.util.watch.IModifiable#lastModifiedTime()
         * @return The last time this resource was modified
         */
        public Time lastModifiedTime() {
            if (file != null) {
                long lastModified = file.lastModified();
                if (lastModified != this.lastModified) {
                    this.lastModified = lastModified;
                    contentLength = (int) file.length();
                }
            } else {
                URLConnection urlConnection = null;
                boolean close = false;
                try {
                    urlConnection = url.openConnection();
                    long lastModified = this.lastModified;
                    if (urlConnection instanceof JarURLConnection) {
                        JarURLConnection jarUrlConnection = (JarURLConnection) urlConnection;
                        URL jarFileUrl = jarUrlConnection.getJarFileURL();
                        URLConnection jarFileConnection = jarFileUrl.openConnection();
                        try {
                            lastModified = jarFileConnection.getLastModified();
                        } finally {
                            jarFileConnection.getInputStream().close();
                        }
                    } else {
                        close = true;
                        lastModified = urlConnection.getLastModified();
                    }
                    // update the last modified time.
                    if (lastModified != this.lastModified) {
                        this.lastModified = lastModified;
                        close = true;
                        contentLength = urlConnection.getContentLength();
                    }
                } catch (IOException e) {
                    if (url.toString().indexOf(".jar!") >= 0) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER
                                    .fine("getLastModified for " + url + " failed: "
                                            + e.getMessage());
                        }
                    } else {
                        LOGGER.severe("getLastModified for " + url + " failed: " + e.getMessage());
                    }
                } finally {
                    // if applicable, disconnect
                    if (urlConnection != null) {
                        if (urlConnection instanceof HttpURLConnection) {
                            ((HttpURLConnection) urlConnection).disconnect();
                        } else if (close) {
                            try {
                                urlConnection.getInputStream().close();
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    }
                }
            }
            return Time.milliseconds(lastModified);
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return url.toString();
        }

        /**
         * @see org.apache.wicket.util.resource.IResourceStream#length()
         */
        public long length() {
            return contentLength;
        }

        /**
         * @see org.apache.wicket.util.resource.IFixedLocationResourceStream#locationAsString()
         */
        public String locationAsString() {
            return url.toExternalForm();
        }
    }

}
