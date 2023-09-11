/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.ResourceLocator;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.util.logging.Logging;

class StyleResourceCollector extends AbstractStyleVisitor {

    static final Logger LOGGER = Logging.getLogger(StyleResourceCollector.class);

    private ResourceLocator locator;
    private Map<String, GeoPkgSymbolImage> resources = new HashMap<>();
    private final String symbolPrefix;
    private int symbolId = 0;

    public StyleResourceCollector(ResourceLocator locator, String symbolPrefix) {
        this.locator = locator;
        this.symbolPrefix = symbolPrefix;
    }

    @Override
    public void visit(ExternalGraphic exgr) {
        String uri = exgr.getURI();
        URL location = locator.locateResource(uri);

        if (location != null) {
            byte[] contents;
            try (InputStream is = location.openStream()) {
                contents = IOUtils.toByteArray(is);
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "Cannot retrieve external graphic source "
                                + location
                                + " for original uri "
                                + uri,
                        e);
                return;
            }

            String symbolUri = symbolPrefix + symbolId;
            GeoPkgSymbol symbol = new GeoPkgSymbol(uri, null, symbolUri);
            GeoPkgSymbolImage image =
                    new GeoPkgSymbolImage(exgr.getFormat(), contents, symbolUri, symbol);
            symbolId++;
            resources.put(uri, image);
        }
    }

    public Map<String, GeoPkgSymbolImage> getResources() {
        return Collections.unmodifiableMap(resources);
    }
}
