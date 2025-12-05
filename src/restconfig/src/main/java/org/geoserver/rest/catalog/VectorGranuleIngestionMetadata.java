/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.net.URI;
import java.util.Properties;
import org.locationtech.jts.geom.Envelope;

/**
 * Simple holder for VectorGranule ingestion metadata, including the granule URI, associated properties and its
 * footprint.
 *
 * <p>This class supports VectorMosaic ingestion. A {@link VectorGranuleIngestionConfigurer} processes an input vector
 * dataset and prepares the ingestion configuration. Different configurer implementations may adjust parameters
 * differently or use optimized logic to determine the dataset’s footprint. Although primarily used by VectorMosaic,
 * other community modules (such as OGC-DGGS) also rely on it, so it resides in the restconfig module.
 */
public class VectorGranuleIngestionMetadata {
    URI uri;
    Properties params;
    Envelope footprint;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Properties getParams() {
        return params;
    }

    public void setParams(Properties params) {
        this.params = params;
    }

    public Envelope getFootprint() {
        return footprint;
    }

    public void setFootprint(Envelope footprint) {
        this.footprint = footprint;
    }
}
