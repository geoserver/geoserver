/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.Response;
import org.geoserver.security.WrapperPolicy;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wmts.WebMapTileServer;

/**
 * The default secured wrapper factory, used as a fallback when no other, more specific factory can
 * be used.
 *
 * <p><b>Implementation note</b>: this factory uses actual decorator objects to perform the secure
 * wrapping. <br>
 * Proxies and invocation handlers could be used instead, the catch is that they would be likely to
 * fail in an event of a refactoring or the wrapped interfaces. <br>
 * Given that it's security we're talking about, a type safe approach that gives a compile error
 * right away has been preferred.
 *
 * @author Andrea Aime - TOPP
 */
public class DefaultSecureDataFactory implements SecuredObjectFactory {

    public boolean canSecure(Class clazz) {
        return DataAccess.class.isAssignableFrom(clazz)
                || DataStore.class.isAssignableFrom(clazz)
                || FeatureSource.class.isAssignableFrom(clazz)
                || FeatureStore.class.isAssignableFrom(clazz)
                || FeatureLocking.class.isAssignableFrom(clazz)
                || FeatureCollection.class.isAssignableFrom(clazz)
                || FeatureIterator.class.isAssignableFrom(clazz)
                || GridCoverage2DReader.class.isAssignableFrom(clazz)
                || StructuredGridCoverage2DReader.class.isAssignableFrom(clazz)
                || AbstractGridFormat.class.isAssignableFrom(clazz)
                || WebMapServer.class.isAssignableFrom(clazz)
                || WebMapTileServer.class.isAssignableFrom(clazz);
    }

    public Object secure(Object object, WrapperPolicy policy) {
        // null check
        if (object == null) return null;

        // wrapping check
        Class clazz = object.getClass();
        if (!canSecure(clazz))
            throw new IllegalArgumentException(
                    "Don't know how to wrap objects of class " + object.getClass());

        // scan classes from the most specific to the most general (inheritance
        // wise). Start with data stores and data access, which do provide
        // metadata
        if (DataStore.class.isAssignableFrom(clazz)) {
            return new ReadOnlyDataStore((DataStore) object, policy);
        } else if (DataAccess.class.isAssignableFrom(clazz)) {
            return new ReadOnlyDataAccess((DataAccess) object, policy);
        }

        // for FeatureSource and family, we only return writable wrappers if the
        // challenge mode is set to true, otherwise we're hide mode and we
        // should just return a read only wrapper, a FeatureSource
        if (SimpleFeatureSource.class.isAssignableFrom(clazz)) {
            if ((policy.level == AccessLevel.READ_ONLY
                            || policy.level == AccessLevel.METADATA
                            || policy.level == AccessLevel.HIDDEN)
                    && policy.response != Response.CHALLENGE) {
                return new SecuredSimpleFeatureSource((SimpleFeatureSource) object, policy);
            } else if (SimpleFeatureLocking.class.isAssignableFrom(clazz)) {
                return new SecuredSimpleFeatureLocking((SimpleFeatureLocking) object, policy);
            } else if (SimpleFeatureStore.class.isAssignableFrom(clazz)) {
                return new SecuredSimpleFeatureStore((SimpleFeatureStore) object, policy);
            } else if (SimpleFeatureSource.class.isAssignableFrom(clazz)) {
                return new SecuredSimpleFeatureSource((SimpleFeatureSource) object, policy);
            }
        } else if (FeatureSource.class.isAssignableFrom(clazz)) {
            if ((policy.level == AccessLevel.READ_ONLY
                            || policy.level == AccessLevel.METADATA
                            || policy.level == AccessLevel.HIDDEN)
                    && policy.response != Response.CHALLENGE) {
                return new SecuredFeatureSource((FeatureSource) object, policy);
            } else if (FeatureLocking.class.isAssignableFrom(clazz)) {
                return new SecuredFeatureLocking((FeatureLocking) object, policy);
            } else if (FeatureStore.class.isAssignableFrom(clazz)) {
                return new SecuredFeatureStore((FeatureStore) object, policy);
            } else if (FeatureSource.class.isAssignableFrom(clazz)) {
                return new SecuredFeatureSource((FeatureSource) object, policy);
            }
        }

        // deal with feature collection and family
        if (SimpleFeatureCollection.class.isAssignableFrom(clazz)) {
            return new SecuredSimpleFeatureCollection((SimpleFeatureCollection) object, policy);
        } else if (FeatureCollection.class.isAssignableFrom(clazz)) {
            return new SecuredFeatureCollection((FeatureCollection) object, policy);
        } else if (SimpleFeatureIterator.class.isAssignableFrom(clazz)) {
            return new SecuredSimpleFeatureIterator((SimpleFeatureIterator) object);
        } else if (FeatureIterator.class.isAssignableFrom(clazz)) {
            return new SecuredFeatureIterator((FeatureIterator) object);
        }

        // try coverage readers and formats
        if (StructuredGridCoverage2DReader.class.isAssignableFrom(clazz)) {
            return new SecuredStructuredGridCoverage2DReader(
                    (StructuredGridCoverage2DReader) object, policy);
        } else if (GridCoverage2DReader.class.isAssignableFrom(clazz)) {
            return new SecuredGridCoverage2DReader((GridCoverage2DReader) object, policy);
        } else if (AbstractGridFormat.class.isAssignableFrom(clazz)) {
            return new SecuredGridFormat((AbstractGridFormat) object, policy);
        }

        // wms cascading related
        if (WebMapServer.class.isAssignableFrom(clazz)) {
            try {
                return new SecuredWebMapServer((WebMapServer) object);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error wrapping the web map server", e);
            }
        }
        if (WebMapTileServer.class.isAssignableFrom(clazz)) {
            try {
                return new SecuredWebMapTileServer((WebMapTileServer) object);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error wrapping the web map tile server", e);
            }
        }

        // all attempts have been made, we don't know how to handle this object
        throw new IllegalArgumentException(
                "Don't know how to wrap objects of class " + object.getClass());
    }

    /** Returns {@link ExtensionPriority#LOWEST} since the wrappers generated by this factory */
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
