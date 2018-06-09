/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.TypeBuilder;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.PropertyDescriptor;

class OrderedTypeBuilder extends TypeBuilder {

    static final FeatureTypeFactory TYPE_FACTORY = CommonFactoryFinder.getFeatureTypeFactory(null);

    public OrderedTypeBuilder() {
        super(TYPE_FACTORY);
    }

    @Override
    protected Collection<PropertyDescriptor> newCollection() {
        return new LinkedHashSet<PropertyDescriptor>();
    }
}
