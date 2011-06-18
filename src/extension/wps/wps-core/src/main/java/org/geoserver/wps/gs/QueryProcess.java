/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.List;

import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.ProcessException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

@DescribeProcess(title = "Query", description = "Applies a filter and an attribute selection to the incoming feature collection. "
        + "The process can be also used as a pure format converter when no filtering or attribute selection is performed")
public class QueryProcess implements GeoServerProcess {
    @DescribeResult(name = "result", description = "The filtered collection")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection to filter") SimpleFeatureCollection features,
            @DescribeParameter(name = "attribute", description = "The attribute name(s)", collectionType = String.class, min = 0) List<String> attributes,
            @DescribeParameter(name = "filter", min = 0, description = "Filters the input features") Filter filter)
            throws ProcessException {
        // apply filtering if necessary
        if (filter != null && !filter.equals(Filter.INCLUDE)) {
            features = new FilteringSimpleFeatureCollection(features, filter);
        }

        // apply retyping if necessary
        if (attributes != null && attributes.size() > 0) {
            final String[] names = (String[]) attributes.toArray(new String[attributes.size()]);
            SimpleFeatureType ft = SimpleFeatureTypeBuilder.retype(features.getSchema(), names);
            if (!(ft.equals(features.getSchema()))) {
                features = new ReTypingFeatureCollection(features, ft);
            }
        }

        return features;
    }

}
