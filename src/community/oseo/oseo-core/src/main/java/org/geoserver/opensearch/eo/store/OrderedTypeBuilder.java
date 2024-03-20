/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.FeatureTypeFactory;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.TypeBuilder;

class OrderedTypeBuilder extends TypeBuilder {

    static final FeatureTypeFactory TYPE_FACTORY = CommonFactoryFinder.getFeatureTypeFactory(null);

    public OrderedTypeBuilder() {
        super(TYPE_FACTORY);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<PropertyDescriptor> newCollection() {
        return new LinkedHashSet<PropertyDescriptor>();
    }

    @Override
    public AttributeType getBinding(Class binding) {
        AttributeType type = bindings().get(binding);
        if (type == null) {
            AttributeTypeBuilder atb = new AttributeTypeBuilder();
            atb.setBinding(binding);
            atb.setName(binding.getSimpleName());
            type = atb.buildType();
            bindings().put(binding, type);
        }
        return type;
    }
}
