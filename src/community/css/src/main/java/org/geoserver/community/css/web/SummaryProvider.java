/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A provider to make summaries available to the SummaryTable.
 */
class SummaryProvider extends GeoServerDataProvider<Summary> {
    private final List<Summary> summaries;

    public SummaryProvider(List<Summary> summaries) {
        this.summaries = summaries;
    }

    public SummaryProvider(FeatureCollection<SimpleFeatureType, SimpleFeature> data) {
        this(Summary.summarize(data));
    }

    @Override
    public List<Property<Summary>> getProperties() {
        List<Property<Summary>> props = new ArrayList<Property<Summary>>();
        props.add(
            new AbstractProperty<Summary>("Name") {
                public Object getPropertyValue(Summary sum) {
                    return sum.getName();
                }
            });
        
        props.add(
            new AbstractProperty<Summary>("Minimum") {
                public Object getPropertyValue(Summary sum) {
                    return sum.getMin();
                }
            });

        props.add(
            new AbstractProperty<Summary>("Maximum") {
                public Object getPropertyValue(Summary sum) {
                    return sum.getMax();
                }
            });

        return props;
    }

    @Override
    public List<Summary> getItems() {
        return Collections.unmodifiableList(summaries);
    }
}
