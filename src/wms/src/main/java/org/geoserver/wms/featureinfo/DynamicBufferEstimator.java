/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Stroke;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;
import org.opengis.style.GraphicalSymbol;

/**
 * Evaluates a meta-buffer against the specified feature. Can be called with multiple subsequent
 * features and will accumulate the largest available buffer.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DynamicBufferEstimator extends AbstractStyleVisitor {

    static final Logger LOGGER = Logging.getLogger(VectorRenderingLayerIdentifier.class);

    Feature feature;

    double buffer;

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public int getBuffer() {
        return (int) Math.ceil(buffer);
    }

    @Override
    public void visit(Stroke stroke) {
        Expression width = stroke.getWidth();
        if (width != null) {
            Double w = width.evaluate(feature, Double.class);
            if (w != null && w > buffer) {
                buffer = w;
            }
        }
    }

    @Override
    public void visit(Graphic gr) {
        try {
            Expression grSize = gr.getSize();
            if (grSize != null) {
                Double size = grSize.evaluate(feature, Double.class);
                if (size != null) {
                    buffer = Math.max(buffer, size);
                }
                return;
            }

            // no fixed size, proceed
            for (GraphicalSymbol gs : gr.graphicalSymbols()) {
                if (gs instanceof ExternalGraphic) {
                    ExternalGraphic eg = (ExternalGraphic) gs;
                    Icon icon = null;
                    if (eg.getInlineContent() != null) {
                        icon = eg.getInlineContent();
                    } else {
                        String location = eg.getLocation().toExternalForm();
                        // expand embedded cql expression
                        Expression expanded = ExpressionExtractor.extractCqlExpressions(location);

                        Iterator<ExternalGraphicFactory> it =
                                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
                        while (it.hasNext()) {
                            try {
                                icon = it.next().getIcon(feature, expanded, eg.getFormat(), -1);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.FINE,
                                        "Error occurred evaluating external graphic",
                                        e);
                            }
                        }
                    }
                    // evaluate the icon if found, if not SLD asks us to go to the next one
                    if (icon != null) {
                        int size = Math.max(icon.getIconHeight(), icon.getIconWidth());
                        if (size > buffer) {
                            buffer = size;
                        }
                        return;
                    }
                } else if (gs instanceof Mark) {
                    // if we get here it means size was null
                    if (SLDStyleFactory.DEFAULT_MARK_SIZE > buffer) {
                        buffer = SLDStyleFactory.DEFAULT_MARK_SIZE;
                    }
                }
            }
        } catch (ClassCastException e) {
            LOGGER.info("Could not parse graphic size, " + "it's a literal but not a Number...");
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Error occured during the graphic size estimation, "
                            + "meta buffer estimate cannot be performed",
                    e);
        }
    }
}
