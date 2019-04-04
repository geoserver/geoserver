/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.MaxErrorEnforcer;
import org.geoserver.wms.map.RenderExceptionStrategy;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.w3c.dom.Document;

/**
 * Renders svg using the Batik SVG Toolkit. An SVG context is created for a map and then passed of
 * to {@link org.geotools.renderer.lite.StreamingRenderer}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public final class SVGBatikMapOutputFormat implements GetMapOutputFormat {

    /**
     * Default capabilities for SVG format.
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

    private final WMS wms;

    public SVGBatikMapOutputFormat(WMS wms) {
        this.wms = wms;
    }

    /**
     * @return {@code ["image/svg+xml", "image/svg xml", "image/svg"]}
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return SVG.OUTPUT_FORMATS;
    }

    /**
     * @return {@code "image/svg+xml"}
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return SVG.MIME_TYPE;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    public BatikSVGMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {

        StreamingRenderer renderer = setUpRenderer(mapContent);
        SVGGraphics2D g = createSVGMap(renderer, mapContent);
        renderer = null;

        // This method of output does not output the DOCTYPE definiition
        // TODO: make a config option that toggles wether doctype is
        // written out.
        // OutputFormat format = new OutputFormat();
        // XMLSerializer serializer = new XMLSerializer(new OutputStreamWriter(out, "UTF-8"),
        // format);

        return new BatikSVGMap(mapContent, g);
    }

    private StreamingRenderer setUpRenderer(WMSMapContent mapContent) {
        StreamingRenderer renderer;
        renderer = new StreamingRenderer();

        // optimized data loading was not here, but yet it seems sensible to
        // have it...
        Map<String, Object> rendererParams = new HashMap<String, Object>();
        rendererParams.put("optimizedDataLoadingEnabled", Boolean.TRUE);
        // we need the renderer to draw everything on the batik provided graphics object
        rendererParams.put(StreamingRenderer.OPTIMIZE_FTS_RENDERING_KEY, Boolean.FALSE);
        // render everything in vector form if possible
        rendererParams.put(StreamingRenderer.VECTOR_RENDERING_KEY, Boolean.TRUE);
        rendererParams.put("renderingBuffer", Integer.valueOf(mapContent.getBuffer()));
        if (DefaultWebMapService.isLineWidthOptimizationEnabled()) {
            rendererParams.put(StreamingRenderer.LINE_WIDTH_OPTIMIZATION_KEY, true);
        }
        rendererParams.put(
                StreamingRenderer.SCALE_COMPUTATION_METHOD_KEY,
                mapContent.getRendererScaleMethod());
        renderer.setRendererHints(rendererParams);
        renderer.setMapContent(mapContent);
        return renderer;
    }

    public SVGGraphics2D createSVGMap(
            final StreamingRenderer renderer, final WMSMapContent mapContent)
            throws ServiceException, IOException {
        try {
            MapContent map = renderer.getMapContent();
            double width = -1;
            double height = -1;

            if (map instanceof WMSMapContent) {
                WMSMapContent wmsMap = (WMSMapContent) map;
                width = wmsMap.getMapWidth();
                height = wmsMap.getMapHeight();
            } else {
                // get fromt he map content
                Rectangle screenArea = map.getViewport().getScreenArea();
                width = screenArea.getWidth();
                height = screenArea.getHeight();
            }

            if ((height == -1) || (width == -1)) {
                throw new IOException("Could not determine map dimensions");
            }

            SVGGeneratorContext context = setupContext();
            SVGGraphics2D g = new SVGGraphics2D(context, true);

            g.setSVGCanvasSize(new Dimension((int) width, (int) height));

            // turn off/on anti aliasing
            if (wms.isSvgAntiAlias()) {
                g.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            } else {
                g.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            }

            // enforce no more than x rendering errors
            int maxErrors = wms.getMaxRenderingErrors();
            MaxErrorEnforcer errorChecker = new MaxErrorEnforcer(renderer, maxErrors);

            // Add a render listener that ignores well known rendering exceptions and reports back
            // non
            // ignorable ones
            final RenderExceptionStrategy nonIgnorableExceptionListener;
            nonIgnorableExceptionListener = new RenderExceptionStrategy(renderer);
            renderer.addRenderListener(nonIgnorableExceptionListener);

            renderer.paint(
                    g,
                    new Rectangle(g.getSVGCanvasSize()),
                    mapContent.getRenderingArea(),
                    mapContent.getRenderingTransform());

            // check if too many errors occurred
            if (errorChecker.exceedsMaxErrors()) {
                throw new ServiceException(
                        "More than " + maxErrors + " rendering errors occurred, bailing out.",
                        errorChecker.getLastException(),
                        "internalError");
            }

            // check if a non ignorable error occurred
            if (nonIgnorableExceptionListener.exceptionOccurred()) {
                Exception renderError = nonIgnorableExceptionListener.getException();
                throw new ServiceException(
                        "Rendering process failed", renderError, "internalError");
            }

            return g;
        } catch (ParserConfigurationException e) {
            throw new ServiceException("Unexpected exception", e, "internalError");
        }
    }

    private SVGGeneratorContext setupContext()
            throws FactoryConfigurationError, ParserConfigurationException {
        Document document = null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = dbf.newDocumentBuilder();

        // Create an instance of org.w3c.dom.Document
        String svgNamespaceURI = "http://www.w3.org/2000/svg";
        document = db.getDOMImplementation().createDocument(svgNamespaceURI, "svg", null);

        // Set up the context
        return SVGGeneratorContext.createDefault(document);
    }

    public MapProducerCapabilities getCapabilities(String format) {
        return CAPABILITIES;
    }
}
