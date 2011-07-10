/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;

/**
 * A process clipping the geometries in the input feature collection to a specified rectangle
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
@DescribeProcess(title = "rectangularClip", description = "Clips the features to the specified bounding box")
public class RectangularClipProcess implements GeoServerProcess {

    @DescribeResult(name = "result", description = "The feature collection bounds")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection to be simplified") SimpleFeatureCollection features,
            @DescribeParameter(name = "clip", description = "The clipping area") ReferencedEnvelope clip)
            throws ProcessException {
        return new ClipProcess().execute(features, JTS.toGeometry(clip));
    }

    
}
