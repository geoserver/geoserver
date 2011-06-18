/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.legendgraphic.LegendUtils;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.map.MapLayer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.NumberRange;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class LegendDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER = 
        org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.responses");

    private static final int TITLE_INDENT = 4;

    private static SLDStyleFactory styleFactory = new SLDStyleFactory();

    private Color bgcolor = Color.WHITE;
    private Color fgcolor = Color.BLACK;

    private static final GeometryFactory geomFac = new GeometryFactory();

    /**
     * Just a holder to avoid creating many polygon shapes from inside
     * <code>getSampleShape()</code>
     */
    private LiteShape2 sampleRect;

    /**
     * Just a holder to avoid creating many line shapes from inside
     * <code>getSampleShape()</code>
     */
    private LiteShape2 sampleLine;

    /**
     * Just a holder to avoid creating many point shapes from inside
     * <code>getSampleShape()</code>
     */
    private LiteShape2 samplePoint;

    private final WMS wms;

    private static final StyledShapePainter shapePainter = new StyledShapePainter();

    public LegendDecoration(WMS wms){
        this.wms = wms;
    }
    
    public void loadOptions(Map<String, String> options){
        Color tmp = parseColor(options.get("bgcolor"));
        if (tmp != null) this.bgcolor = tmp;

        tmp = parseColor(options.get("fgcolor"));
        if (tmp != null) this.fgcolor = tmp;
    }

    Catalog findCatalog(WMSMapContext mapContext){
        return wms.getGeoServer().getCatalog();
    }
    
    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContext mapContext){
        int x = 0, y = 0;
        Catalog catalog = findCatalog(mapContext);
        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont().deriveFont(Font.BOLD));
        double scaleDenominator = RendererUtilities.calculateOGCScale(
                mapContext.getAreaOfInterest(),
                mapContext.getRequest().getWidth(),
                null
                );

        for (MapLayer layer : mapContext.getLayers()){
            SimpleFeatureType type = (SimpleFeatureType)layer.getFeatureSource().getSchema();
            if (!isGridLayer(type)) {
                try {
                    Dimension legend = getLegendSize(
                            type,
                            layer.getStyle(),
                            scaleDenominator,
                            g2d
                            );
                    x = Math.max(x, (int)legend.width);
                    x = Math.max(x, TITLE_INDENT + metrics.stringWidth(findTitle(layer, catalog)));
                    y += legend.height + metrics.getHeight(); 
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error sizing legend for " + layer);
                    continue;
                }
            } else {
                LOGGER.log(Level.FINE, "Skipping raster layer: " + layer);
            }
        }
        x += metrics.getDescent();
        return new Dimension(x, y);
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContext mapContext) 
    throws Exception {
    	Catalog catalog = wms.getGeoServer().getCatalog();
        Dimension d = findOptimalSize(g2d, mapContext);
        Rectangle bgRect = new Rectangle(0, 0, d.width, d.height);
        double scaleDenominator = RendererUtilities.calculateOGCScale(
            mapContext.getAreaOfInterest(),
            mapContext.getRequest().getWidth(),
            new HashMap()
        );


        Color oldColor = g2d.getColor();
        AffineTransform oldTransform = (AffineTransform)g2d.getTransform().clone();
        Font oldFont = g2d.getFont();
        Stroke oldStroke = g2d.getStroke();
        g2d.translate(paintArea.getX(), paintArea.getY());

        AffineTransform tx = new AffineTransform(); 

        FontMetrics metrics = g2d.getFontMetrics(g2d.getFont().deriveFont(Font.BOLD));

        double scaleFactor = (paintArea.getWidth() / d.getWidth());
        scaleFactor = Math.min(scaleFactor, paintArea.getHeight() / d.getHeight());

        if (scaleFactor < 1.0) {
            g2d.scale(scaleFactor, scaleFactor);
        }
        AffineTransform bgTransform = g2d.getTransform();
        g2d.setColor(bgcolor);
        g2d.fill(bgRect);
        g2d.setColor(fgcolor);

        for (MapLayer layer : mapContext.getLayers()){
            SimpleFeatureType type = (SimpleFeatureType)layer.getFeatureSource().getSchema();
            if (!isGridLayer(type)) {
                try { 
                    g2d.translate(0, metrics.getHeight());
                    g2d.setFont(g2d.getFont().deriveFont(Font.BOLD));
                    g2d.drawString(findTitle(layer, catalog), TITLE_INDENT, 0 - metrics.getDescent());
                    g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN));
                    Dimension dim = drawLegend(
                            type,
                            layer.getStyle(),
                            scaleDenominator,
                            g2d
                            );
                    g2d.translate(0, dim.getHeight()); 
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Couldn't make a legend for " + type.getName(), e);
                }
            } else {
                LOGGER.log(Level.FINE, "Skipping raster layer " + type.getName() + " in legend decoration");
            }
        }

        g2d.setTransform(bgTransform);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new Rectangle(bgRect.x, bgRect.y, bgRect.width -1, bgRect.height - 1));

        g2d.setStroke(oldStroke);
        g2d.setTransform(oldTransform);
        g2d.setFont(oldFont);
        g2d.setColor(oldColor);
    }

    private String findTitle(MapLayer layer, Catalog catalog) {
        String[] nameparts = layer.getTitle().split(":");

        ResourceInfo resource = nameparts.length > 1 
            ? catalog.getResourceByName(nameparts[0], nameparts[1], ResourceInfo.class) 
            : catalog.getResourceByName(nameparts[0], ResourceInfo.class);

        return resource != null
            ? resource.getTitle()
            : layer.getTitle();
    }

    public Dimension getLegendSize(
        final SimpleFeatureType layer,
        final Style style,
        final double scaleDenominator,
        Graphics2D g2d
    ) throws ServiceException {

		final SimpleFeature sampleFeature = createSampleFeature(layer);
        final FeatureTypeStyle[] ftStyles = style.getFeatureTypeStyles();
        final Rule[] applicableRules = LegendUtils.getApplicableRules(ftStyles, scaleDenominator);
        final NumberRange<Double> scaleRange = 
            NumberRange.create(scaleDenominator, scaleDenominator);
        final int ruleCount = applicableRules.length;

        final int w = 20;
        final int h = 20;

        FontMetrics metrics = g2d.getFontMetrics();

        float totalHeight = 0, totalWidth = 0;

        for (int i = 0; i < ruleCount; i++) {
            final Symbolizer[] symbolizers = applicableRules[i].getSymbolizers();

            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];

                if (symbolizer instanceof RasterSymbolizer) {
                    throw new IllegalStateException(
                        "It is not legal to have a RasterSymbolizer here"
                    );
                } 
            }

            String label = applicableRules[i].getTitle();
            if (label == null) label = applicableRules[i].getName();
            if (label == null) label = "";

            float heightIncrement = Math.max(h, metrics.getHeight());

            totalHeight = totalHeight + heightIncrement;
            totalWidth = 
                Math.max(totalWidth, w + metrics.getDescent() + metrics.stringWidth(label));
        }

        return new Dimension((int)totalWidth, (int)totalHeight);
    }

    public Dimension drawLegend(
            final SimpleFeatureType layer,
            final Style style,
            final double scaleDenominator,
            Graphics2D g2d) throws ServiceException {

		final SimpleFeature sampleFeature = createSampleFeature(layer);
        final FeatureTypeStyle[] ftStyles = style.getFeatureTypeStyles();
        final Rule[] applicableRules = LegendUtils.getApplicableRules(ftStyles, scaleDenominator);
        final NumberRange<Double> scaleRange = 
            NumberRange.create(scaleDenominator, scaleDenominator);
        final int ruleCount = applicableRules.length;

        final int w = 20;
        final int h = 20;

        FontMetrics metrics = g2d.getFontMetrics();
        AffineTransform oldTransform = g2d.getTransform();
        Composite oldComposite = g2d.getComposite();

        float totalHeight = 0, totalWidth = 0;

        for (int i = 0; i < ruleCount; i++) {
            final Symbolizer[] symbolizers = applicableRules[i].getSymbolizers();

            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];

                if (symbolizer instanceof RasterSymbolizer) {
                    throw new IllegalStateException(
                            "It is not legal to have a RasterSymbolizer here"
                            );
                } else {
                    Style2D style2d = 
                        styleFactory.createStyle(sampleFeature, symbolizer, scaleRange);
                    LiteShape2 shape = getSampleShape(symbolizer, w, h);

                    if (style2d != null) {
                        shapePainter.paint(g2d, shape, style2d, scaleDenominator);
                    }
                }
            }

            String label = applicableRules[i].getTitle();
            if (label == null) label = applicableRules[i].getName();
            if (label == null) label = "";

            g2d.setColor(Color.BLACK);
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.drawString(
                    label, 
                    h + metrics.getDescent(), 
                    metrics.getHeight()
                    );

            float heightIncrement = Math.max(h, metrics.getHeight());
            g2d.translate(0, heightIncrement);

            totalHeight = totalHeight + heightIncrement;
            totalWidth = 
                Math.max(totalWidth, w + metrics.getDescent() + metrics.stringWidth(label));

        }

        g2d.setTransform(oldTransform);
        g2d.setComposite(oldComposite);

        return new Dimension((int)totalWidth, (int)totalHeight);
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the
     * rendering of the legend graphic.
     *
     * @param schema the schema for which to create a sample Feature instance
     *
     * @throws ServiceException
     */
    private static SimpleFeature createSampleFeature(SimpleFeatureType schema)
        throws ServiceException {
        SimpleFeature sampleFeature;

        try {
            sampleFeature = SimpleFeatureBuilder.template(schema, null); 
        } catch (IllegalAttributeException e) {
            throw new ServiceException(e);
        }

        return sampleFeature;
    }

    /**
     * Returns a <code>java.awt.Shape</code> appropiate to render a legend
     * graphic given the symbolizer type and the legend dimensions.
     *
     * @param symbolizer the Symbolizer for whose type a sample shape will be
     *        created
     * @param legendWidth the requested width, in output units, of the legend
     *        graphic
     * @param legendHeight the requested height, in output units, of the legend
     *        graphic
     *
     * @return an appropiate Line2D, Rectangle2D or LiteShape(Point) for the
     *         symbolizer, wether it is a LineSymbolizer, a PolygonSymbolizer,
     *         or a Point ot Text Symbolizer
     *
     * @throws IllegalArgumentException if an unknown symbolizer impl was
     *         passed in.
     */
    private LiteShape2 getSampleShape(Symbolizer symbolizer, int legendWidth, int legendHeight) {
        LiteShape2 sampleShape;
        final float hpad = (legendWidth * LegendUtils.hpaddingFactor);
        final float vpad = (legendHeight * LegendUtils.vpaddingFactor);

        if (symbolizer instanceof LineSymbolizer) {
            if (this.sampleLine == null) {
                Coordinate[] coords = {
                        new Coordinate(hpad, legendHeight - vpad),
                        new Coordinate(legendWidth - hpad, vpad)
                    };
                LineString geom = geomFac.createLineString(coords);

                try {
                    this.sampleLine = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleLine = null;
                }
            }

            sampleShape = this.sampleLine;
        } else if ((symbolizer instanceof PolygonSymbolizer)
                || (symbolizer instanceof RasterSymbolizer)) {
            if (this.sampleRect == null) {
                final float w = legendWidth - (2 * hpad);
                final float h = legendHeight - (2 * vpad);

                Coordinate[] coords = {
                        new Coordinate(hpad, vpad), new Coordinate(hpad, vpad + h),
                        new Coordinate(hpad + w, vpad + h), new Coordinate(hpad + w, vpad),
                        new Coordinate(hpad, vpad)
                    };
                LinearRing shell = geomFac.createLinearRing(coords);
                Polygon geom = geomFac.createPolygon(shell, null);

                try {
                    this.sampleRect = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleRect = null;
                }
            }

            sampleShape = this.sampleRect;
        } else if (symbolizer instanceof PointSymbolizer || symbolizer instanceof TextSymbolizer) {
            if (this.samplePoint == null) {
                Coordinate coord = new Coordinate(legendWidth / 2, legendHeight / 2);

                try {
                    this.samplePoint = new LiteShape2(geomFac.createPoint(coord), null, null, false);
                } catch (Exception e) {
                    this.samplePoint = null;
                }
            }

            sampleShape = this.samplePoint;
        } else {
            throw new IllegalArgumentException("Unknown symbolizer: " + symbolizer);
        }

        return sampleShape;
    }

    public static boolean isGridLayer(final SimpleFeatureType layer) {
		for(PropertyDescriptor descriptor : layer.getDescriptors()){
			final PropertyType type = descriptor.getType();
			if (type.getBinding().isAssignableFrom(AbstractGridCoverage2DReader.class)) {
				return true;
			}
		}

        return false;
    }
    
    public static Color parseColor(String origInput) {
        if (origInput == null) return null;

        String input = origInput.trim();
        input = input.replaceFirst("\\A#", "");

        int r, g, b, a;

        switch (input.length()){
            case 1:
            case 2:
                return new Color(Integer.valueOf(input, 16));
            case 3:
                r = Integer.valueOf(input.substring(0, 1), 16);
                g = Integer.valueOf(input.substring(1, 2), 16);
                b = Integer.valueOf(input.substring(2, 3), 16);
                return new Color(r, g, b);
            case 4:
                r = Integer.valueOf(input.substring(0, 1), 16);
                g = Integer.valueOf(input.substring(1, 2), 16);
                b = Integer.valueOf(input.substring(2, 3), 16);
                a = Integer.valueOf(input.substring(3, 4), 16);
                return new Color(r, g, b, a);
            case 6:
                r = Integer.valueOf(input.substring(0, 2), 16);
                g = Integer.valueOf(input.substring(2, 4), 16);
                b = Integer.valueOf(input.substring(4, 6), 16);
                return new Color(r, g, b);
            case 8:
                r = Integer.valueOf(input.substring(0, 2), 16);
                g = Integer.valueOf(input.substring(2, 4), 16);
                b = Integer.valueOf(input.substring(4, 6), 16);
                a = Integer.valueOf(input.substring(6, 8), 16);
                return new Color(r, g, b, a);
            default: 
                throw new RuntimeException("Couldn't decode color value: " + origInput 
                    + " (" + input +")"
                );
        }
    }
}
