package org.geoserver.data.versioning.decorator;

import org.geogit.repository.Repository;
import org.geoserver.data.versioning.VersioningDataStore;
import org.geoserver.geogit.GEOGIT;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.type.Name;
import org.springframework.util.Assert;

public class VersioningAdapterFactory {

    @SuppressWarnings({ "rawtypes" })
    public static FeatureSource create(final FeatureSource subject) {
        final Repository versioningRepo = GEOGIT.get().getRepository();

        final Name typeName = subject.getSchema().getName();

        if (subject.getDataStore() instanceof VersioningDataStore) {
            return subject;
        }

        // do not wrap if not versioned
        if (!FeatureSourceDecorator.isVersioned(typeName, versioningRepo)) {
            return subject;
        }

        if (subject instanceof SimpleFeatureLocking) {
            return new SimpleFeatureLockingDecorator((SimpleFeatureLocking) subject, versioningRepo);
        }
        if (subject instanceof SimpleFeatureStore) {
            return new SimpleFeatureStoreDecorator((SimpleFeatureStore) subject, versioningRepo);
        }
        if (subject instanceof SimpleFeatureSource) {
            return new SimpleFeatureSourceDecorator((SimpleFeatureSource) subject, versioningRepo);
        }

        if (subject instanceof FeatureLocking) {
            return new FeatureLockingDecorator((FeatureLocking) subject, versioningRepo);
        }
        if (subject instanceof FeatureStore) {
            return new FeatureStoreDecorator((FeatureStore) subject, versioningRepo);
        }

        return new FeatureSourceDecorator(subject, versioningRepo);
    }

    @SuppressWarnings("rawtypes")
    public static DataAccess create(final DataAccess subject) {
        Assert.notNull(subject);

        final Repository versioningRepo = GEOGIT.get().getRepository();

        // if (subject instanceof DataStore) {
        // return new VersioningDataStore((DataStore) subject, versioningRepo);
        // }

        return new DataAccessDecorator((DataStore) subject, versioningRepo);
    }
}
