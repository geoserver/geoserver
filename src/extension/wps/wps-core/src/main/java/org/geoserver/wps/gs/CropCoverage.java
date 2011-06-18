/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * Crops the coverage along the specified 
 * 
 * @author Andrea Aime - GeoSolutions
 * @author ETj <etj at geo-solutions.it>
 */
@DescribeProcess(title = "cropCoverage", description = "Collects all the default geometries in the feature collection and returns them as a single geometry collection")
public class CropCoverage implements GeoServerProcess {

    private static final CoverageProcessor PROCESSOR = CoverageProcessor.getInstance();
    private static final Operation CROP = PROCESSOR.getOperation("CoverageCrop");

    @DescribeResult(name = "result", description = "The cropped raster")
    public GridCoverage2D execute(
            @DescribeParameter(name = "coverage", description = "The raster to be cropped") GridCoverage2D coverage,
            @DescribeParameter(name = "cropShape", description = "The geometry used to crop the raster (either single or a collection)") Geometry cropShape,
            ProgressListener progressListener) throws IOException {
        // get the bounds
        CoordinateReferenceSystem crs;
        if (cropShape.getUserData() instanceof CoordinateReferenceSystem) {
            crs = (CoordinateReferenceSystem) cropShape.getUserData();
        } else {
            // assume the geometry is in the same crs
            crs = coverage.getCoordinateReferenceSystem();
        }
        GeneralEnvelope bounds = new GeneralEnvelope(new ReferencedEnvelope(cropShape.getEnvelopeInternal(), crs));

        // force it to a collection if necessary
        GeometryCollection roi;
        if (!(cropShape instanceof GeometryCollection)) {
            roi = cropShape.getFactory().createGeometryCollection(new Geometry[] { cropShape });
        } else {
            roi = (GeometryCollection) cropShape;
        }

        // perform the crops
        final ParameterValueGroup param = CROP.getParameters();
        param.parameter("Source").setValue(coverage);
        param.parameter("Envelope").setValue(bounds);
        param.parameter("ROI").setValue(roi);

        return (GridCoverage2D) PROCESSOR.doOperation(param);
    }

}
