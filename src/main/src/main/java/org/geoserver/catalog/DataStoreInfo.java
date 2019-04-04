/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.ProgressListener;

/**
 * A vector or feature based store.
 *
 * @author Justin Deoliveira, The Open Planning project
 */
public interface DataStoreInfo extends StoreInfo {

    /**
     * Returns the underlying datastore instance.
     *
     * <p>This method does I/O and is potentially blocking. The <tt>listener</tt> may be used to
     * report the progress of loading the datastore and also to report any errors or warnings that
     * occur.
     *
     * @param listener A progress listener, may be <code>null</code>.
     * @return The datastore.
     * @throws IOException Any I/O problems.
     */
    DataAccess<? extends FeatureType, ? extends Feature> getDataStore(ProgressListener listener)
            throws IOException;

    /**
     * DataStoreInfo equality is based on the following properties:
     *
     * <ul>
     *   <li>{@link StoreInfo#getId()}
     *   <li>{@link StoreInfo#getName()}
     *   <li>{@link StoreInfo#getDescription()}
     *   <li>{@link StoreInfo#getNamespace()}
     *   <li>{@link StoreInfo#isEnabled()}
     *   <li>{@link DataStoreInfo#getConnectionParameters()}
     * </ul>
     */
    boolean equals(Object obj);

    /**
     * Returns the feature resource from the store with the given name.
     *
     * <p><tt>listener</tt> is used to report the progress of finding the resource.
     *
     * @throws IOException Any I/O problems.
     */
    // FeatureResource getResource(String name, ProgressListener listener)
    //        throws IOException;

    /** Returns the feature resources provided by the store. */
    // Iterator<FeatureResource> getResources(ProgressListener monitor)
    //    throws IOException;
}
