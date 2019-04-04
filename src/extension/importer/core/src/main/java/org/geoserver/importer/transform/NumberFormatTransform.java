/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;

public class NumberFormatTransform extends AttributeRemapTransform {

    public NumberFormatTransform(String field, Class<? extends Number> type) {
        super(field, type);
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object val = oldFeature.getAttribute(field);
        if (val != null) {
            feature.setAttribute(field, Converters.convert(val, type));
        }
        return feature;
    }
}
