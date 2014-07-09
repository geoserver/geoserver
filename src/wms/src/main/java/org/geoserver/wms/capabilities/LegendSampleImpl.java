/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import it.geosolutions.imageio.plugins.png.PNGWriter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphic;
import org.geoserver.wms.legendgraphic.PNGLegendOutputFormat;
import org.opengis.feature.type.FeatureType;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.PngReader;

/**
 * Default implementation of LegendSample. Implements samples caching to disk
 * (sidecar png file for each sld file), to be able to read the dimensions when
 * needed. Implements CatalogListener to be notified of style changes and update
 * the cache accordingly and GeoServerLifecycleHandler to handle catalog reload
 * events.
 * 
 * @author Mauro Bartolomeoli (mauro.bartolomeoli @ geo-solutions.it)
 */
public class LegendSampleImpl implements CatalogListener, LegendSample,
        GeoServerLifecycleHandler {

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(LegendSampleImpl.class.getPackage().getName());
    
    private static final String DEFAULT_SAMPLE_FORMAT = "png";
    
    private Catalog catalog;
    
    private GeoServerResourceLoader loader;
    
    private Set<String> invalidated = new HashSet<String>();
    
    private static enum FileType {
        SLD, SAMPLE
    };
    
    public LegendSampleImpl(Catalog catalog, GeoServerResourceLoader loader) {
        super();
        this.catalog = catalog;
        this.loader = loader;
        this.clean();
        this.catalog.addListener(this);
    }
    
    /**
     * Clean up no more valid samples: SLD updated from latest sample creation.
     */
    private void clean() {
        for (StyleInfo style : catalog.getStyles()) {
            synchronized (style) {
                File styleFile = getStyleResource(style, FileType.SLD).file();
                Resource sampleResource = getStyleResource(style, FileType.SAMPLE);
                // remove old samples
                if (styleFile.exists()
                        && sampleResource.getType() == Resource.Type.RESOURCE
                        && styleFile.lastModified() > sampleResource.file()
                                .lastModified()) {
                    sampleResource.file().delete();
                }
            }
        }
        invalidated = new HashSet<String>();
    }
    
    /**
     * Gets a file object for style related files: SLD or SAMPLE.
     * 
     * @param style
     * @param type
     * @return
     */
    private Resource getStyleResource(StyleInfo style, FileType type) {
        String[] prefix = new String[0];
        if (style.getWorkspace() != null) {
            prefix = new String[] { "workspaces", style.getWorkspace().getName() };
        }
        String fileName = type == FileType.SLD ? style.getFilename() : style
                .getName() + "." + DEFAULT_SAMPLE_FORMAT;
        String[] pathParts = (String[]) ArrayUtils.addAll(prefix, new String[] {
                "styles", fileName });
        String path = Paths.path(pathParts);
        return loader.get(path);
    }
    
    /**
     * Calculates legendURL size (width x height) for the given style.
     * 
     * @param style
     * @return
     */
    public Dimension getLegendURLSize(StyleInfo style) {
        synchronized (style) {
            GetLegendGraphicOutputFormat pngOutputFormat = new PNGLegendOutputFormat();
    
            Resource sampleLegend = getStyleResource(style, FileType.SAMPLE);
            if (sampleLegend.getType() == Resource.Type.RESOURCE
                    && !isStyleSampleInvalid(style)) {
                // using existing sample if sld has not been updated from
                // latest sample update
                return getSizeFromSample(sampleLegend);
            } else {
                // generates a new icon, and save it on disk (aside of the sld) for
                // later usage
                return createNewSample(style, pngOutputFormat, sampleLegend);
            }
        }
    }
    
    private Dimension createNewSample(StyleInfo style,
            GetLegendGraphicOutputFormat pngOutputFormat, Resource sampleLegend) {
        GetLegendGraphicRequest legendGraphicRequest = new GetLegendGraphicRequest();
        PNGWriter writer = null;
    
        try {
            legendGraphicRequest.setLayers(Arrays.asList((FeatureType) null));
            legendGraphicRequest.setStyles(Arrays.asList(style.getStyle()));
            legendGraphicRequest.setFormat(pngOutputFormat.getContentType());
            Object legendGraphic = pngOutputFormat
                    .produceLegendGraphic(legendGraphicRequest);
            if (legendGraphic instanceof BufferedImageLegendGraphic) {
                BufferedImage image = ((BufferedImageLegendGraphic) legendGraphic)
                        .getLegend();
    
                writer = new PNGWriter();
                OutputStream outStream = null;
                try {
                    outStream = sampleLegend.out();
                    writer.writePNG(image, outStream, 0.0f, FilterType.FILTER_NONE);
                    removeStyleSampleInvalidation(style);
                    return new Dimension(image.getWidth(), image.getHeight());
                } finally {
                    outStream.close();
                }
    
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering sample legend", e);
        }
        return null;
    }
    
    private Dimension getSizeFromSample(Resource sampleLegend) {
        File sampleLegendFile = sampleLegend.file();
    
        PngReader pngReader = null;
        try {
            // reads size using PNGJ reader, that can read metadata without reading
            // the full image
            pngReader = new PngReader(sampleLegendFile);
            return new Dimension(pngReader.imgInfo.cols, pngReader.imgInfo.rows);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot read sample legend image from "
                    + sampleLegendFile.getAbsolutePath(), e);
            return null;
        } finally {
            if (pngReader != null) {
                pngReader.close();
            }
        }
    }
    
    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
    
    }
    
    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            // invalidate removed styles (is this needed?)
            invalidateStyleSample((StyleInfo) event.getSource());
        }
    }
    
    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
    
    }
    
    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event)
            throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            // invalidate updated styles
            invalidateStyleSample((StyleInfo) event.getSource());
        }
    }
    
    /**
     * Set the given style sample as invalid.
     * 
     * @param event
     */
    private void invalidateStyleSample(StyleInfo style) {
        synchronized (style) {
            invalidated.add(getStyleName(style));
        }
    }
    
    /**
     * Remove the given style sample from invalid ones.
     * 
     * @param style
     */
    private void removeStyleSampleInvalidation(StyleInfo style) {
        invalidated.remove(getStyleName(style));
    }
    
    /**
     * Checks if the given style sample is marked as invalid.
     * 
     * @param style
     * @return
     */
    private boolean isStyleSampleInvalid(StyleInfo style) {
        synchronized (style) {
            return invalidated.contains(getStyleName(style));
        }
    }
    
    /**
     * Gets a unique name for a style, considering the workspace definition, in the
     * form worspacename:stylename (or stylename if the style is global).
     * 
     * @param styleInfo
     * @return
     */
    private String getStyleName(StyleInfo styleInfo) {
        return styleInfo.getWorkspace() != null ? (styleInfo.getWorkspace()
                .getName() + ":" + styleInfo.getName()) : styleInfo.getName();
    }
    
    @Override
    public void reloaded() {
        clean();
    }
    
    @Override
    public void onReset() {
    
    }
    
    @Override
    public void onDispose() {
    
    }
    
    @Override
    public void beforeReload() {
    
    }
    
    @Override
    public void onReload() {
        reloaded();
    }

}
