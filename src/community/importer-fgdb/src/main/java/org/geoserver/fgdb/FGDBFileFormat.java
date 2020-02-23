/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fgdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.ogr.OGR;
import org.geotools.data.ogr.OGRDataStore;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.geotools.data.ogr.jni.JniOGR;
import org.geotools.data.ogr.jni.JniOGRDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Custom VectorFormat for GDBDataStoreFactory restricting OGR Driver to FileGDB.
 *
 * @author Jody Garnett
 */
public class FGDBFileFormat extends VectorFormat {
    private static final Logger LOGGER = Logging.getLogger(FGDBFileFormat.class);

    /** Factory wrapper */
    static final OGRDataStoreFactory factory = new JniOGRDataStoreFactory();

    private static final String DRIVER = "FileGDB";

    private static final String FILE_TYPE = "gdb";

    @SuppressWarnings("rawtypes")
    @Override
    public FeatureReader read(ImportData data, ImportTask item) throws IOException {
        // final SimpleFeatureType featureType = (SimpleFeatureType)
        // item.getMetadata().get(FeatureType.class);
        final FeatureSource source = (FeatureSource) item.getMetadata().get(FeatureSource.class);
        Name name = source.getName();

        Query query = new Query(name.getLocalPart(), Filter.INCLUDE);
        DataStore store = (DataStore) source.getDataStore();
        return store.getFeatureReader(query, Transaction.AUTO_COMMIT);
    }

    @Override
    public void dispose(FeatureReader reader, ImportTask item) throws IOException {
        reader.close();
    }

    @Override
    public int getFeatureCount(ImportData data, ImportTask item) throws IOException {
        return -1;
    }

    @Override
    public String getName() {
        return "FileGDB";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        if (factory.isAvailable()) {
            if (data instanceof FileData) {
                FileData fileData = (FileData) data;
                File file = fileData.getFile();
                return file.getName().equalsIgnoreCase("gdb");
            }
        }
        return false;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        return null; // Direct use not supported, import to default workspace store
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {
        List<ImportTask> tasks = new ArrayList<ImportTask>();
        monitor.started();
        if (data instanceof FileData) {
            FileData fileData = (FileData) data;
            File file = fileData.getFile();

            OGR ogr = new JniOGR();
            if (file.getName().equalsIgnoreCase(FILE_TYPE)) {
                file = file.getParentFile();
            }

            DataStore store = new OGRDataStore(file.getAbsolutePath(), DRIVER, null, ogr);

            List<Name> names = store.getNames();
            for (Name name : names) {
                try {
                    FeatureSource source = store.getFeatureSource(name);
                    ImportTask task = task(data, catalog, source);
                    if (task != null) {
                        tasks.add(task);
                    }
                } catch (Exception issue) {
                    LOGGER.log(
                            Level.WARNING,
                            "Unable to access " + name + " for import:" + issue,
                            issue);
                }
            }
        }
        return tasks;
    }
    /**
     * Load a single table from FileGDB.
     *
     * @param data OGRDataStore
     * @return import task for aynchronous loading
     */
    private ImportTask task(
            ImportData data, Catalog catalog, FeatureSource<FeatureType, Feature> source)
            throws IOException {
        //
        // Connect and confirm data avaialbility
        //
        Name name = source.getName();
        FeatureType gdbType = source.getSchema();
        LOGGER.info("Importing " + gdbType);
        //
        // FEATURE TYPE INFO
        //
        FeatureTypeInfo featureTypeInfo = catalog.getFactory().createFeatureType();
        featureTypeInfo.setName(name.getLocalPart());
        featureTypeInfo.setNativeName(featureTypeInfo.getName());

        // determine spatial reference system
        CoordinateReferenceSystem nativeCRS = null;
        if (gdbType != null && gdbType.getCoordinateReferenceSystem() != null) {
            nativeCRS = gdbType.getCoordinateReferenceSystem();
        }
        if (nativeCRS == null) {
            try {

                nativeCRS = CRS.decode("EPSG:4326");
            } catch (Exception e) {
                throw new IOException("Unable to set " + name + " native CRS to EPSG:4326", e);
            }
        }
        featureTypeInfo.setNativeCRS(nativeCRS);

        Integer code = null;
        try {
            code = CRS.lookupEpsgCode(nativeCRS, false);
            if (code == null) {
                code = CRS.lookupEpsgCode(nativeCRS, true);
            }
            featureTypeInfo.setSRS("EPSG:" + code);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Error looking up epsg code", e);
        }

        ReferencedEnvelope gdbBounds = source.getBounds();
        if (gdbBounds == null) {
            gdbBounds = source.getFeatures().getBounds();
        }
        featureTypeInfo.setNativeBoundingBox(gdbBounds);
        try {
            ReferencedEnvelope latLon = gdbBounds.transform(DefaultGeographicCRS.WGS84, true);
            featureTypeInfo.setLatLonBoundingBox(latLon);
        } catch (Exception ignore) {
            LOGGER.log(Level.FINER, "Error transforming to latlon bounds", ignore);
        }
        //
        // LAYER INFO
        //
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(featureTypeInfo);

        ImportTask task = new ImportTask(data);
        task.setLayer(layer);

        task.getMetadata().put(FeatureType.class, gdbType);
        task.getMetadata().put(Name.class, name);
        task.getMetadata().put(FeatureSource.class, source);

        return task;
    }
}
