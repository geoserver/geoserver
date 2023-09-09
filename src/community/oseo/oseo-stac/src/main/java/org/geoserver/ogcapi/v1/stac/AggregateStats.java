/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.io.IOException;
import org.geotools.data.FeatureSource;

/** Get Aggregate Statistics for a collection */
public interface AggregateStats {
    /**
     * Get the aggregate statistics for a collection
     *
     * @param delegateProductSource The product source
     * @param collectionIdentifier The collection identifier
     * @param sourceProperty The source property
     * @return The aggregate statistics
     * @throws IOException If an error occurs
     */
    Object getStat(FeatureSource productSource, String collectionIdentifier, String sourceProperty)
            throws IOException;
}
