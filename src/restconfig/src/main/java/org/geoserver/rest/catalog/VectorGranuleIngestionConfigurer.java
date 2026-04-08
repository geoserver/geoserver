/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.Properties;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Extension point used during vector granule ingestion into a VectorMosaic, performing additional configuration and
 * metadata extraction for ingested granules.
 *
 * <p>A {@link VectorGranuleIngestionConfigurer} receives the raw ingested granule together with their associated
 * properties, and is responsible for producing a {@link VectorGranuleIngestionMetadata} instance describing how the
 * granule should be interpreted, referenced, and later ingested.
 *
 * <p>Typical implementations may:
 *
 * <ul>
 *   <li>normalize or enrich resource-specific properties;
 *   <li>merge generic ingestion configuration with per-resource settings;
 *   <li>inspect the ingested granule to extract metadata such as bounding boxes, connection parameters, or identifying
 *       attributes;
 *   <li>prepare the final configuration that subsequent ingestion steps will use.
 * </ul>
 *
 * This interface is shared by the VectorMosaic community modules and the OGC-API DGGS implementation. To prevent
 * cross-module dependencies, it is kept in the restconfig package.
 */
public interface VectorGranuleIngestionConfigurer {

    GeometryFactory GF = new GeometryFactory();

    /** Return the name of this configurer */
    String getName();

    /**
     * Builds and returns a {@link VectorGranuleIngestionMetadata} instance for the given resource.
     *
     * @param resource the resource for which metadata should be generated
     * @param resourceProperties properties specific to the individual resource
     * @param commonProperties configuration settings shared across the harvesting process
     * @return the configured {@link VectorGranuleIngestionMetadata} for the specified resource
     * @throws Exception if metadata creation or configuration fails
     */
    VectorGranuleIngestionMetadata configureMetadata(
            Object resource, Properties resourceProperties, Properties commonProperties) throws Exception;

    /**
     * A default implementation is getting the Envelope from the specified featureSource, by invoking the getBounds on
     * the underlying features. Specific implementation could provide optimized searches.
     */
    default Envelope getEnvelope(SimpleFeatureSource source) throws IOException {
        SimpleFeatureCollection features = source.getFeatures();
        if (features == null || features.isEmpty()) {
            return null;
        }
        return features.getBounds();
    }
}
