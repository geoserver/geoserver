/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

/**
 * Helper to write out mosaic index shapefile and properties files.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MosaicIndex {

    static Logger LOGGER = Logging.getLogger(MosaicIndex.class);

    Mosaic mosaic;

    public MosaicIndex(Mosaic mosaic) {
        this.mosaic = mosaic;
    }

    public File getFile() {
        return new File(mosaic.getFile(), mosaic.getName() + ".shp");
    }

    public void delete() throws IOException {
        File[] files =
                mosaic.getFile()
                        .listFiles(
                                (dir, name) -> {
                                    if ("sample_image".equalsIgnoreCase(name)) {
                                        return true;
                                    }

                                    if (!mosaic.getName()
                                            .equalsIgnoreCase(FilenameUtils.getBaseName(name))) {
                                        return false;
                                    }

                                    String ext = FilenameUtils.getExtension(name);
                                    ShpFileType shpFileType = null;
                                    if (ext != null) {
                                        try {
                                            shpFileType = ShpFileType.valueOf(ext.toUpperCase());
                                        } catch (IllegalArgumentException iae) {
                                            // the extension is not matching
                                        }
                                    }
                                    return "properties".equalsIgnoreCase(ext)
                                            || shpFileType != null;
                                });
        if (files != null) {
            for (File f : files) {
                if (!f.delete()) {
                    // throwing exception here caused sporadic test failures on
                    // windows related to file locking but only in the cleanup
                    // method SystemTestData.tearDown
                    LOGGER.warning("unable to delete mosaic file " + f.getAbsolutePath());
                }
            }
        }
    }

    public void write() throws IOException {
        // delete if already exists
        delete();

        Collection<Granule> granules = mosaic.granules();
        if (granules.isEmpty()) {
            LOGGER.warning("No granules in mosaic, nothing to write");
            return;
        }

        Granule first =
                Iterators.find(
                        granules.iterator(),
                        new Predicate<Granule>() {
                            @Override
                            public boolean apply(Granule input) {
                                return input.getEnvelope() != null
                                        && input.getEnvelope().getCoordinateReferenceSystem()
                                                != null;
                            }
                        });
        if (first == null) {
            throw new IOException("Unable to determine CRS for mosaic");
        }

        Envelope2D envelope = new Envelope2D(first.getEnvelope());

        // create index schema
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(mosaic.getName());
        typeBuilder.setCRS(envelope.getCoordinateReferenceSystem());
        typeBuilder.add("the_geom", Polygon.class);
        typeBuilder.add("location", String.class);

        if (mosaic.getTimeMode() != TimeMode.NONE) {
            typeBuilder.add("time", Date.class);
        }

        // tell image mosaic to use the index file we are creating
        File indexerFile = new File(mosaic.getFile(), "indexer.properties");
        Properties indexer = new Properties();
        indexer.put(Utils.Prop.NAME, mosaic.getName());
        indexer.put(Utils.Prop.INDEX_NAME, mosaic.getName());
        indexer.put(Utils.Prop.USE_EXISTING_SCHEMA, "true");
        try (FileOutputStream ifos = new FileOutputStream(indexerFile)) {
            indexer.store(ifos, null);
        }

        // create a new shapefile feature store
        ShapefileDataStoreFactory shpFactory = new ShapefileDataStoreFactory();
        DirectoryDataStore dir =
                new DirectoryDataStore(
                        mosaic.getFile(),
                        new ShapefileDataStoreFactory.ShpFileStoreFactory(
                                shpFactory, new HashMap()));

        try {
            dir.createSchema(typeBuilder.buildFeatureType());

            FeatureWriter<SimpleFeatureType, SimpleFeature> w =
                    dir.getFeatureWriterAppend(mosaic.getName(), Transaction.AUTO_COMMIT);

            try {
                for (Granule g : mosaic.granules()) {
                    if (g.getEnvelope() == null) {
                        LOGGER.warning(
                                "Skipping " + g.getFile().getAbsolutePath() + ", no envelope");
                    }

                    SimpleFeature f = w.next();
                    f.setDefaultGeometry(JTS.toGeometry((BoundingBox) g.getEnvelope()));
                    f.setAttribute("location", g.getFile().getName());
                    if (mosaic.getTimeMode() != TimeMode.NONE) {
                        f.setAttribute("time", g.getTimestamp());
                    }
                    w.write();

                    // track total bounds
                    envelope.include(g.getEnvelope());
                }

            } finally {
                w.close();
            }
        } finally {
            dir.dispose();
        }

        // have the image mosaic write the property file
        ImageMosaicFormat format = new ImageMosaicFormat();
        ImageMosaicReader reader = format.getReader(mosaic.getFile());
        reader.dispose();

        // if we have to add the time, do so now
        if (mosaic.getTimeMode() != TimeMode.NONE) {
            File propertyFile = new File(mosaic.getFile(), mosaic.getName() + ".properties");
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(propertyFile)) {
                props.load(fis);
            }
            props.setProperty("TimeAttribute", "time");
            try (FileOutputStream fos = new FileOutputStream(propertyFile)) {
                props.store(fos, null);
            }
        }
    }
}
