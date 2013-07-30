package org.geoserver.importer.transform;

import org.geotools.data.DataStore;
import org.geotools.util.Converters;
import org.geoserver.importer.ImportTask;
import org.opengis.feature.simple.SimpleFeature;

public class NumberFormatTransform extends AttributeRemapTransform {

    public NumberFormatTransform(String field, Class<? extends Number> type) {
        super(field, type);
    }

    @Override
    public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature, 
        SimpleFeature feature) throws Exception {
        Object val = feature.getAttribute(field);
        if (val != null) {
            feature.setAttribute(field, Converters.convert(val, type));
        }
        return feature;
    }
}
