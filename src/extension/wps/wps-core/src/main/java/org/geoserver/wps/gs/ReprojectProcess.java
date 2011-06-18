/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Will reproject the features to another CRS. Can also be used to force a known CRS onto a dataset
 * that does not have ones
 * 
 * @author Andrea Aime
 */
@DescribeProcess(title = "reprojectFeatures", description = "Reprojects the specified features to another CRS, can also be used to force a known CRS onto a set of feaures that miss one (or that have a wrong one)")
public class ReprojectProcess implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The reprojected features")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection that will be reprojected") SimpleFeatureCollection features,
            @DescribeParameter(name = "forcedCRS", min = 0, description = "Forces a certain CRS on features before reprojection") CoordinateReferenceSystem forcedCRS,
            @DescribeParameter(name = "targetCRS", min = 0, description = "Features will be reprojected from their native/forced CRS to the target CRS") CoordinateReferenceSystem targetCRS)
            throws Exception {

        if (forcedCRS != null) {
            features = new ForceCoordinateSystemFeatureResults(features, forcedCRS, false);
        }
        if (targetCRS != null) {
            // note, using ReprojectFeatureResults would work. However that would
            // just work by accident... see GEOS-4072
            features = new ReprojectingFeatureCollection(features, targetCRS);
        }
        return features;
    }

}
