/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.UniqueVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.util.ProgressListener;

/**
 * Returns the unique values of a certain attribute
 * 
 * @author Andrea Aime
 */
@DescribeProcess(title = "Unique values", description = "Returns the unique values of a certain attribute")
public class UniqueProcess implements GeoServerProcess {
    // the functions this process can handle
    public enum AggregationFunction {
        Average, Max, Median, Min, StdDev, Sum;
    }

    @DescribeResult(name = "result", description = "The list of unique values extracted from the feature list")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection that will be inspected") SimpleFeatureCollection features,
            @DescribeParameter(name = "attribute", description = "The attribute whose unique values will be returned") String attribute,
            ProgressListener progressListener) throws Exception {

        int attIndex = -1;
        List<AttributeDescriptor> atts = features.getSchema().getAttributeDescriptors();
        for (int i = 0; i < atts.size(); i++) {
            if (atts.get(i).getLocalName().equals(attribute)) {
                attIndex = i;
                break;
            }
        }

        UniqueVisitor visitor = new UniqueVisitor(attIndex, features.getSchema());
        features.accepts(visitor, progressListener);
        List uniqueValues = visitor.getResult().toList();

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("value", features.getSchema().getDescriptor(attIndex).getType().getBinding());
        tb.setName("UniqueValue");
        SimpleFeatureType ft = tb.buildFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(ft);

        ListFeatureCollection result = new ListFeatureCollection(ft);
        for (Object value : uniqueValues) {
            fb.add(value);
            result.add(fb.buildFeature(null));
        }
        return result;
    }

    private List<String> attNames(List<AttributeDescriptor> atts) {
        List<String> result = new ArrayList<String>();
        for (AttributeDescriptor ad : atts) {
            result.add(ad.getLocalName());
        }
        return result;
    }

}
