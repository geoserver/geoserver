/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.FontCache;
import org.geotools.renderer.style.GraphicCache;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Drops imaging caches
 *
 * @author Andrea Aime - OpenGeo
 */
public class WMSLifecycleHandler implements GeoServerLifecycleHandler, ApplicationListener {

    static final Logger LOGGER = Logging.getLogger(WMSLifecycleHandler.class);

    GeoServerDataDirectory data;
    WMS wmsConfig;

    public WMSLifecycleHandler(GeoServerDataDirectory data, WMS wmsConfig) {
        this.data = data;
        this.wmsConfig = wmsConfig;
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        // clear the caches for good measure
        onReset();
    }

    @Override
    public void onReset() {
        // kill the image caches
        Iterator<ExternalGraphicFactory> it = DynamicSymbolFactoryFinder.getExternalGraphicFactories();
        while (it.hasNext()) {
            ExternalGraphicFactory egf = it.next();
            if (egf instanceof GraphicCache cache) {
                cache.clearCache();
            }
        }

        // reloads the font cache
        reloadFontCache();
    }

    void reloadFontCache() {
        List<Font> fonts = loadFontsFromDataDirectory();
        final FontCache cache = FontCache.getDefaultInstance();
        cache.resetCache();
        for (Font font : fonts) {
            cache.registerFont(font);
        }
    }

    List<Font> loadFontsFromDataDirectory() {
        List<Font> result = new ArrayList<>();
        for (Resource file : Resources.list(data.getStyles(), new Resources.ExtensionFilter("TTF", "OTF"), true)) {
            try {
                final Font font = Font.createFont(Font.TRUETYPE_FONT, file.file());
                result.add(font);
                LOGGER.log(
                        Level.INFO,
                        "Loaded font file "
                                + file
                                + ", loaded font '"
                                + font.getName()
                                + "' in family '"
                                + font.getFamily()
                                + "'");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load font file " + file, e);
            }
        }

        return result;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            reloadFontCache();
        }
    }
}
