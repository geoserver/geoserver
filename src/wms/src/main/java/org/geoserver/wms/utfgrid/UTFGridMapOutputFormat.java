/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.CachedGridReaderLayer;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.map.RasterLayer;
import org.geotools.map.StyleLayer;
import org.geotools.ows.wms.map.WMSLayer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;

/**
 * Handles a GetMap request that spects a map in UTFGrid format.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class UTFGridMapOutputFormat extends AbstractMapOutputFormat {

    static final Logger LOGGER = Logging.getLogger(UTFGridMapOutputFormat.class);

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "application/json;type=utfgrid";

    static final String OUTPUT_FORMAT_NAME = "utfgrid";

    /** Default scale-down factor for the output grid size */
    static final int DEFAULT_UTFRESOLUTION = 4;

    /**
     * Default capabilities for UTFGrid format.
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = unsupported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = unsupported
     *   <li>transparency = supported
     * </ol>
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(false, false, false, true, null);

    private WMS wms;

    public UTFGridMapOutputFormat(WMS wms) {
        super(MIME_TYPE, new String[] {OUTPUT_FORMAT_NAME});
        this.wms = wms;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }

    @Override
    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        RenderedImageMapOutputFormat of =
                new RenderedImageMapOutputFormat(wms) {
                    @Override
                    protected StreamingRenderer buildRenderer() {
                        // use a renderer that won't render raster or labels, not even by accident
                        return new PureVectorRenderer();
                    }

                    @Override
                    protected RenderedImage prepareImage(
                            int width, int height, IndexColorModel palette, boolean transparent) {
                        // each color is a feature index
                        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    }

                    @Override
                    protected void onBeforeRender(StreamingRenderer renderer) {
                        // disable antialiasing, numbers signify ids, we cannot have "half tints"
                        renderer.getJava2DHints()
                                .put(
                                        RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_OFF);

                        Map hints = renderer.getRendererHints();
                        double dpi = RendererUtilities.getDpi(hints);
                        dpi = dpi / DEFAULT_UTFRESOLUTION;
                        hints.put(StreamingRenderer.DPI_KEY, dpi);
                    }
                };

        UTFGridEntries entries = new UTFGridEntries();
        UTFGridMapContent utfGridMapContent = buildUTFGridMapContent(mapContent, entries);
        RenderedImageMap map = of.produceMap(utfGridMapContent);
        return new UTFGridMap(utfGridMapContent, map.getImage());
    }

    private UTFGridMapContent buildUTFGridMapContent(
            WMSMapContent original, UTFGridEntries entries) {
        UTFGridColorFunction colorFunction = new UTFGridColorFunction(entries);

        UTFGridMapContent result = new UTFGridMapContent(original, entries, DEFAULT_UTFRESOLUTION);
        List<Layer> utfLayers = new ArrayList<>();
        for (Layer layer : original.layers()) {
            // can only draw vector layers, or raster ones that will be transformed into
            // vectors by a rendering transformation
            if (!(layer instanceof StyleLayer)) {
                continue;
            }
            StyleLayer sl = (StyleLayer) layer;

            // deep clone the style and adapt it to use the color function
            UTFGridStyleVisitor styleVisitor = new UTFGridStyleVisitor(colorFunction);
            layer.getStyle().accept(styleVisitor);
            Style copy = (Style) styleVisitor.getCopy();

            // if the copy is empty or we don't have vector transformations, skip it
            if (copy.featureTypeStyles().isEmpty()
                    || (styleVisitor.hasTransformations()
                            && !styleVisitor.hasVectorTransformations())) {
                continue;
            }

            // skip also raster layers not transformed into vector ones
            if (layer instanceof RasterLayer && !styleVisitor.hasVectorTransformations()) {
                continue;
            }

            if (layer instanceof FeatureLayer) {
                // copy making sure we retain all attributes
                FeatureLayer fl =
                        new FeatureLayer(
                                new UTFGridFeatureSource(layer.getFeatureSource(), null), copy);
                fl.setQuery(layer.getQuery());
                sl = fl;
            } else if (layer instanceof GridCoverageLayer) {
                GridCoverageLayer gc = (GridCoverageLayer) sl;
                sl = new GridCoverageLayer(gc.getCoverage(), copy);
            } else if (layer instanceof WMSLayer) {
                // these are pure raster, we cannot do a UTFGrid out of them... although
                // we could try a cascading if the remote server also supports UTFGrid,
                // in the future
                continue;
            } else if (layer instanceof CachedGridReaderLayer) {
                CachedGridReaderLayer gr = (CachedGridReaderLayer) sl;
                sl = new CachedGridReaderLayer(gr.getReader(), copy);
            } else if (layer instanceof GridReaderLayer) {
                GridReaderLayer gr = (GridReaderLayer) sl;
                sl = new GridReaderLayer(gr.getReader(), copy);
            } else {
                LOGGER.log(
                        Level.WARNING,
                        "Skipping unknown layer " + sl + " of type " + sl.getClass());
                continue;
            }
            utfLayers.add(sl);
        }
        result.layers().addAll(utfLayers);

        return result;
    }
}
