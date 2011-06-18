/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Will reproject a geometry to another CRS. 
 * 
 * @author Andrea Aime
 */
@DescribeProcess(title = "reprojectGeometry", description = "Reprojects the specified geometry from the source CRS to the target one)")
public class ReprojectGeometry implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The reprojected geometry")
    public Geometry execute(
            @DescribeParameter(name = "geometry", description = "The geometry to be reprojected") Geometry geometry,
            @DescribeParameter(name = "sourceCRS", min = 0, description = "The source CRS") CoordinateReferenceSystem sourceCRS,
            @DescribeParameter(name = "targetCRS", min = 0, description = "The target CRS") CoordinateReferenceSystem targetCRS)
            throws Exception {

        return JTS.transform(geometry, CRS.findMathTransform(sourceCRS, targetCRS, true));
    }

}
