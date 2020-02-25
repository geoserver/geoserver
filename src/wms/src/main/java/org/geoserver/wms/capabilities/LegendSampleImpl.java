/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.PngReader;
import it.geosolutions.imageio.plugins.png.PNGWriter;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
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
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphic;
import org.geoserver.wms.legendgraphic.LegendGraphic;
import org.geoserver.wms.legendgraphic.PNGLegendOutputFormat;
import org.opengis.feature.type.FeatureType;

/**
 * Default implementation of LegendSample. Implements samples caching to disk (png file on a
 * dedicated data_dir folder for each sld file), to be able to read the dimensions when needed.
 * Implements CatalogListener to be notified of style changes and update the cache accordingly and
 * GeoServerLifecycleHandler to handle catalog reload events.
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli @ geo-solutions.it)
 */
public class LegendSampleImpl implements CatalogListener, LegendSample, GeoServerLifecycleHandler {

    public static final String LEGEND_SAMPLES_FOLDER = "legendsamples";

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    LegendSampleImpl.class.getPackage().getName());

    private static final String DEFAULT_SAMPLE_FORMAT = "png";

    private Catalog catalog;

    private GeoServerResourceLoader loader;

    private Set<String> invalidated = new HashSet<String>();

    Resource baseDir;

    public LegendSampleImpl(Catalog catalog, GeoServerResourceLoader loader) {
        super();
        this.catalog = catalog;
        this.loader = loader;
        this.baseDir = loader.get(Paths.BASE);
        this.clean();
        this.catalog.addListener(this);
    }

    /** Clean up no more valid samples: SLD updated from latest sample creation. */
    private void clean() {
        for (StyleInfo style : catalog.getStyles()) {
            synchronized (style) {
                Resource styleResource = getStyleResource(style);
                Resource sampleFile;
                try {
                    // remove old samples
                    sampleFile = getSampleFile(style);
                    if (isStyleNewerThanSample(styleResource, sampleFile)) {
                        sampleFile.delete();
                    }
                } catch (IOException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Error cleaning invalid legend sample for " + style.getName(),
                            e);
                }
            }
        }
        invalidated = new HashSet<String>();
    }

    /** Checks if the given SLD resource is newer than the given sample file. */
    private boolean isStyleNewerThanSample(Resource styleResource, Resource sampleFile) {
        return styleResource != null
                && styleResource.getType() == Resource.Type.RESOURCE
                && styleResource.lastmodified() > sampleFile.lastmodified();
    }

    /** Returns the cached sample for the given file, if it exists, null otherwise. */
    private Resource getSampleFile(StyleInfo style) throws IOException {
        String fileName = getSampleFileName(style);
        return getSampleFile(fileName);
    }

    /** Returns the cached sample with the given name. */
    private Resource getSampleFile(String fileName) {
        return getSamplesFolder().get(fileName);
    }

    /** Gets a unique fileName for a sample. */
    private String getSampleFileName(StyleInfo style) {
        String prefix = "";
        if (style.getWorkspace() != null) {
            prefix = style.getWorkspace().getName() + "_";
        }
        String fileName = prefix + style.getName() + "." + DEFAULT_SAMPLE_FORMAT;
        return fileName;
    }

    /** Gets an SLD resource for the given style. */
    private Resource getStyleResource(StyleInfo style) {
        String[] prefix = new String[0];
        if (style.getWorkspace() != null) {
            prefix = new String[] {"workspaces", style.getWorkspace().getName()};
        }
        String fileName = style.getFilename();
        String[] pathParts =
                (String[]) ArrayUtils.addAll(prefix, new String[] {"styles", fileName});
        String path = Paths.path(pathParts);
        return loader.get(path);
    }

    /**
     * Calculates legendURL size (width x height) for the given style.
     *
     * @return legend dimensions
     */
    public Dimension getLegendURLSize(StyleInfo style) throws Exception {
        synchronized (style) {
            GetLegendGraphicOutputFormat pngOutputFormat = new PNGLegendOutputFormat();

            Resource sampleLegend = getSampleFile(style);
            if (isSampleExisting(sampleLegend) && !isStyleSampleInvalid(style)) {
                // using existing sample if sld has not been updated from
                // latest sample update
                return getSizeFromSample(sampleLegend);
            } else {
                // generates a new sample, and save it on disk (in the dedicated folder) for
                // later usage
                return createNewSample(style, pngOutputFormat);
            }
        }
    }

    private boolean isSampleExisting(Resource sampleLegend) {
        return sampleLegend != null && sampleLegend.getType() == Type.RESOURCE;
    }

    /**
     * Creates a new sample file for the given style and stores it on disk. The sample dimensions
     * (width x height) are returned.
     */
    private Dimension createNewSample(StyleInfo style, GetLegendGraphicOutputFormat pngOutputFormat)
            throws Exception {
        GetLegendGraphicRequest legendGraphicRequest = new GetLegendGraphicRequest(WMS.get());
        Resource sampleLegendFolder = getSamplesFolder();

        legendGraphicRequest.setStrict(false);
        legendGraphicRequest.setLayer((FeatureType) null);
        legendGraphicRequest.setStyle(style.getStyle());
        legendGraphicRequest.setFormat(pngOutputFormat.getContentType());
        Object legendGraphic = pngOutputFormat.produceLegendGraphic(legendGraphicRequest);
        if (legendGraphic instanceof BufferedImageLegendGraphic) {
            BufferedImage image = (BufferedImage) ((LegendGraphic) legendGraphic).getLegend();

            PNGWriter writer = new PNGWriter();
            OutputStream outStream = null;
            try {
                Resource sampleFile = sampleLegendFolder.get(getSampleFileName(style));
                outStream = sampleFile.out();
                writer.writePNG(image, outStream, 0.0f, FilterType.FILTER_NONE);
                removeStyleSampleInvalidation(style);
                return new Dimension(image.getWidth(), image.getHeight());
            } finally {
                if (outStream != null) {
                    outStream.close();
                }
            }
        }

        return null;
    }

    private Resource getSamplesFolder() {
        return baseDir.get(LEGEND_SAMPLES_FOLDER);
    }

    /** @param sampleLegendFile */
    private Dimension getSizeFromSample(Resource sampleLegendFile) {
        PngReader pngReader = null;
        try {
            // reads size using PNGJ reader, that can read metadata without reading
            // the full image
            pngReader = new PngReader(sampleLegendFile.file());
            return new Dimension(pngReader.imgInfo.cols, pngReader.imgInfo.rows);
        } finally {
            if (pngReader != null) {
                pngReader.close();
            }
        }
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            // invalidate removed styles (is this needed?)
            invalidateStyleSample((StyleInfo) event.getSource());
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {}

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            // invalidate updated styles
            invalidateStyleSample((StyleInfo) event.getSource());
        }
    }

    /** Set the given style sample as invalid. */
    private void invalidateStyleSample(StyleInfo style) {
        synchronized (style) {
            invalidated.add(getStyleName(style));
        }
    }

    /** Remove the given style sample from invalid ones. */
    private void removeStyleSampleInvalidation(StyleInfo style) {
        invalidated.remove(getStyleName(style));
    }

    /** Checks if the given style sample is marked as invalid. */
    private boolean isStyleSampleInvalid(StyleInfo style) {
        return invalidated.contains(getStyleName(style));
    }

    /**
     * Gets a unique name for a style, considering the workspace definition, in the form
     * worspacename:stylename (or stylename if the style is global).
     */
    private String getStyleName(StyleInfo styleInfo) {
        return styleInfo.getWorkspace() != null
                ? (styleInfo.getWorkspace().getName() + ":" + styleInfo.getName())
                : styleInfo.getName();
    }

    @Override
    public void reloaded() {
        clean();
    }

    @Override
    public void onReset() {}

    @Override
    public void onDispose() {
        catalog.removeListener(this);
    }

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {
        reloaded();
    }
}
