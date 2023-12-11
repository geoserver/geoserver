/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.grid.ortholine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridFeatureBuilder;

/**
 * A builder to generate a grid of horizontal and/or vertical ortho-lines.
 *
 * @author mbedward
 * @author ian turton
 * @since 31.0
 * @version $Id$
 */
public class CentredOrthoLineBuilder {
    private static final double TOL = 1.0e-8;

    private final ReferencedEnvelope gridBounds;
    private boolean hasVerticals;
    private boolean hasHorizontals;
    private boolean densify;

    private SimpleFeatureBuilder featureBuilder;

    /**
     * Creates a new builder for the specified envelope.
     *
     * @param gridBounds bounds of the area for which lines will be generated
     */
    public CentredOrthoLineBuilder(ReferencedEnvelope gridBounds) {
        this.gridBounds = gridBounds;
    }

    /**
     * Creates line features according to the provided {@code OrthoLineDef} objects and places them
     * into the provided {@link ListFeatureCollection}. Densified lines (lines strings with
     * additional vertices along their length) can be created by setting the value of {@code
     * vertexSpacing} greater than zero; if so, any lines more than twice as long as this value will
     * be densified.
     *
     * @param lineDefs line definitions specifying the orientation, spacing and level of lines
     * @param lineFeatureBuilder the feature build to create {@code SimpleFeatures} from line
     *     elements
     * @param vertexSpacing maximum distance between adjacent vertices along a line
     * @param fc the feature collection into which generated line features are placed
     */
    public void buildGrid(
            Collection<OrthoLineDef> lineDefs,
            GridFeatureBuilder lineFeatureBuilder,
            double vertexSpacing,
            ListFeatureCollection fc) {

        init(lineDefs, lineFeatureBuilder, vertexSpacing);

        List<OrthoLineDef> horizontal = new ArrayList<>();
        List<OrthoLineDef> vertical = new ArrayList<>();

        for (OrthoLineDef lineDef : lineDefs) {
            switch (lineDef.getOrientation()) {
                case HORIZONTAL:
                    horizontal.add(lineDef);
                    break;

                case VERTICAL:
                    vertical.add(lineDef);
                    break;
            }
        }

        doBuildLineFeatures(
                horizontal,
                LineOrientation.HORIZONTAL,
                lineFeatureBuilder,
                densify,
                vertexSpacing,
                fc);
        doBuildLineFeatures(
                vertical, LineOrientation.VERTICAL, lineFeatureBuilder, densify, vertexSpacing, fc);
    }

    private void doBuildLineFeatures(
            List<OrthoLineDef> lineDefs,
            LineOrientation orientation,
            GridFeatureBuilder lineFeatureBuilder,
            boolean densify,
            double vertexSpacing,
            ListFeatureCollection fc) {

        final int NDEFS = lineDefs.size();
        if (NDEFS > 0) {
            double minOrdinate, maxOrdinate;

            if (orientation == LineOrientation.HORIZONTAL) {
                minOrdinate = gridBounds.getMinY();
                maxOrdinate = gridBounds.getMaxY();
            } else {
                minOrdinate = gridBounds.getMinX();
                maxOrdinate = gridBounds.getMaxX();
            }

            double[] pos = new double[NDEFS];
            boolean[] active = new boolean[NDEFS];
            boolean[] atCurPos = new boolean[NDEFS];
            boolean[] generate = new boolean[NDEFS];

            Map<String, Object> attributes = new HashMap<>();
            String geomPropName =
                    lineFeatureBuilder.getType().getGeometryDescriptor().getLocalName();

            for (int i = 0; i < NDEFS; i++) {
                pos[i] = minOrdinate;
                active[i] = true;
            }

            int numActive = NDEFS;
            while (numActive > 0) {
                /*
                 * Update scan position (curPos)
                 */
                double curPos = maxOrdinate;
                for (int i = 0; i < NDEFS; i++) {
                    if (active[i] && pos[i] < curPos - TOL) {
                        curPos = pos[i];
                    }
                }

                /*
                 * Check which line elements are at the current scan position
                 */
                for (int i = 0; i < NDEFS; i++) {
                    atCurPos[i] = active[i] && Math.abs(pos[i] - curPos) < TOL;
                }

                /*
                 * Get line with highest precedence for the current position
                 */
                System.arraycopy(atCurPos, 0, generate, 0, NDEFS);
                for (int i = 0; i < NDEFS - 1; i++) {
                    if (generate[i] && atCurPos[i]) {
                        for (int j = i + 1; j < NDEFS; j++) {
                            if (generate[j] && atCurPos[j]) {
                                if (lineDefs.get(i).getLevel() >= lineDefs.get(j).getLevel()) {
                                    generate[j] = false;
                                } else {
                                    generate[i] = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        generate[i] = false;
                    }
                }

                /*
                 * Create the line feature with highest precedence
                 */
                for (int i = 0; i < NDEFS; i++) {
                    if (generate[i]) {
                        OrthoLine element =
                                new OrthoLine(
                                        gridBounds,
                                        orientation,
                                        pos[i],
                                        lineDefs.get(i).getLevel());

                        if (lineFeatureBuilder.getCreateFeature(element)) {
                            lineFeatureBuilder.setAttributes(element, attributes);

                            if (densify) {
                                featureBuilder.set(
                                        geomPropName, element.toDenseGeometry(vertexSpacing));
                            } else {
                                featureBuilder.set(geomPropName, element.toGeometry());
                            }

                            for (String propName : attributes.keySet()) {
                                featureBuilder.set(propName, attributes.get(propName));
                            }

                            String featureID = lineFeatureBuilder.getFeatureID(element);
                            SimpleFeature feature = featureBuilder.buildFeature(featureID);
                            fc.add(feature);
                        }
                    }
                }

                /*
                 * Update line element positions
                 */
                for (int i = 0; i < NDEFS; i++) {
                    if (atCurPos[i]) {
                        pos[i] += lineDefs.get(i).getSpacing();
                        if (pos[i] > maxOrdinate + TOL) {
                            active[i] = false;
                            numActive--;
                        }
                    }
                }
            }
        }
    }

    private boolean isValidDenseVertexSpacing(double v) {
        double minDim;

        if (hasVerticals) {
            if (hasHorizontals) {
                minDim = Math.min(gridBounds.getWidth(), gridBounds.getHeight());
            } else {
                minDim = gridBounds.getHeight();
            }
        } else {
            minDim = gridBounds.getWidth();
        }

        return v > 0 && v < minDim / 2;
    }

    private void init(
            Collection<OrthoLineDef> controls,
            GridFeatureBuilder lineFeatureBuilder,
            double vertexSpacing) {

        if (gridBounds == null || gridBounds.isEmpty()) {
            throw new IllegalArgumentException("gridBounds must not be null or empty");
        }
        if (controls == null || controls.isEmpty()) {
            throw new IllegalArgumentException("required one or more line parameters");
        }

        for (OrthoLineDef param : controls) {
            if (param.getOrientation() == LineOrientation.HORIZONTAL) {
                hasHorizontals = true;
            } else if (param.getOrientation() == LineOrientation.VERTICAL) {
                hasVerticals = true;
            } else {
                throw new IllegalArgumentException(
                        "Only horizontal and vertical lines are supported");
            }
        }

        densify = isValidDenseVertexSpacing(vertexSpacing);
        featureBuilder = new SimpleFeatureBuilder(lineFeatureBuilder.getType());
    }
}
