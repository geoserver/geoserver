/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.Map;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.GridElement;
import org.geotools.grid.GridFeatureBuilder;
import org.geotools.grid.PolygonElement;
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.grid.hexagon.Hexagons;
import org.geotools.grid.oblong.Oblongs;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;

/**
 * A process that builds a regular grid as a feature collection
 * 
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(title = "grid", description = "Builds a regular cell grid")
public class GridProcess implements GeoServerProcess {

    enum GridMode {
        Rectangular, HexagonFlat, HexagonAngled
    };

    @DescribeResult(name = "result", description = "The grid")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The grid bounds") ReferencedEnvelope bounds,
            @DescribeParameter(name = "width", description = "Cell width (in the same uom as the bounds referencing system)") double width,
            @DescribeParameter(name = "height", description = "Cell height (optional, used only for rectangular grids, "
                    + "if not provided it is assumed equals to the width)", min = 0) Double height,
            @DescribeParameter(name = "vertexSpacing", description = "Distance between vertices (used to create densified " +
            		"sides suitable for reprojection)", min = 0) Double vertexSpacing,
            @DescribeParameter(name = "mode", description = "The type of grid to be generated", min = 0) GridMode mode)
            throws ProcessException {
        final GridFeatureBuilder builder = new GridFeatureBuilderImpl(bounds
                .getCoordinateReferenceSystem());
        double h = height != null ? height : width;

        SimpleFeatureSource source;
        if (mode == null || mode == GridMode.Rectangular) {
            source = Oblongs.createGrid(bounds, width, h, builder);
        } else if (mode == GridMode.HexagonFlat) {
            source = Hexagons.createGrid(bounds, width, HexagonOrientation.FLAT, builder);
        } else {
            source = Hexagons.createGrid(bounds, width, HexagonOrientation.ANGLED, builder);
        }

        try {
            return source.getFeatures();
        } catch (IOException e) {
            throw new WPSException("Unexpected exception while grabbing features", e);
        }
    }

    /**
     * Builds the feature attributes providing the cell center and a stable id
     * 
     * @author Andrea Aime - GeoSolutions
     */
    static final class GridFeatureBuilderImpl extends GridFeatureBuilder {
        private int id;

        /**
         * Creates the feature TYPE
         * 
         * @param typeName
         *            name for the feature TYPE; if {@code null} or empty,
         *            {@linkplain #DEFAULT_TYPE_NAME} will be used
         * 
         * @param crs
         *            coordinate reference system (may be {@code null})
         * 
         * @return the feature TYPE
         */
        protected static SimpleFeatureType createType(CoordinateReferenceSystem crs) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName("grid");
            tb.add("cell", Polygon.class, crs);
            tb.add("id", Integer.class);
            tb.add("centerX", Double.class);
            tb.add("centerY", Double.class);
            return tb.buildFeatureType();
        }

        /**
         * Creates a new instance.
         * 
         * @param crs
         *            coordinate reference system (may be {@code null})
         */
        public GridFeatureBuilderImpl(CoordinateReferenceSystem crs) {
            super(createType(crs));
        }

        @Override
        public String getFeatureID(GridElement ge) {
            return String.valueOf("grid." + (id++));
        }

        /**
         * Overrides {@linkplain GridFeatureBuilder#setAttributes(GridElement, Map)} to assign a
         * sequential integer id value to each grid element feature as it is constructed.
         * 
         * @param el
         *            the element from which the new feature is being constructed
         * 
         * @param attributes
         *            a {@code Map} with the single key "id"
         */
        @Override
        public void setAttributes(GridElement ge, Map<String, Object> attributes) {
        	PolygonElement pe = (PolygonElement) ge;
            attributes.put("id", id);
            attributes.put("centerX", pe.getCenter().x);
            attributes.put("centerY", pe.getCenter().y);
        }

    }

}
