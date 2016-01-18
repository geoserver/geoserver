/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
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
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.wicket.core.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
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
    @Override
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


}
