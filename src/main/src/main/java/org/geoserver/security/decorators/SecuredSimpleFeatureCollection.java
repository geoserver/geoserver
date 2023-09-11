/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;

/**
 * Simple version of {@link SecuredFeatureCollection}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredSimpleFeatureCollection
        extends SecuredFeatureCollection<SimpleFeatureType, SimpleFeature>
        implements SimpleFeatureCollection {

    SimpleFeatureType readSchema;

    SecuredSimpleFeatureCollection(
            FeatureCollection<SimpleFeatureType, SimpleFeature> delegate, WrapperPolicy policy) {
        super(delegate, policy);
        if (policy.getLimits() instanceof VectorAccessLimits) {
            List<PropertyName> properties =
                    ((VectorAccessLimits) policy.getLimits()).getReadAttributes();
            if (properties == null) {
                this.readSchema = getSchema();
            } else {
                List<String> names = new ArrayList<>();
                for (PropertyName property : properties) {
                    names.add(property.getPropertyName());
                }
                String[] nameArray = names.toArray(new String[names.size()]);
                try {
                    this.readSchema = DataUtilities.createSubType(getSchema(), nameArray);
                } catch (SchemaException e) {
                    // should just not happen
                    throw new RuntimeException(e);
                }
            }
        } else {
            this.readSchema = getSchema();
        }
    }

    @Override
    public SimpleFeatureCollection sort(SortBy order) {
        return (SimpleFeatureCollection) super.sort(order);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        return (SimpleFeatureCollection) super.subCollection(filter);
    }

    @Override
    public SimpleFeatureIterator features() {
        return (SimpleFeatureIterator) super.features();
    }

    @Override
    public void accepts(
            org.geotools.api.feature.FeatureVisitor visitor,
            org.geotools.api.util.ProgressListener progress)
            throws IOException {
        if (canDelegate(visitor)) {
            delegate.accepts(visitor, progress);
        } else {
            super.accepts(visitor, progress);
        }
    }

    @Override
    protected boolean canDelegate(FeatureVisitor visitor) {
        return ReTypingFeatureCollection.isTypeCompatible(visitor, readSchema);
    }
}
