/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    public void onDispose() {
        // dispose the WMS Animator Executor Service
        shutdownAnimatorExecutorService();
    }

    public void beforeReload() {
        // nothing to do
    }

    public void onReload() {
        // clear the caches for good measure
        onReset();
    }

    public void onReset() {
        // kill the image caches
        Iterator<ExternalGraphicFactory> it =
                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
        while (it.hasNext()) {
            ExternalGraphicFactory egf = it.next();
            if (egf instanceof GraphicCache) {
                ((GraphicCache) egf).clearCache();
            }
        }

        // reloads the font cache
        reloadFontCache();

        // reset WMS Animator Executor Service
        resetAnimatorExecutorService();
    }

    /** Shutting down pending tasks and resetting the executor service timeout. */
    private void resetAnimatorExecutorService() {
        shutdownAnimatorExecutorService();

        Long framesTimeout =
                this.wmsConfig.getMaxAnimatorRenderingTime() != null
                        ? this.wmsConfig.getMaxAnimatorRenderingTime()
                        : Long.MAX_VALUE;
        ExecutorService animatorExecutorService =
                new ThreadPoolExecutor(
                        4,
                        20,
                        framesTimeout,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>());

        this.wmsConfig.setAnimatorExecutorService(animatorExecutorService);
    }

    /** Suddenly shuts down the Animator Executor Service */
    private void shutdownAnimatorExecutorService() {
        final ExecutorService animatorExecutorService = this.wmsConfig.getAnimatorExecutorService();
        if (animatorExecutorService != null && !animatorExecutorService.isShutdown()) {
            animatorExecutorService.shutdownNow();
        }
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
        List<Font> result = new ArrayList<Font>();
        for (Resource file :
                Resources.list(
                        data.getStyles(), new Resources.ExtensionFilter("TTF", "OTF"), true)) {
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

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            reloadFontCache();

            // reset WMS Animator Executor Service
            resetAnimatorExecutorService();
        }
    }
}
