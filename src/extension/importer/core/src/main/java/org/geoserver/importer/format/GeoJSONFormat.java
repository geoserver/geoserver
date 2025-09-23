/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;

public class GeoJSONFormat extends VectorFormat {

    static Logger LOG = Logging.getLogger(GeoJSONFormat.class);
    private static ReferencedEnvelope EMPTY_BOUNDS = new ReferencedEnvelope();

    @Override
    @SuppressWarnings("PMD.CloseResource") // wrapped and returned
    public FeatureReader read(ImportData data, ImportTask item) throws IOException {
        final SimpleFeatureType featureType = item.getFeatureType();
        GeoJSONReader reader = new GeoJSONReader(new FileInputStream(file(data, item)));
        reader.setSchema(featureType);

        final FeatureIterator it = reader.getIterator();
        return new FeatureReader() {

            @Override
            public FeatureType getFeatureType() {
                return featureType;
            }

            @Override
            public boolean hasNext() throws IOException {
                return it.hasNext();
            }

            @Override
            public Feature next() throws IOException, IllegalArgumentException, NoSuchElementException {
                return it.next();
            }

            @Override
            public void close() throws IOException {
                it.close();
            }
        };
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
        return "GeoJSON";
    }

    @Override
    public boolean canRead(ImportData data) throws IOException {
        Optional<File> file = maybeFile(data);
        if (file.isPresent()) {
            return sniff(file.get()) != null;
        }

        return false;
    }

    SimpleFeature sniff(File file) {
        try (FileInputStream fis = new FileInputStream(file);
                FeatureIterator it = new GeoJSONReader(fis).getIterator()) {
            if (it.hasNext()) {
                return (SimpleFeature) it.next();
            }
        } catch (Exception e) {
            LOG.log(Level.FINER, "Error reading file as json", e);
        }
        return null;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog) throws IOException {
        // direct import not supported
        LOG.log(
                Level.INFO,
                "Direct import of GeoJSON is not supported.  " + "You mush select a supported target data store");
        return null;
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor) throws IOException {

        if (data instanceof Directory directory) {
            List<ImportTask> tasks = new ArrayList<>();
            for (FileData file : directory.getFiles()) {
                tasks.add(task(file, catalog));
            }
            return tasks;
        } else {
            return Arrays.asList(task(data, catalog));
        }
    }

    ImportTask task(ImportData data, Catalog catalog) throws IOException {
        File file = maybeFile(data).get();
        CatalogFactory factory = catalog.getFactory();
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);

        // get the composite feature type
        SimpleFeatureType featureType;
        LOG.log(Level.INFO, "Parsing JSON file to get data schema " + file.getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(file);
                GeoJSONReader reader = new GeoJSONReader(fis)) {
            featureType = reader.getFeatures().getSchema();
        }
        LOG.log(Level.FINE, featureType.toString());

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(featureType);
        tb.setName(FilenameUtils.getBaseName(file.getName()));
        featureType = tb.buildFeatureType();

        // create the feature type
        FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
        ft.setName(FilenameUtils.getBaseName(file.getName()));
        ft.setNativeName(ft.getName());

        List<AttributeTypeInfo> attributes = ft.getAttributes();
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            AttributeTypeInfo att = factory.createAttribute();
            att.setName(ad.getLocalName());
            att.setBinding(ad.getType().getBinding());
            attributes.add(att);
        }

        // crs
        CoordinateReferenceSystem crs = null;
        if (featureType != null && featureType.getCoordinateReferenceSystem() != null) {
            crs = featureType.getCoordinateReferenceSystem();
        }
        try {
            crs = crs != null ? crs : CRS.decode("EPSG:4326");
        } catch (Exception e) {
            throw new IOException(e);
        }

        ft.setNativeCRS(crs);

        String srs = srs(crs);
        if (srs != null) {
            ft.setSRS(srs);
        }

        // bounds
        ft.setNativeBoundingBox(EMPTY_BOUNDS);
        ft.setLatLonBoundingBox(EMPTY_BOUNDS);
        ft.getMetadata().put(Importer.CALCULATE_BOUNDS, Boolean.TRUE);

        LayerInfo layer = catalogBuilder.buildLayer((ResourceInfo) ft);

        ImportTask task = new ImportTask(data);
        task.setLayer(layer);

        task.setFeatureType(featureType);

        return task;
    }

    File file(ImportData data, final ImportTask item) {
        if (data instanceof Directory directory) {
            return Iterables.find(directory.getFiles(), input -> FilenameUtils.getBaseName(
                                    input.getFile().getName())
                            .equals(item.getLayer().getName()))
                    .getFile();
        } else {
            return maybeFile(data).get();
        }
    }

    Optional<File> maybeFile(ImportData data) {
        if (data instanceof FileData fileData) {
            return Optional.of(fileData.getFile());
        }
        return Optional.absent();
    }

    String srs(CoordinateReferenceSystem crs) {
        Integer epsg = null;

        try {
            epsg = CRS.lookupEpsgCode(crs, false);
            if (epsg == null) {
                epsg = CRS.lookupEpsgCode(crs, true);
            }
        } catch (Exception e) {
            LOG.log(Level.FINER, "Error looking up epsg code", e);
        }
        return epsg != null ? "EPSG:" + epsg : null;
    }
}
