/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import java.util.List;
import java.util.Map;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;

public class Grid extends Domain {

    private static final String TYPE = "Grid";

    public Grid(
            CoordinateReferenceSystem crs,
            List<DimensionBean> dimensions,
            GridGeometry2D gridGeometry,
            GranuleStack granuleStack) {
        super(TYPE, crs, dimensions, gridGeometry, granuleStack);
    }

    @Override
    protected void buildGeoAxis(
            CoordinateReferenceSystem crs, GridGeometry2D gridGeometry, Map<String, Axis> axes) {
        Envelope2D envelope = gridGeometry.getEnvelope2D();
        CoordinateSystem coordinateSystem = crs.getCoordinateSystem();
        for (int i = 0; i < coordinateSystem.getDimension(); i++) {
            CoordinateSystemAxis coordinateSystemAxis = coordinateSystem.getAxis(i);
            Axis axis = new Axis(getKey(coordinateSystemAxis));
            axis.setStart(envelope.getMinimum(i));
            axis.setStop(envelope.getMaximum(i));
            axis.setNum(gridGeometry.getGridRange().getSpan(i));
            axes.put(axis.getKey(), axis);
        }
    }
}
