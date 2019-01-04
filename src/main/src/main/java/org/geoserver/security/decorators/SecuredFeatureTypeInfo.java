/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.factory.Hints;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Wraps a {@link FeatureTypeInfo} so that it will return a secured FeatureSource
 *
 * @author Andrea Aime - TOPP
 */
public class SecuredFeatureTypeInfo extends DecoratingFeatureTypeInfo {

    WrapperPolicy policy;

    public SecuredFeatureTypeInfo(FeatureTypeInfo info, WrapperPolicy policy) {
        super(info);
        this.policy = policy;
    }

    @Override
    public FeatureType getFeatureType() throws IOException {

        FeatureType ft = super.getFeatureType();

        if (policy.getLimits() == null) {
            return ft;
        } else if (policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits val = (VectorAccessLimits) policy.getLimits();

            // get what we can actually read (and it makes it easier to deal with property names)
            Query query = val.getReadQuery();

            // do we have any attribute filtering?
            if (query.getPropertyNames() == Query.ALL_NAMES) {
                return ft;
            }

            if (ft instanceof SimpleFeatureType) {
                SimpleFeatureType sft = (SimpleFeatureType) ft;
                Set<String> properties =
                        new HashSet<String>(Arrays.asList(query.getPropertyNames()));
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(sft);
                for (AttributeDescriptor at : sft.getAttributeDescriptors()) {
                    String attName = at.getLocalName();
                    if (!properties.contains(attName)) {
                        tb.remove(attName);
                    }
                }
                return tb.buildFeatureType();
            } else {
                // if it's a complex type, we don't have a type builder on all branches, so
                // we'll run an empty query instead

                query.setFilter(Filter.EXCLUDE);
                FeatureSource fs = getFeatureSource(null, null);
                FeatureCollection fc = fs.getFeatures(query);
                return fc.getSchema();
            }
        } else {
            throw new IllegalArgumentException(
                    "SecureFeatureSources has been fed "
                            + "with unexpected AccessLimits class "
                            + policy.getLimits().getClass());
        }
    }

    // --------------------------------------------------------------------------
    // WRAPPED METHODS TO ENFORCE SECURITY POLICY
    // --------------------------------------------------------------------------

    @Override
    public FeatureSource getFeatureSource(ProgressListener listener, Hints hints)
            throws IOException {
        final FeatureSource fs = delegate.getFeatureSource(listener, hints);

        if (policy.level == AccessLevel.METADATA) {
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        } else {
            return (FeatureSource) SecuredObjects.secure(fs, policy);
        }
    }

    @Override
    public DataStoreInfo getStore() {
        return (DataStoreInfo) SecuredObjects.secure(delegate.getStore(), policy);
    }

    @Override
    public void setStore(StoreInfo store) {
        // need to make sure the store isn't secured
        super.setStore((StoreInfo) SecureCatalogImpl.unwrap(store));
    }
}
