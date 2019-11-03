/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Base for formats that have a DataStore implementation.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DataStoreFormat extends VectorFormat {

    private static final Logger LOGGER = Logging.getLogger(DataStoreFormat.class);

    private static final long serialVersionUID = 1L;

    private Class<? extends DataStoreFactorySpi> dataStoreFactoryClass;
    private transient volatile DataStoreFactorySpi dataStoreFactory;

    public DataStoreFormat(Class<? extends DataStoreFactorySpi> dataStoreFactoryClass) {
        this.dataStoreFactoryClass = dataStoreFactoryClass;
    }

    public DataStoreFormat(DataStoreFactorySpi dataStoreFactory) {
        this(dataStoreFactory.getClass());
        this.dataStoreFactory = dataStoreFactory;
    }

    @Override
    public String getName() {
        return factory().getDisplayName();
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        DataStore store = createDataStore(data);
        try {
            return store != null;
        } finally {
            if (store != null) {
                store.dispose();
            }
        }
    }

    public DataStoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        Map<String, Serializable> params = createConnectionParameters(data, catalog);
        if (params == null) {
            return null;
        }

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(workspace);
        DataStoreInfo store = cb.buildDataStore(data.getName());
        DataStoreFactorySpi factory = factory();
        if (store.getName() == null) {
            store.setName(factory.getDisplayName());
        }
        store.setType(factory().getDisplayName());
        store.getConnectionParameters().putAll(params);
        return store;
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {
        DataStore dataStore = createDataStore(data);
        try {
            CatalogBuilder cb = new CatalogBuilder(catalog);

            // create a dummy datastore
            DataStoreInfo store = cb.buildDataStore("dummy");
            cb.setStore(store);

            List<ImportTask> tasks = new ArrayList<ImportTask>();
            for (String typeName : dataStore.getTypeNames()) {
                if (monitor.isCanceled()) {
                    break;
                }
                monitor.setTask("Processing " + typeName);

                // warning - this will log a scary exception if SRS cannot be found
                try {
                    FeatureTypeInfo featureType =
                            cb.buildFeatureType(dataStore.getFeatureSource(typeName));
                    featureType.setStore(null);
                    featureType.setNamespace(null);

                    SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

                    // Defer bounds calculation
                    featureType.setNativeBoundingBox(EMPTY_BOUNDS);
                    featureType.setLatLonBoundingBox(EMPTY_BOUNDS);
                    featureType.getMetadata().put("recalculate-bounds", Boolean.TRUE);

                    // add attributes
                    CatalogFactory factory = catalog.getFactory();
                    SimpleFeatureType schema = featureSource.getSchema();
                    for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
                        AttributeTypeInfo att = factory.createAttribute();
                        att.setName(ad.getLocalName());
                        att.setBinding(ad.getType().getBinding());
                        featureType.getAttributes().add(att);
                    }

                    LayerInfo layer = cb.buildLayer((ResourceInfo) featureType);

                    ImportTask task = new ImportTask(data.part(typeName));
                    task.setLayer(layer);
                    task.setFeatureType(schema);

                    tasks.add(task);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error occured loading " + typeName, e);
                }
            }

            return tasks;
        } finally {
            dataStore.dispose();
        }
    }

    private DataStore getDataStore(ImportData data, ImportTask task) throws IOException {
        DataStore dataStore = (DataStore) task.getMetadata().get(DataStore.class);
        if (dataStore == null) {
            dataStore = createDataStore(data);

            // store in order to later dispose
            // TODO: come up with a better scheme for caching the datastore
            task.getMetadata().put(DataStore.class, dataStore);
        }
        return dataStore;
    }

    public FeatureSource getFeatureSource(ImportData data, ImportTask task) throws IOException {
        return getDataStore(data, task).getFeatureSource(task.getOriginalLayerName());
    }

    @Override
    public FeatureReader read(ImportData data, ImportTask task) throws IOException {
        FeatureReader reader =
                getDataStore(data, task)
                        .getFeatureReader(
                                new Query(task.getOriginalLayerName()), Transaction.AUTO_COMMIT);
        return reader;
    }

    @Override
    public void dispose(FeatureReader reader, ImportTask task) throws IOException {
        reader.close();

        if (task.getMetadata().containsKey(DataStore.class)) {
            DataStore dataStore = (DataStore) task.getMetadata().get(DataStore.class);
            dataStore.dispose();
            task.getMetadata().remove(DataStore.class);
        }
    }

    @Override
    public int getFeatureCount(ImportData data, ImportTask task) throws IOException {
        SimpleFeatureSource featureSource =
                getDataStore(data, task).getFeatureSource(task.getOriginalLayerName());
        return featureSource.getCount(Query.ALL);
    }

    public DataStore createDataStore(ImportData data) throws IOException {
        DataStoreFactorySpi dataStoreFactory = factory();

        Map<String, Serializable> params = createConnectionParameters(data, null);
        if (params != null && dataStoreFactory.canProcess(params)) {
            DataStore dataStore = dataStoreFactory.createDataStore(params);
            if (dataStore != null) {
                return dataStore;
            }
        }

        return null;
    }

    public Map<String, Serializable> createConnectionParameters(ImportData data, Catalog catalog)
            throws IOException {
        // try file based
        if (dataStoreFactory instanceof FileDataStoreFactorySpi) {
            File f = null;
            if (data instanceof SpatialFile) {
                f = ((SpatialFile) data).getFile();
            }
            if (data instanceof Directory) {
                f = ((Directory) data).getFile();
            }

            if (f != null) {
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                map.put("url", relativeDataFileURL(URLs.fileToUrl(f).toString(), catalog));
                if (data.getCharsetEncoding() != null) {
                    // @todo this map only work for shapefile
                    map.put("charset", data.getCharsetEncoding());
                }
                return map;
            }
        }

        // try db based
        if (dataStoreFactory instanceof JDBCDataStoreFactory) {
            Database db = null;
            if (data instanceof Database) {
                db = (Database) data;
            }
            if (data instanceof Table) {
                db = ((Table) data).getDatabase();
            }

            if (db != null) {
                return db.getParameters();
            }
        }

        // try non-jdbc db
        Database db = null;
        if (data instanceof Database) {
            db = (Database) data;
        }
        if (data instanceof Table) {
            db = ((Table) data).getDatabase();
        }
        if (db != null) {
            return db.getParameters();
        }

        return null;
    }

    protected DataStoreFactorySpi factory() {
        if (dataStoreFactory == null) {
            synchronized (this) {
                if (dataStoreFactory == null) {
                    try {
                        dataStoreFactory =
                                dataStoreFactoryClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Unable to create instance of: "
                                        + dataStoreFactoryClass.getSimpleName(),
                                e);
                    }
                }
            }
        }
        return dataStoreFactory;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result =
                prime * result
                        + ((dataStoreFactoryClass == null) ? 0 : dataStoreFactoryClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        DataStoreFormat other = (DataStoreFormat) obj;
        if (dataStoreFactoryClass == null) {
            if (other.dataStoreFactoryClass != null) return false;
        } else if (!dataStoreFactoryClass.equals(other.dataStoreFactoryClass)) return false;
        return true;
    }
}
