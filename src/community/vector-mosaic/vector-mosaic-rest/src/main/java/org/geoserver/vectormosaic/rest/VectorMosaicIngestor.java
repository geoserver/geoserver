/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vectormosaic.rest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import org.geoserver.rest.catalog.VectorGranuleIngestionMetadata;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.data.DataUtilities;
import org.geotools.data.util.PropertiesTransformer;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.gce.imagemosaic.properties.PropertiesCollector;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Ingestor responsible for adding VectorMosaic granules to the underlying index datastore.
 *
 * <p>A {@code VectorMosaicHarvester} receives {@link VectorGranuleIngestionMetadata}, builds the corresponding index
 * feature (geometry, connection parameters, and any configured property-collector fields), and inserts it into the
 * mosaic index layer.
 */
public class VectorMosaicIngestor {

    private final DataStore indexStore;
    private final String indexTypeName;
    private final String connectionParameterKey;
    private final Properties commonParameters;

    public VectorMosaicIngestor(
            DataStore indexStore, String indexTypeName, String connectionParameterKey, Properties commonParameters) {
        this.indexStore = indexStore;
        this.indexTypeName = indexTypeName;
        this.connectionParameterKey = connectionParameterKey;
        this.commonParameters = commonParameters;
    }

    /**
     * Inserts a granule into the vector mosaic index by creating and populating an index feature from the provided
     * ingestion metadata.
     *
     * @param granule the ingestion granule metadata to index
     * @throws IOException if the index schema cannot be accessed or insertion fails
     */
    public void ingest(VectorGranuleIngestionMetadata granule) throws IOException {
        URI resourceUri = granule.getUri();
        SimpleFeatureType schema = indexStore.getSchema(indexTypeName);
        if (schema == null) {
            throw new IOException("Index schema '" + indexTypeName + "' not found");
        }

        Name name = new NameImpl(indexTypeName);
        SimpleFeatureStore store = (SimpleFeatureStore) indexStore.getFeatureSource(name);

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);

        // 1. Geometry
        GeometryDescriptor gd = null;
        Envelope envelope = granule.getFootprint();
        if ((gd = schema.getGeometryDescriptor()) != null && envelope != null) {
            Geometry geom = JTS.toGeometry(envelope);
            fb.set(gd.getLocalName(), geom);
        }
        // Connection params
        if (schema.getDescriptor(connectionParameterKey) != null) {
            fb.set(connectionParameterKey, PropertiesTransformer.propertiesToString(granule.getParams()));
        }

        SimpleFeature feature = fb.buildFeature(null);
        PropertiesCollectorParser parser = new PropertiesCollectorParser();
        List<PropertiesCollector> pcs = parser.parse(commonParameters);

        for (PropertiesCollector pc : pcs) {
            pc.collect(resourceUri).setProperties(feature);
            pc.reset();
        }

        store.addFeatures(DataUtilities.collection(feature));
    }
}
