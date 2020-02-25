/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.api.InvalidParameterValueException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.stereotype.Component;

@Component
public class ChangesetIndexProvider {

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    private static final Logger LOGGER = Logging.getLogger(ChangesetIndexProvider.class);

    public static final String INITIAL_STATE = "Initial";
    public static final String CHECKPOINT = "checkpoint";
    public static final String FOOTPRINT = "footprint";
    public static final String TIMESTAMP = "timestamp";

    private final DataStore checkpointIndex;

    public ChangesetIndexProvider(GeoServerDataDirectory dd, Catalog catalog) throws IOException {
        this.checkpointIndex = getCheckpointDataStore(dd);
        catalog.addListener(new IndexCatalogListener());
    }

    DataStore getCheckpointDataStore(GeoServerDataDirectory dd) throws IOException {
        // see if there is a configuration file
        Resource properties = dd.get("changeset-store.properties");
        if (properties.getType() == Resource.Type.RESOURCE) {
            Properties p = new Properties();
            try (InputStream is = properties.in()) {
                p.load(is);
            }

            return DataStoreFinder.getDataStore(p);
        } else {
            // go and create a simple H2 database for local usage

            // make sure we have the directory
            Resource changesetDir = dd.get("changeset");
            if (changesetDir.getType() == Resource.Type.UNDEFINED) {
                changesetDir.dir();
            }

            // create the index
            Map<String, Object> params = new HashMap<>();
            params.put("dbtype", "h2");
            params.put("database", new File(changesetDir.dir(), "index").getAbsolutePath());
            H2DataStoreFactory factory = new H2DataStoreFactory();
            return factory.createDataStore(params);
        }
    }

    /**
     * Looks up the feature type associated to the coverage. Uses the identifier, rather than the
     * name, as it's stable across renames and workspace moves
     */
    SimpleFeatureStore getStoreForCoverage(CoverageInfo ci, boolean createIfMissing)
            throws IOException {
        String typeName = ci.getId();
        if (!Arrays.asList(checkpointIndex.getTypeNames()).contains(typeName)) {
            if (createIfMissing) {
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.add(CHECKPOINT, String.class);
                tb.add(TIMESTAMP, Timestamp.class);
                tb.add(FOOTPRINT, MultiPolygon.class, ci.getCRS());
                tb.setName(typeName);
                SimpleFeatureType type = tb.buildFeatureType();
                checkpointIndex.createSchema(type);
            } else {
                return null;
            }
        }

        return (SimpleFeatureStore) checkpointIndex.getFeatureSource(typeName);
    }

    void addCheckpoint(CoverageInfo ci, SimpleFeature feature) throws IOException {
        SimpleFeatureStore store = getStoreForCoverage(ci, true);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(store.getSchema());
        String checkpoint = UUID.randomUUID().toString();
        fb.set(CHECKPOINT, checkpoint);
        fb.set(TIMESTAMP, new Timestamp(System.currentTimeMillis()));
        fb.set(FOOTPRINT, getFootprint(feature));
        SimpleFeature checkPointFeature = fb.buildFeature(null);
        store.addFeatures(DataUtilities.collection(checkPointFeature));
    }

    private Geometry getFootprint(SimpleFeature feature) {
        Geometry featureGeometry = (Geometry) feature.getDefaultGeometry();
        if (featureGeometry instanceof MultiPolygon) {
            return featureGeometry;
        } else if (featureGeometry instanceof Polygon) {
            return featureGeometry
                    .getFactory()
                    .createMultiPolygon(new Polygon[] {(Polygon) featureGeometry});
        } else {
            throw new IllegalArgumentException(
                    "Unexpected geometry (type) from checkpoint: " + featureGeometry);
        }
    }

    /**
     * Returns the list of checkpoint featues for the given coverage, checkpoint and spatial filter.
     * Will throw an {@link org.geoserver.api.APIException} if the checkpoint is not known to the
     * server.
     *
     * @param ci Returns the modified areas for this coverage info
     * @param checkpoint The reference checkpoint from which to start
     * @param spatialFilter The eventual spatial filter to consider selecting the modified areas
     * @return The list of modifications, or null if nothing changed
     */
    public SimpleFeatureCollection getModifiedAreas(
            CoverageInfo ci, String checkpoint, Filter spatialFilter) throws IOException {
        SimpleFeatureStore store = getStoreForCoverage(ci, false);
        // if no changes recorded yet, return everything
        if (store == null) {
            return null;
        }

        // make sure
        if (spatialFilter != null) {
            ReprojectingFilterVisitor visitor =
                    new ReprojectingFilterVisitor(FF, store.getSchema());
            spatialFilter = (Filter) spatialFilter.accept(visitor, null);
        }

        if (INITIAL_STATE.equals(checkpoint)) {
            return store.getFeatures(spatialFilter);
        }

        // get the time for the reference checkpoint
        Timestamp reference = getTimestampForCheckpoint(store, checkpoint);

        // return all features that are after the checkpoint, and in the desired area
        Query q = new Query();
        PropertyIsGreaterThan timeFilter =
                FF.greater(FF.property(TIMESTAMP), FF.literal(reference));
        if (spatialFilter != Filter.INCLUDE) {
            q.setFilter(FF.and(timeFilter, spatialFilter));
        } else {
            q.setFilter(timeFilter);
        }
        q.setSortBy(new SortBy[] {FF.sort(TIMESTAMP, SortOrder.ASCENDING)});
        return store.getFeatures(q);
    }

    private Timestamp getTimestampForCheckpoint(SimpleFeatureStore store, String checkpoint)
            throws IOException {
        SimpleFeatureCollection fc =
                store.getFeatures(FF.equals(FF.property(CHECKPOINT), FF.literal(checkpoint)));
        SimpleFeature first = DataUtilities.first(fc);
        if (first == null) {
            throw new InvalidParameterValueException(
                    "Checkpoint " + checkpoint + " cannot be found in change history");
        }

        return (Timestamp) first.getAttribute(TIMESTAMP);
    }

    public String getLatestCheckpoint(CoverageInfo ci) throws IOException {
        SimpleFeatureStore store = getStoreForCoverage(ci, false);
        if (store == null) {
            return INITIAL_STATE;
        }

        Query q = new Query(store.getName().getLocalPart());
        q.setSortBy(new SortBy[] {FF.sort(TIMESTAMP, SortOrder.DESCENDING)});
        q.setMaxFeatures(1);
        SimpleFeatureCollection fc = store.getFeatures(q);
        SimpleFeature latestCheckpoint = DataUtilities.first(fc);
        if (latestCheckpoint == null) {
            return INITIAL_STATE;
        } else {
            return (String) latestCheckpoint.getAttribute(CHECKPOINT);
        }
    }

    /** Drops the index tables when a store is removed */
    private class IndexCatalogListener implements CatalogListener {
        @Override
        public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
            if (event.getSource() instanceof CoverageInfo) {
                String typeName = event.getSource().getId();
                try {
                    if (Arrays.asList(checkpointIndex.getTypeNames()).contains(typeName)) {
                        checkpointIndex.removeSchema(typeName);
                    }
                } catch (IOException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Coverage store "
                                    + event.getSource()
                                    + " has been removed, could not remove the corresponding index table named "
                                    + typeName,
                            e);
                }
            }
        }

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {}

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

        @Override
        public void reloaded() {}
    }
}
