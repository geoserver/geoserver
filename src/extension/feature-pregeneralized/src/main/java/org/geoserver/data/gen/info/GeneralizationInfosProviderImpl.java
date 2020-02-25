/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.gen.info;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;

/**
 * The default implementation for GeneralizationInfosProvider, reading the info from an XML file.
 *
 * <p>The xml schema file is "/geninfos_1.0.xsd"
 *
 * @author Christian Mueller
 */
public class GeneralizationInfosProviderImpl
        extends org.geotools.data.gen.info.GeneralizationInfosProviderImpl {

    protected URL deriveURLFromSourceObject(Object source) throws IOException {
        if (source == null) {
            throw new IOException("Cannot read from null");
        }

        if (source instanceof String) {
            String path = (String) source;

            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            File f =
                    Resources.find(
                            Resources.fromURL(Files.asResource(loader.getBaseDirectory()), path),
                            true);
            URL url = null;
            if (f != null && f.exists()) {
                url = f.toURI().toURL();
            } else {
                url = new URL(path);
            }
            url = new URL(URLDecoder.decode(url.toExternalForm(), "UTF8"));
            return url;
        }
        throw new IOException("Cannot read from " + source);
    }
}
