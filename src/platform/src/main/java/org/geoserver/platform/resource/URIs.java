/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import org.geotools.util.URLs;

/**
 * Use a URI as a resource (only for reading, most operations are unsupported).
 *
 * @author Niels Charlier
 */
public final class URIs {

    private URIs() {}

    static class ResourceAdaptor implements Resource {

        private URL url;

        public ResourceAdaptor(URL url) {
            this.url = url;
        }

        public URL getURL() {
            return url;
        }

        @Override
        public String path() {
            return url.getPath();
        }

        @Override
        public String name() {
            String path = url.getPath();
            return path.substring(path.lastIndexOf("/") + 1);
        }

        @Override
        public Lock lock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(ResourceListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeListener(ResourceListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream in() {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public OutputStream out() {
            throw new UnsupportedOperationException();
        }

        @Override
        public File file() {
            return URLs.urlToFile(url);
        }

        @Override
        public File dir() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long lastmodified() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource parent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Resource get(String resourcePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Resource> list() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Type getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean renameTo(Resource dest) {
            throw new UnsupportedOperationException();
        }
    }

    public static Resource asResource(URI uri) throws MalformedURLException {
        return new ResourceAdaptor(uri.toURL());
    }

    public static Resource asResource(URL url) {
        return new ResourceAdaptor(url);
    }
}
