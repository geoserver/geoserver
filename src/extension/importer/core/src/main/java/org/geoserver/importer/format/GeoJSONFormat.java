/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.format;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.File;
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
import org.geoserver.importer.VectorFormat;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.FeatureReader;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeoJSONFormat extends VectorFormat {

    static Logger LOG = Logging.getLogger(GeoJSONFormat.class);
    private static ReferencedEnvelope EMPTY_BOUNDS = new ReferencedEnvelope();

    @Override
    public FeatureReader read(ImportData data, ImportTask item) throws IOException {
        final SimpleFeatureType featureType = item.getFeatureType();
        FeatureJSON json = new FeatureJSON();
        json.setFeatureType(featureType);
        final FeatureIterator it = json.streamFeatureCollection(file(data, item));

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
            public Feature next()
                    throws IOException, IllegalArgumentException, NoSuchElementException {
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
        try {
            FeatureIterator it = new FeatureJSON().streamFeatureCollection(file);
            try {
                if (it.hasNext()) {
                    return (SimpleFeature) it.next();
                }
            } finally {
                it.close();
            }
        } catch (Exception e) {
            LOG.log(Level.FINER, "Error reading file as json", e);
        }
        return null;
    }

    @Override
    public StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException {
        // direct import not supported
        return null;
    }

    @Override
    public List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException {

        if (data instanceof Directory) {
            List<ImportTask> tasks = new ArrayList<ImportTask>();
            for (FileData file : ((Directory) data).getFiles()) {
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
        SimpleFeatureType featureType = new FeatureJSON().readFeatureCollectionSchema(file, false);
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
        ft.getMetadata().put("recalculate-bounds", Boolean.TRUE);

        LayerInfo layer = catalogBuilder.buildLayer((ResourceInfo) ft);

        ImportTask task = new ImportTask(data);
        task.setLayer(layer);

        task.setFeatureType(featureType);

        return task;
    }

    File file(ImportData data, final ImportTask item) {
        if (data instanceof Directory) {
            return Iterables.find(
                            ((Directory) data).getFiles(),
                            new Predicate<FileData>() {
                                @Override
                                public boolean apply(FileData input) {
                                    return FilenameUtils.getBaseName(input.getFile().getName())
                                            .equals(item.getLayer().getName());
                                }
                            })
                    .getFile();
        } else {
            return maybeFile(data).get();
        }
    }

    Optional<File> maybeFile(ImportData data) {
        if (data instanceof FileData) {
            return Optional.of(((FileData) data).getFile());
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
