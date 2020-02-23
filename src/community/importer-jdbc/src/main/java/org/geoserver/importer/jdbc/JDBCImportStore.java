/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import static org.geoserver.importer.jdbc.ImportContextMapper.CTX_FEATURE_TYPE;
import static org.geoserver.importer.jdbc.ImportContextMapper.STATE;
import static org.geoserver.importer.jdbc.ImportContextMapper.USER;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportStore;
import org.geoserver.importer.Importer;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.jdbc.Index;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class JDBCImportStore implements ImportStore {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory2();
    static final Logger LOGGER = Logging.getLogger(JDBCImportStore.class);

    /** The datastore backing the import context storage */
    JDBCDataStore backingStore;
    /** The actual type name, some stores might mangle the name, change case, and the like */
    private String actualTypeName;
    /** Remembers if the expected and actual feature types need mapping between each other */
    private boolean needsMapping;
    /** Maps between ImportContext objects and GeoTools features */
    private final ImportContextMapper mapper;

    /** Used by Spring */
    public JDBCImportStore(JDBCImportStoreLoader loader, Importer importer) {
        this.mapper = new ImportContextMapper(importer);
        this.backingStore = loader.getStore();
    }

    /** Used mostly for tests */
    JDBCImportStore(JDBCDataStore backingStore, Importer importer) {
        this.mapper = new ImportContextMapper(importer);
        this.backingStore = backingStore;
    }

    @Override
    public String getName() {
        return "JDBC";
    }

    @Override
    public void init() {
        if (backingStore == null) {
            throw new IllegalStateException(
                    "Cannot initialize the import store, no backing store has been setup yet");
        }

        try {
            // lookup the store, considering it might not respect case
            SimpleFeatureType statusSchema = lookupStatusSchema();
            if (statusSchema == null) {
                backingStore.createSchema(CTX_FEATURE_TYPE);
            }
            statusSchema = lookupStatusSchema();
            this.actualTypeName = statusSchema.getTypeName();
            this.needsMapping = DataUtilities.compare(statusSchema, CTX_FEATURE_TYPE) != 0;

            // try creating indexes, not a big deal if that does not work
            try {
                backingStore.createIndex(
                        new Index(actualTypeName, actualTypeName + "_user", false, "user"));
                backingStore.createIndex(
                        new Index(actualTypeName, actualTypeName + "_state", false, "state"));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to create database indexes", e);
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    private SimpleFeatureType lookupStatusSchema() throws IOException {
        String[] typeNames = backingStore.getTypeNames();
        for (String typeName : typeNames) {
            if (typeName.equalsIgnoreCase(CTX_FEATURE_TYPE.getTypeName())) {
                return backingStore.getSchema(typeName);
            }
        }
        return null;
    }

    private SimpleFeatureStore getBackingFeatureStore() throws IOException {
        SimpleFeatureStore featureStore =
                (SimpleFeatureStore) backingStore.getFeatureSource(actualTypeName);

        if (needsMapping) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        return featureStore;
    }

    @Override
    public Long advanceId(Long id) {
        // bizarre functionality, does not seem even documented in the user guide...
        return null;
    }

    @Override
    public ImportContext get(long id) {
        try {
            Id filter =
                    FF.id(
                            Collections.singleton(
                                    FF.featureId(CTX_FEATURE_TYPE.getTypeName() + "." + id)));
            SimpleFeatureCollection fc = getBackingFeatureStore().getFeatures(filter);
            SimpleFeature feature = DataUtilities.first(fc);
            if (feature == null) {
                return null;
            }
            return mapper.toContext(feature);
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while looking up context", e);
        }
    }

    @Override
    public void add(ImportContext context) {
        try {
            SimpleFeature feature = mapper.toFeature(context);
            List<FeatureId> ids =
                    getBackingFeatureStore().addFeatures(DataUtilities.collection(feature));
            if (ids == null || ids.isEmpty()) {
                throw new ServiceException("No identifiers returned after insertion");
            }
            Long id = mapper.getContextId(ids.iterator().next().getID());
            context.setId(id);
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while saving context: " + context, e);
        }
    }

    @Override
    public void save(ImportContext context) {
        try {
            if (context.getId() == null) {
                add(context);
            } else {
                SimpleFeature feature = mapper.toFeature(context);
                Id filter = FF.id(Collections.singleton(FF.featureId(feature.getID())));
                String[] names =
                        CTX_FEATURE_TYPE
                                .getAttributeDescriptors()
                                .stream()
                                .map(ad -> ad.getLocalName())
                                .toArray(n -> new String[n]);
                Object[] values = feature.getAttributes().toArray();
                getBackingFeatureStore().modifyFeatures(names, values, filter);
            }
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while saving context: " + context, e);
        }
    }

    @Override
    public void remove(ImportContext importContext) {
        try {
            Id filter =
                    FF.id(
                            Collections.singleton(
                                    FF.featureId(
                                            CTX_FEATURE_TYPE.getTypeName()
                                                    + "."
                                                    + importContext.getId())));
            getBackingFeatureStore().removeFeatures(filter);
        } catch (IOException e) {
            throw new ServiceException(
                    "Unexpected exception while removing context: " + importContext, e);
        }
    }

    @Override
    public void removeAll() {
        try {
            getBackingFeatureStore().removeFeatures(Filter.INCLUDE);
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while removing all context", e);
        }
    }

    @Override
    public Iterator<ImportContext> iterator() {
        try {
            Query q = new Query();
            q.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});
            SimpleFeatureCollection fc = getBackingFeatureStore().getFeatures(q);
            return new MappingIterator(fc.features(), mapper);
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while setting up iteration", e);
        }
    }

    @Override
    public Iterator<ImportContext> iterator(String sortBy) {
        try {
            Query q = new Query();
            q.setSortBy(new SortBy[] {FF.sort(sortBy, SortOrder.ASCENDING)});
            SimpleFeatureCollection fc = getBackingFeatureStore().getFeatures(q);
            return new MappingIterator(fc.features(), mapper);
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while setting up iteration", e);
        }
    }

    @Override
    public Iterator<ImportContext> allNonCompleteImports() {
        try {
            Filter filter =
                    FF.notEqual(
                            FF.property(STATE), FF.literal(ImportContext.State.COMPLETE.name()));
            Query q = new Query();
            q.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});
            q.setFilter(filter);
            SimpleFeatureCollection fc = getBackingFeatureStore().getFeatures(q);
            return new MappingIterator(fc.features(), mapper);
        } catch (IOException e) {
            throw new ServiceException(
                    "Unexpected exception while looking up incomplete imports", e);
        }
    }

    @Override
    public Iterator<ImportContext> importsByUser(String user) {
        try {
            Filter filter = FF.equal(FF.property(USER), FF.literal(user), true);
            Query q = new Query();
            q.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});
            q.setFilter(filter);
            SimpleFeatureCollection fc = getBackingFeatureStore().getFeatures(q);
            return new MappingIterator(fc.features(), mapper);
        } catch (IOException e) {
            throw new ServiceException(
                    "Unexpected exception while looking up imports by user: " + user, e);
        }
    }

    @Override
    public void query(ImportVisitor visitor) {
        try {
            Query q = new Query();
            q.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});
            SimpleFeatureCollection fc = getBackingFeatureStore().getFeatures(q);
            fc.accepts(
                    new FeatureVisitor() {
                        @Override
                        public void visit(Feature feature) {
                            ImportContext context = mapper.toContext((SimpleFeature) feature);
                            visitor.visit(context);
                        }
                    },
                    null);
        } catch (IOException e) {
            throw new ServiceException("Unexpected exception while looking up context", e);
        }
    }

    @Override
    public void destroy() {
        if (backingStore != null) {
            backingStore.dispose();
            backingStore = null;
        }
    }
}
