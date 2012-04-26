package org.geoserver.data.versioning.decorator;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DecoratingFeature;
import org.geotools.filter.identity.ResourceIdImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.ResourceId;

import com.sun.org.apache.xpath.internal.operations.Gte;

class VersionedFeatureWrapper {

    @SuppressWarnings("unchecked")
    public static <F extends Feature> F wrap(final F f, final String versionId) {
        if (versionId == null) {
            return f;
        }
        if (f instanceof SimpleFeature) {
            return (F) new SimpleFeatureWrapper((SimpleFeature) f, versionId);
        }
        throw new UnsupportedOperationException("Non simple Features are not yet supported: " + f);
    }

    private static final class SimpleFeatureWrapper extends DecoratingFeature implements
            SimpleFeature {
        private static final FilterFactory2 FILTER_FACTORY = CommonFactoryFinder
                .getFilterFactory2(null);

        private final String versionId;

        public SimpleFeatureWrapper(final SimpleFeature delegate, final String versionId) {
            super(delegate);
            this.versionId = versionId;
        }

        @Override
        public FeatureId getIdentifier() {
            FeatureId rid = FILTER_FACTORY.featureId(super.getID(), versionId);
            return rid;
        }

        @Override
        public String getID() {
            return super.getID(); // + '@' + versionId;
        }
    }
}