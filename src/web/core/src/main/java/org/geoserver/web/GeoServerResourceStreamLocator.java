/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.apache.wicket.core.util.resource.locator.ResourceNameIterator;
import org.apache.wicket.core.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.geotools.util.logging.Logging;

/**
 * A custom resource stream locator which supports loading i18n properties files on a single file
 * per module basis. It also works around https://issues.apache.org/jira/browse/WICKET-2534.
 *
 * <p>This class also tries to optimize resource lookups (which are slower with the wicket 7
 * upgrade) by skipping the mechanism that looks up static files (markup, css, etc...) with the
 * locale specific prefixes.
 */
public class GeoServerResourceStreamLocator extends ResourceStreamLocator {
    public static Logger LOGGER = Logging.getLogger("org.geoserver.web");

    static Pattern GS_PROPERTIES = Pattern.compile("GeoServerApplication.*.properties");

    static Pattern GS_LOCAL_I18N = Pattern.compile("org/geoserver/.*(\\.properties|\\.xml)]");

    @SuppressWarnings({"serial"})
    @Override
    public IResourceStream locate(Class<?> clazz, String path) {
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

    static Map<String, List<String>> PREFIXES = new HashMap<>();

    static {
        PREFIXES.put("html", Arrays.asList("html"));
        PREFIXES.put("css", Arrays.asList("css"));
        PREFIXES.put("png", Arrays.asList("png"));
        PREFIXES.put("js", Arrays.asList("js"));
        PREFIXES.put("ico", Arrays.asList("ico"));
    }

    @Override
    public IResourceNameIterator newResourceNameIterator(
            String path,
            Locale locale,
            String style,
            String variation,
            String extension,
            boolean strict) {

        Iterable<String> extensions = null;

        // if the resource under the geoserver or wicket namespace?
        if (path.startsWith("org/geoserver") || path.startsWith("org/apache/wicket")) {
            String ext = extension;
            if (ext == null) {
                // no extension passed in, strip it from the path
                ext = FilenameUtils.getExtension(path);
            }

            if (ext != null) {
                // we have an extension, look it up in the whitelist
                extensions = PREFIXES.get(ext);
            }
        }

        if (extensions != null) {
            // ensure the path doesn't contain the extension, sometimes this method is called with
            // extension == null,
            // in which case the extension is usually in the path
            path =
                    FilenameUtils.getPathNoEndSeparator(path)
                            + "/"
                            + FilenameUtils.getBaseName(path);
            return new ResourceNameIterator(path, style, variation, null, extensions, false);
        }

        // couldn't optimize, just pass through to parent
        return super.newResourceNameIterator(path, locale, style, variation, extension, strict);
    }
}
