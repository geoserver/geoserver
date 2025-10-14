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
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.util.InternationalString;
import org.geotools.api.util.ProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.factory.Hints;

/**
 * Wraps a {@link FeatureTypeInfo} so that it will return a secured FeatureSource
 *
 * @author Andrea Aime - TOPP
 */
public class SecuredFeatureTypeInfo extends DecoratingFeatureTypeInfo {

    protected static final String GET_CAPABILITIES = "GetCapabilities";

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

            if (ft instanceof SimpleFeatureType sft) {
                Set<String> properties = new HashSet<>(Arrays.asList(query.getPropertyNames()));
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
            throw new IllegalArgumentException("SecureFeatureSources has been fed "
                    + "with unexpected AccessLimits class "
                    + policy.getLimits().getClass());
        }
    }

    // --------------------------------------------------------------------------
    // WRAPPED METHODS TO ENFORCE SECURITY POLICY
    // --------------------------------------------------------------------------

    @Override
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(
            ProgressListener listener, Hints hints) throws IOException {
        final FeatureSource<? extends FeatureType, ? extends Feature> fs = delegate.getFeatureSource(listener, hints);
        Request request = Dispatcher.REQUEST.get();
        if (policy.level == AccessLevel.METADATA && !isGetCapabilities(request)) {
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        } else {
            return SecuredObjects.secure(fs, computeWrapperPolicy(request));
        }
    }

    /**
     * Checks if current request is GetCapabilities and returns a new WrapperPolicy with attributes read allowed to
     * compute the dimensions values. If current request is not a GetCapabilities one, returns the same policy without
     * changes.
     */
    private WrapperPolicy computeWrapperPolicy(Request request) {
        if (isGetCapabilities(request) && policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits accessLimits = (VectorAccessLimits) policy.getLimits();
            VectorAccessLimits newLimits = (VectorAccessLimits) accessLimits.clone();
            newLimits.setReadAttributes(null);
            newLimits.setReadFilter(Filter.INCLUDE);
            WrapperPolicy newPolicy = WrapperPolicy.readOnlyChallenge(newLimits);
            return newPolicy;
        }
        return policy;
    }

    /** Returns true only if current request is a GetCapabilities, otherwise returns false. */
    private boolean isGetCapabilities(Request request) {
        if (request == null) return false;
        return GET_CAPABILITIES.equalsIgnoreCase(request.getRequest());
    }

    @Override
    public DataStoreInfo getStore() {
        return SecuredObjects.secure(delegate.getStore(), policy);
    }

    @Override
    public void setStore(StoreInfo store) {
        // need to make sure the store isn't secured
        super.setStore((StoreInfo) SecureCatalogImpl.unwrap(store));
    }

    @Override
    public InternationalString getInternationalTitle() {
        return delegate.getInternationalTitle();
    }

    @Override
    public void setInternationalTitle(InternationalString internationalTitle) {
        delegate.setInternationalTitle(internationalTitle);
    }

    @Override
    public InternationalString getInternationalAbstract() {
        return delegate.getInternationalAbstract();
    }

    @Override
    public void setInternationalAbstract(InternationalString internationalAbstract) {
        delegate.setInternationalAbstract(internationalAbstract);
    }
}
