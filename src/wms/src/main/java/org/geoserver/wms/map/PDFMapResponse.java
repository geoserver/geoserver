/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGraphics2D;
import com.lowagie.text.pdf.PdfPatternPainter;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geoserver.wms.map.PDFMapOutputFormat.PDFMap;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.LabelCache;
import org.geotools.renderer.lite.ParallelLinesFiller;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.renderer.style.IconStyle2D;
import org.geotools.renderer.style.MarkStyle2D;
import org.geotools.renderer.style.Style2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.util.Assert;

/**
 * Handles a GetMap request that spects a map in PDF format.
 *
 * @author Pierre-Emmanuel Balageas, ALCER (http://www.alcer.com)
 * @author Simone Giannecchini - GeoSolutions
 * @author Gabriel Roldan
 * @version $Id$
 */
public class PDFMapResponse extends AbstractMapResponse {
    /** A logger for this class. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map.pdf");

    /**
     * Whether to apply the new vector hatch fill optimization, or not (on by default, this is just
     * a safeguard)
     */
    static boolean ENCODE_TILING_PATTERNS =
            Boolean.parseBoolean(
                    System.getProperty("org.geoserver.pdf.encodeTilingPatterns", "true"));

    /** A kilobyte */
    private static final int KB = 1024;

    private WMS wms;

    public PDFMapResponse(WMS wms) {
        super(PDFMap.class, PDFMapOutputFormat.MIME_TYPE);
        this.wms = wms;
    }

    /**
     * Writes the PDF.
     *
     * <p>NOTE: the document seems to actually be created in memory, and being written down to
     * {@code output} once we call {@link Document#close()}. If there's no other way to do so, it'd
     * be better to actually split out the process into produceMap/write?
     *
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        Assert.isInstanceOf(PDFMap.class, value);
        WMSMapContent mapContent = ((PDFMap) value).getContext();

        final int width = mapContent.getMapWidth();
        final int height = mapContent.getMapHeight();

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("setting up " + width + "x" + height + " image");
        }

        try {
            // step 1: creation of a document-object
            // width of document-object is width*72 inches
            // height of document-object is height*72 inches
            com.lowagie.text.Rectangle pageSize = new com.lowagie.text.Rectangle(width, height);

            Document document = new Document(pageSize);
            document.setMargins(0, 0, 0, 0);

            // step 2: creation of the writer
            PdfWriter writer = PdfWriter.getInstance(document, output);

            // step 3: we open the document
            document.open();

            // step 4: we grab the ContentByte and do some stuff with it

            // we create a fontMapper and read all the fonts in the font
            // directory
            DefaultFontMapper mapper = new DefaultFontMapper();
            FontFactory.registerDirectories();

            // we create a template and a Graphics2D object that corresponds
            // with it
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height);
            PdfGraphics2D graphic = (PdfGraphics2D) tp.createGraphics(width, height, mapper);

            // we set graphics options
            if (!mapContent.isTransparent()) {
                graphic.setColor(mapContent.getBgColor());
                graphic.fillRect(0, 0, width, height);
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("setting to transparent");
                }

                int type = AlphaComposite.SRC;
                graphic.setComposite(AlphaComposite.getInstance(type));

                Color c =
                        new Color(
                                mapContent.getBgColor().getRed(),
                                mapContent.getBgColor().getGreen(),
                                mapContent.getBgColor().getBlue(),
                                0);
                graphic.setBackground(mapContent.getBgColor());
                graphic.setColor(c);
                graphic.fillRect(0, 0, width, height);

                type = AlphaComposite.SRC_OVER;
                graphic.setComposite(AlphaComposite.getInstance(type));
            }

            Rectangle paintArea = new Rectangle(width, height);

            StreamingRenderer renderer;
            if (ENCODE_TILING_PATTERNS) {
                renderer = new PDFStreamingRenderer();
            } else {
                renderer = new StreamingRenderer();
            }
            renderer.setMapContent(mapContent);
            // TODO: expose the generalization distance as a param
            // ((StreamingRenderer) renderer).setGeneralizationDistance(0);

            RenderingHints hints =
                    new RenderingHints(
                            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            renderer.setJava2DHints(hints);

            // we already do everything that the optimized data loading does...
            // if we set it to true then it does it all twice...
            java.util.Map rendererParams = new HashMap();
            rendererParams.put("optimizedDataLoadingEnabled", Boolean.TRUE);
            rendererParams.put("renderingBuffer", Integer.valueOf(mapContent.getBuffer()));
            // we need the renderer to draw everything on the batik provided graphics object
            rendererParams.put(StreamingRenderer.OPTIMIZE_FTS_RENDERING_KEY, Boolean.FALSE);
            // render everything in vector form if possible
            rendererParams.put(StreamingRenderer.VECTOR_RENDERING_KEY, Boolean.TRUE);
            if (DefaultWebMapService.isLineWidthOptimizationEnabled()) {
                rendererParams.put(StreamingRenderer.LINE_WIDTH_OPTIMIZATION_KEY, true);
            }
            rendererParams.put(
                    StreamingRenderer.SCALE_COMPUTATION_METHOD_KEY,
                    mapContent.getRendererScaleMethod());

            renderer.setRendererHints(rendererParams);

            // enforce no more than x rendering errors
            int maxErrors = wms.getMaxRenderingErrors();
            MaxErrorEnforcer errorChecker = new MaxErrorEnforcer(renderer, maxErrors);

            // Add a render listener that ignores well known rendering exceptions and reports back
            // non
            // ignorable ones
            final RenderExceptionStrategy nonIgnorableExceptionListener;
            nonIgnorableExceptionListener = new RenderExceptionStrategy(renderer);
            renderer.addRenderListener(nonIgnorableExceptionListener);

            // enforce max memory usage
            int maxMemory = wms.getMaxRequestMemory() * KB;
            PDFMaxSizeEnforcer memoryChecker = new PDFMaxSizeEnforcer(renderer, graphic, maxMemory);

            // render the map
            renderer.paint(
                    graphic,
                    paintArea,
                    mapContent.getRenderingArea(),
                    mapContent.getRenderingTransform());

            // render the watermark
            MapDecorationLayout.Block watermark =
                    RenderedImageMapOutputFormat.getWatermark(wms.getServiceInfo());

            if (watermark != null) {
                MapDecorationLayout layout = new MapDecorationLayout();
                layout.paint(graphic, paintArea, mapContent);
            }

            // check if a non ignorable error occurred
            if (nonIgnorableExceptionListener.exceptionOccurred()) {
                Exception renderError = nonIgnorableExceptionListener.getException();
                throw new ServiceException(
                        "Rendering process failed", renderError, "internalError");
            }

            // check if too many errors occurred
            if (errorChecker.exceedsMaxErrors()) {
                throw new ServiceException(
                        "More than " + maxErrors + " rendering errors occurred, bailing out",
                        errorChecker.getLastException(),
                        "internalError");
            }

            // check we did not use too much memory
            if (memoryChecker.exceedsMaxSize()) {
                long kbMax = maxMemory / KB;
                throw new ServiceException(
                        "Rendering request used more memory than the maximum allowed:"
                                + kbMax
                                + "KB");
            }

            graphic.dispose();
            cb.addTemplate(tp, 0, 0);

            // step 5: we close the document
            document.close();
            writer.flush();
            writer.close();
        } catch (DocumentException t) {
            throw new ServiceException("Error setting up the PDF", t, "internalError");
        }
    }

    private static class PDFStreamingRenderer extends StreamingRenderer {

        public PDFStreamingRenderer() {
            this.painter = new PDFStyledPainter(labelCache);
        }
    }

    /**
     * Optimized StyledShapePainter that can optimize painting repeated external graphics by
     * encoding them in a native PDF pattern painter, with no repetition
     */
    private static class PDFStyledPainter extends StyledShapePainter {

        /**
         * Re-creating the patterns is expensive and would increase the overall document size, keep
         * a cache of them instead
         */
        WeakHashMap<Style2D, PdfPatternPainter> patternCache = new WeakHashMap<>();

        public PDFStyledPainter(LabelCache labelCache) {
            super(labelCache);
        }

        @Override
        protected void paintGraphicFill(
                Graphics2D graphics, Shape shape, Style2D graphicFill, double scale) {

            if (graphics instanceof PdfGraphics2D
                    && (graphicFill instanceof IconStyle2D || isMarkNonHatchFill(graphicFill))) {
                fillShapeAsPattern((PdfGraphics2D) graphics, shape, graphicFill, scale);
            } else {
                super.paintGraphicFill(graphics, shape, graphicFill, scale);
            }
        }

        private boolean isMarkNonHatchFill(Style2D graphicFill) {
            if (!(graphicFill instanceof MarkStyle2D)) {
                return false;
            }

            if (OPTIMIZE_VECTOR_HATCH_FILLS) {
                MarkStyle2D ms = (MarkStyle2D) graphicFill;
                ParallelLinesFiller filler = ParallelLinesFiller.fromStipple(ms.getShape());
                if (filler != null) {
                    return false;
                }
            }

            return true;
        }

        private void fillShapeAsPattern(
                PdfGraphics2D graphics, Shape shape, Style2D graphicFill, double scale) {
            final PdfContentByte content = graphics.getContent();
            Paint oldPaint = graphics.getPaint();
            try {
                // prepare the PDF pattern
                PdfPatternPainter pattern = getPatternPainter(graphicFill, content, scale);
                final PdfContentByte cb = graphics.getContent();
                content.setPatternFill(pattern);

                // Paint the pattern. Unfortunately we cannot rely on PDFGraphic but have to
                // use the low level API to walk the shape
                AffineTransform tx = new AffineTransform(graphics.getTransform());
                float documentHeight = cb.getPdfDocument().getPageSize().getHeight();
                tx.concatenate(new AffineTransform(1, 0, 0, -1, 0, documentHeight));
                PathIterator pathIterator = shape.getPathIterator(tx);
                float[] coords = new float[6];
                if (!pathIterator.isDone()) {
                    // walk the path
                    while (!pathIterator.isDone()) {
                        int segtype = pathIterator.currentSegment(coords);
                        switch (segtype) {
                            case PathIterator.SEG_MOVETO:
                                cb.moveTo(coords[0], coords[1]);
                                break;

                            case PathIterator.SEG_LINETO:
                                cb.lineTo(coords[0], coords[1]);
                                break;

                            case PathIterator.SEG_QUADTO:
                                cb.curveTo(coords[0], coords[1], coords[2], coords[3]);
                                break;

                            case PathIterator.SEG_CUBICTO:
                                cb.curveTo(
                                        coords[0], coords[1], coords[2], coords[3], coords[4],
                                        coords[5]);
                                break;

                            case PathIterator.SEG_CLOSE:
                                cb.closePath();
                                break;
                        }
                        pathIterator.next();
                    }
                    // run the actual fill
                    if (pathIterator.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
                        cb.eoFill();
                    } else {
                        cb.fill();
                    }
                }
            } finally {
                // reset the old paint
                graphics.setPaint(oldPaint);
            }
        }

        private PdfPatternPainter getPatternPainter(
                Style2D graphicFill, final PdfContentByte content, double scale) {
            PdfPatternPainter pattern = patternCache.get(graphicFill);
            if (pattern == null) {

                pattern = buildPattern(graphicFill, content, scale);
                patternCache.put(graphicFill, pattern);
            }
            return pattern;
        }

        private PdfPatternPainter buildPattern(
                Style2D graphicFill, final PdfContentByte content, double scale) {
            PdfPatternPainter pattern;
            if (graphicFill instanceof IconStyle2D) {
                final Icon icon = ((IconStyle2D) graphicFill).getIcon();
                int width = icon.getIconWidth();
                int height = icon.getIconHeight();
                pattern = content.createPattern(width, height);
                Graphics2D patternGraphic = pattern.createGraphics(width, height);
                icon.paintIcon(null, patternGraphic, 0, 0);
                patternGraphic.dispose();
            } else if (graphicFill instanceof MarkStyle2D) {
                MarkStyle2D mark = (MarkStyle2D) graphicFill;
                int size = (int) Math.round(mark.getSize());
                pattern = content.createPattern(size, size);
                Graphics2D patternGraphic = pattern.createGraphics(size, size);
                GeometryFactory geomFactory = new GeometryFactory();
                Coordinate stippleCoord = new Coordinate(size / 2d, size / 2d);
                Geometry stipplePoint = geomFactory.createPoint(stippleCoord);
                Decimator nullDecimator = new Decimator(-1, -1);
                AffineTransform2D identityTransf = new AffineTransform2D(new AffineTransform());
                try {
                    paint(
                            patternGraphic,
                            new LiteShape2(stipplePoint, identityTransf, nullDecimator, false),
                            mark,
                            scale);
                } catch (TransformException | FactoryException e) {
                    // this should not happen given the arguments passed to the lite shape
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("Unsupported style " + graphicFill);
            }
            return pattern;
        }
    }
}
