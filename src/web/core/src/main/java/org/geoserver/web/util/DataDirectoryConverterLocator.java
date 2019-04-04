/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.IConverterLocator;
import org.apache.wicket.util.convert.IConverter;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

/**
 * Special converter locator which can resolve relative urls relative to the GeoServer data
 * directory.
 *
 * <p>This converter locator will turn URL's of the form "file:data/..." into full path URL's such
 * as "file://var/lib/geoserver/data/...".
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
@SuppressWarnings("serial")
public class DataDirectoryConverterLocator implements IConverterLocator {

    static final Logger LOGGER = Logging.getLogger(DataDirectoryConverterLocator.class);
    GeoServerResourceLoader resourceLoader;

    public DataDirectoryConverterLocator(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @SuppressWarnings("unchecked")
    public <C> IConverter<C> getConverter(Class<C> type) {
        if (File.class.isAssignableFrom(type)) {
            return (IConverter<C>) new FileLocator();
        }
        if (URL.class.isAssignableFrom(type)) {
            return (IConverter<C>) new URLLocator();
        }
        if (URI.class.isAssignableFrom(type)) {
            return (IConverter<C>) new URILocator();
        }

        return null;
    }

    File toFile(String value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        try {
            // first try as a url to strip off url protocol prefix
            try {
                URL url = new URL(value);
                if ("file".equals(url.getProtocol())) {
                    value = url.getFile();
                }
            } catch (MalformedURLException e) {
            }

            File file = new File(value);
            if (file.isAbsolute()) {
                return file;
            }

            return resourceLoader.find(value);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to File", e);
        }

        return null;
    }

    String fromFile(File file) {
        File data = resourceLoader.getBaseDirectory();

        // figure out if the file is a child of the base data directory
        List<String> path = new ArrayList<String>();
        boolean isChild = false;
        while (file != null) {
            if (file.equals(data)) {
                isChild = true;
                break;
            }

            path.add(file.getName());
            file = file.getParentFile();
        }

        if (isChild) {
            StringBuffer b = new StringBuffer("file:");
            for (int i = path.size() - 1; i > -1; i--) {
                b.append(path.get(i)).append(File.separatorChar);
            }
            b.setLength(b.length() - 1);
            return b.toString();
        }

        return null;
    }

    class FileLocator implements IConverter<File> {

        public File convertToObject(String value, Locale locale) {
            return toFile(value);
        }

        public String convertToString(File value, Locale locale) {
            return fromFile((File) value);
        }
    }

    class URLLocator implements IConverter<URL> {

        public URL convertToObject(String value, Locale locale) {
            File file = toFile(value);
            if (file != null) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to URL", e);
                }
            }

            return null;
        }

        public String convertToString(URL value, Locale locale) {
            String file = value.getFile();
            if (file != null && !"".equals(file)) {
                return fromFile(new File(value.getFile()));
            }
            return null;
        }
    }

    class URILocator implements IConverter<URI> {

        public URI convertToObject(String value, Locale locale) {
            File file = toFile(value);
            if (file != null) {
                return file.toURI();
            }
            return null;
        }

        public String convertToString(URI value, Locale locale) {
            try {
                return new URLLocator().convertToString(value.toURL(), locale);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error converting \"" + value + "\" to URI", e);
                return null;
            }
        }
    }
}
