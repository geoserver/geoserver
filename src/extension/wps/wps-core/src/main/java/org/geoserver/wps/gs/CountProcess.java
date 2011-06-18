/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;

/**
 * Counts the elements in the collection (useful as a WFS sidekick)
 * 
 * @author Andrea Aime
 */
@DescribeProcess(title = "countFeatures", description = "Counts the number of features in the specified collection")
public class CountProcess implements GeoServerProcess {
    // the functions this process can handle
    public enum AggregationFunction {
        Average, Max, Median, Min, StdDev, Sum;
    }

    @DescribeResult(name = "result", description = "The reprojected features")
    public Number execute(
            @DescribeParameter(name = "features", description = "The feature collection that will be aggregate") SimpleFeatureCollection features)
            throws Exception {

        return features.size();
    }

}
