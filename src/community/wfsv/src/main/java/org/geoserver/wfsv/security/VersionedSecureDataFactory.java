package org.geoserver.wfsv.security;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.Response;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredObjectFactory;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.VersioningFeatureLocking;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.VersioningFeatureStore;

/**
 * A factory that applies the proper security policy to versionig related data
 * objects
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class VersionedSecureDataFactory implements SecuredObjectFactory {

    public boolean canSecure(Class clazz) {
        return VersioningDataStore.class.isAssignableFrom(clazz)
                || VersioningFeatureSource.class.isAssignableFrom(clazz)
                || VersioningFeatureStore.class.isAssignableFrom(clazz)
                || VersioningFeatureLocking.class.isAssignableFrom(clazz);
    }

    public Object secure(Object object, WrapperPolicy policy) {
        // null check
        if (object == null)
            return null;

        Class clazz = object.getClass();
        if (VersioningDataStore.class.isAssignableFrom(clazz)) {
            return new ReadOnlyVersioningDataStore((VersioningDataStore) object, policy);
        }

        if (VersioningFeatureSource.class.isAssignableFrom(clazz)) {
            // if the policy is not challenge, since this is a secured object,
            // it must be read only (we don't wrap native objects if not
            // required in order to add a security restriction)
            if (policy.response != Response.CHALLENGE) {
                return new ReadOnlyVersioningFeatureSource((VersioningFeatureSource) object,
                        policy);
            } else if (VersioningFeatureLocking.class.isAssignableFrom(clazz)) {
                return new ReadOnlyVersioningFeatureLocking((VersioningFeatureLocking) object,
                        policy);
            } else if (VersioningFeatureStore.class.isAssignableFrom(clazz)) {
                return new ReadOnlyVersioningFeatureStore((VersioningFeatureStore) object,
                        policy);
            } else if (VersioningFeatureSource.class.isAssignableFrom(clazz)) {
                return new ReadOnlyVersioningFeatureSource((VersioningFeatureSource) object,
                        policy);
            }
        }

        throw new IllegalArgumentException("Cannot wrap objects of class " + clazz);
    }

    public int getPriority() {
        return (ExtensionPriority.LOWEST + ExtensionPriority.HIGHEST) / 2;
    }

}
