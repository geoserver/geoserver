/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.swing.Icon;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.styling.ExternalGraphic;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.Mark;
import org.opengis.style.Stroke;

/**
 * Utility methods for working with icons.
 *
 * @author Kevin Smith, OpenGeo
 */
public class Icons {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.icons");

    /**
     * Render symbols this much bigger than they should be then shrink them down in the KML. This
     * makes for nicer looking symbols when they undergo further transformations in Google Earth
     */
    public static final int RENDER_SCALE_FACTOR = 4;
    /** The default size to use for symbols if none is specified */
    public static final double DEFAULT_SYMBOL_SIZE = 16d;

    private Icons() {}

    /**
     * Find the size of a square needed to accommodate the rotated image of another square
     *
     * @param size size of the square to rotate. {@code null} returns {@code null}.
     * @param rotation the angle to rotate in degrees. {@code null} is treated as no rotation.
     * @return the size of a square big enough to contain the rotated image
     */
    public static @Nullable Integer rotationScale(
            @Nullable Integer size, @Nullable Double rotation) {
        if (size == null) return null;
        if (rotation == null || rotation % 90 == 0) return size; // Save us some trig functions
        return (int) Math.ceil(rotationScaleFactor(rotation) * size);
    }

    /**
     * Find the size of a square needed to accommodate the rotated image of another square
     *
     * @param size size of the square to rotate. {@code null} returns {@code null}.
     * @param rotation the angle to rotate in degrees. {@code null} is treated as no rotation.
     * @return the size of a square big enough to contain the rotated image
     */
    public static @Nullable Double rotationScale(@Nullable Double size, @Nullable Double rotation) {
        if (size == null) return null;
        if (rotation == null || rotation % 90 == 0) return size; // Save us some trig functions
        return rotationScaleFactor(rotation) * size;
    }
    /**
     * Find the scale factor needed for a square to accommodate a rotated square.
     *
     * @param rotation the angle in degrees
     */
    public static double rotationScaleFactor(double rotation) {
        return Math.abs(Math.sin(Math.toRadians(rotation)))
                + Math.abs(Math.cos(Math.toRadians(rotation)));
    }

    /** Get the rotation of the given graphic when applied to the given feature */
    public static @Nullable Double getRotation(Graphic g, @Nullable Feature f) {
        if (g.getRotation() != null) {
            return g.getRotation().evaluate(f, Double.class);
        } else {
            return null;
        }
    }

    /** Get the size of the given graphic when applied to the given feature */
    public static @Nullable Double getSpecifiedSize(Graphic g, @Nullable Feature f) {
        if (g.getSize() != null) {
            return g.getSize().evaluate(f, Double.class);
        } else {
            return null;
        }
    }

    private static @Nullable Icon getIcon(ExternalGraphic eg, @Nullable Feature f) {
        // Get the Icon for an external image symbol
        Icon i = eg.getInlineContent();
        if (i == null) {
            Expression location;
            try {
                location = ExpressionExtractor.extractCqlExpressions(eg.getLocation().toString());
                Iterator<ExternalGraphicFactory> it =
                        DynamicSymbolFactoryFinder.getExternalGraphicFactories();
                while (i == null && it.hasNext()) {
                    try {
                        ExternalGraphicFactory fact = it.next();
                        i = fact.getIcon((Feature) null, location, eg.getFormat(), -1);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Error occurred evaluating external graphic", e);
                    }
                }
            } catch (MalformedURLException e1) {
                LOGGER.log(Level.FINER, e1.getMessage(), e1);
            }
        }
        return i;
    }

    /** Get the largest dimension of an external graphic */
    public static @Nullable Integer getExternalSize(ExternalGraphic eg, @Nullable Feature f) {
        Icon i = getIcon(eg, f);
        if (i == null) {
            return (int) Icons.DEFAULT_SYMBOL_SIZE;
        } else {
            return Math.max(i.getIconHeight(), i.getIconWidth());
        }
    }

    /**
     * Get the size of a symbolizer graphic
     *
     * @param rotation Treat the graphic as a square and rotate it, then find a square big enough to
     *     accomodate the rotated square. If {@code null} the rotation will be calculated based on
     *     the feature.
     */
    public static @Nullable Double graphicSize(
            Graphic g, @Nullable Double rotation, @Nullable Feature f) {
        Double size = getSpecifiedSize(g, f);

        double border = 0;

        if (rotation == null) {
            rotation = getRotation(g, f);
        }

        GraphicalSymbol gs = g.graphicalSymbols().iterator().next();

        if (gs instanceof Mark) {
            Stroke stroke = ((Mark) gs).getStroke();
            if (stroke != null && stroke.getWidth() != null) {
                Double width = stroke.getWidth().evaluate(f, Double.class);
                if (width != null) {
                    border = width;
                }
            }
        }

        if (size == null) {
            if (gs instanceof ExternalGraphic) {
                size = (double) getExternalSize((ExternalGraphic) gs, f);
            } else {
                size = (double) DEFAULT_SYMBOL_SIZE;
            }
        }

        size = rotationScale(size, rotation);

        if (size != null) size += border;

        return size;
    }

    /**
     * Get the scale factor to put in the KML
     *
     * @param f rotation Treat the graphic as a square and rotate it, then find a square big enough
     *     to accomodate the rotated square. If {@code null} the rotation will be calculated based
     *     on the feature.
     */
    public static @Nullable Double graphicScale(Graphic g, @Nullable Feature f) {
        Double size = graphicSize(g, null, f);
        if (size != null) {
            return size / DEFAULT_SYMBOL_SIZE;
        }
        return null;
    }
}
