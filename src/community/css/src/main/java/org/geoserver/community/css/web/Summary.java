/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.vividsolutions.jts.geom.Geometry;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * The Summary class represents an entry in the SummaryTable; including a
 * propertyname, minimum, and maximum value.
 */
class Summary implements Serializable {
    final private String name;
    final private Object min;
    final private Object max;

    static final private Logger LOGGER =
        org.geotools.util.logging.Logging.getLogger(Summary.class);

    public Summary(String name, Object min, Object max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    public String getName() { return name; }
    public Object getMin() { return min; }
    public Object getMax() { return max; }

    public static List<Summary> summarize(
        FeatureCollection<SimpleFeatureType, SimpleFeature> data
    ) {
        final List<AttributeDescriptor> comparable = new ArrayList<AttributeDescriptor>();
        final List<AttributeDescriptor> noncomparable = new ArrayList<AttributeDescriptor>();

        for (AttributeDescriptor att : data.getSchema().getAttributeDescriptors()) {
            if (Comparable.class.isAssignableFrom(att.getType().getBinding()) &&
                !Geometry.class.isAssignableFrom(att.getType().getBinding())) {
                comparable.add(att);
            } else {
                noncomparable.add(att);
            }
        }

        LOGGER.log(Level.FINEST, "Comparable attributes: " + comparable);
        LOGGER.log(Level.FINEST, "Non-comparable attributes: " + noncomparable);

        Map<AttributeDescriptor, Comparable<Object>> minima = new HashMap();
        Map<AttributeDescriptor, Comparable<Object>> maxima = new HashMap();

        FeatureIterator<SimpleFeature> it = data.features();

        try {
            while(it.hasNext()) {
                SimpleFeature f = it.next();
                for (AttributeDescriptor att : comparable) {
                    Comparable<Object> value = (Comparable<Object>)f.getAttribute(att.getName());
                    if (value != null) {
                       final Comparable<Object> min =
                           minima.containsKey(att) ? minima.get(att) : value;
                       final Comparable<Object> max =
                           maxima.containsKey(att) ? maxima.get(att) : value;
                       minima.put(att, value.compareTo(min) < 0 ? value : min);
                       maxima.put(att, value.compareTo(max) < 0 ? value : max);
                    }
                }
            }
        } finally {
            it.close();
        }

        final List<Summary> summaries = new ArrayList<Summary>();
        for (AttributeDescriptor att : comparable) {
            summaries.add(new Summary(att.getLocalName(), minima.get(att), maxima.get(att)));
        }

        for (AttributeDescriptor att : noncomparable) {
            summaries.add(new Summary(att.getLocalName(), "[n/a]", "[n/a]"));
        }
        
        return summaries;
    }
}
