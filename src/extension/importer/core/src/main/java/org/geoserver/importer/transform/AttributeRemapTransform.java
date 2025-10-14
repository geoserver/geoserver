/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.Serial;
import org.geoserver.importer.ImportTask;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

/**
 * Attribute that maps an attribute from one type to another.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AttributeRemapTransform extends AbstractTransform implements InlineVectorTransform {

    @Serial
    private static final long serialVersionUID = 1L;

    /** field to remap */
    protected String field;

    /** type to remap to */
    protected Class<?> type;

    public AttributeRemapTransform(String field, Class<?> type) {
        this.field = field;
        this.type = type;
    }

    protected AttributeRemapTransform() {}

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public SimpleFeatureType apply(ImportTask task, DataStore dataStore, SimpleFeatureType featureType)
            throws Exception {
        // remap the type
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);

        int index = featureType.indexOf(field);
        if (index < 0) {
            throw new Exception(
                    "FeatureType " + featureType.getName() + " does not have attribute named '" + field + "'");
        }

        // remap the attribute to type date and ensure schema ordering is the same
        // @todo improve FeatureTypeBuilder to support this directly
        AttributeDescriptor existing = builder.remove(field);
        AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
        attBuilder.init(existing);
        attBuilder.setBinding(type);
        builder.add(index, attBuilder.buildDescriptor(field));

        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        return feature;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "field='" + field + '\'' + ", type=" + type + '}';
    }
}
