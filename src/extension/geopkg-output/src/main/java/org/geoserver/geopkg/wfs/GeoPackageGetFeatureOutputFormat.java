/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wfs;

import static org.geoserver.geopkg.GeoPkg.EXTENSION;
import static org.geoserver.geopkg.GeoPkg.MIME_TYPE;
import static org.geoserver.geopkg.GeoPkg.MIME_TYPES;
import static org.geoserver.geopkg.GeoPkg.NAMES;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.geopkg.GeoPkg;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.springframework.util.StringUtils;

/**
 * WFS GetFeature OutputFormat for GeoPackage
 *
 * @author Niels Charlier
 */
public class GeoPackageGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    protected static Logger LOGGER = Logging.getLogger(GeoPackageGetFeatureOutputFormat.class);

    public static final String PROPERTY_INDEXED = "geopackage.wfs.indexed";
    public static final String CUSTOM_TEMP_DIR_PROPERTY = "geopackage.wfs.tempdir";

    public GeoPackageGetFeatureOutputFormat(GeoServer gs) {
        super(gs, Sets.union(Sets.newHashSet(MIME_TYPES), Sets.newHashSet(NAMES)));
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME_TYPE;
    }

    @Override
    public String getCapabilitiesElementName() {
        return NAMES.iterator().next();
    }

    @Override
    public List<String> getCapabilitiesElementNames() {
        return Lists.newArrayList(NAMES);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return EXTENSION;
    }

    /**
     * This is a smarter version of File.createTempFile().
     *
     * <p>If the CUSTOM_TEMP_DIR_PROPERTY property is set, then use that as the temp directory.
     * Otherwise (unset), use the standard File.createTempFile() temp directory.
     *
     * <p>If the CUSTOM_TEMP_DIR_PROPERTY directory doesn't exist (or isnt a directory), then an
     * exception is thrown
     *
     * @param prefix filename prefix, must be three characters or more
     * @param suffix filename suffix, or if null {@code null} suffix {@code ".tmp") used
     * @return path to newly created temporary file
     */
    File createTempFile(String prefix, String suffix) throws IOException {
        String customTempDir = GeoServerExtensions.getProperty(CUSTOM_TEMP_DIR_PROPERTY);
        if (!StringUtils.hasText(customTempDir)) {
            return File.createTempFile("geopkg", ".tmp.gpkg");
        }
        File tempDir = new File(customTempDir);
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IOException(
                    "GeoPKG output: temp dir: '"
                            + customTempDir
                            + "' doesn't exist or isn't a directory");
        }
        return File.createTempFile("geopkg", ".tmp.gpkg", tempDir);
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        // create the geopackage file and write the features into it.
        // geopackage is written to a temporary file, copied into the outputStream, then the temp
        // file deleted.
        File file = null;
        try {
            file = createTempFile("geopkg", ".tmp.gpkg");

            try (GeoPackage geopkg = GeoPkg.getGeoPackage(file)) {
                for (FeatureCollection collection : featureCollection.getFeatures()) {

                    FeatureEntry e = new FeatureEntry();

                    if (!(collection instanceof SimpleFeatureCollection)) {
                        throw new ServiceException(
                                "GeoPackage OutputFormat does not support Complex Features.");
                    }

                    SimpleFeatureCollection features = (SimpleFeatureCollection) collection;
                    FeatureTypeInfo meta = lookupFeatureType(features);
                    if (meta != null) {
                        // initialize entry metadata
                        e.setIdentifier(meta.getTitle());
                        e.setDescription(abstractOrDescription(meta));
                    }
                    geopkg.add(e, features);

                    if (!"false".equals(System.getProperty(PROPERTY_INDEXED))) {
                        geopkg.createSpatialIndex(e);
                    }
                }
            }

            // write to output and delete temporary file
            try (InputStream temp = new FileInputStream(file)) {
                IOUtils.copy(temp, output);
                output.flush();
            }
        } finally {
            if (file != null) {
                delete(file);
                delete(new File(file.getPath() + "-shm"));
                delete(new File(file.getPath() + "-wal"));
            }
        }
    }

    /** Safely delete of temporary file (if it exists). */
    private void delete(File file) {
        if (file != null && file.exists()) {
            try {
                java.nio.file.Files.delete(file.toPath());
            } catch (IOException ioException) {
                LOGGER.log(
                        Level.FINE,
                        "Unable to delete temporary GeoPackage file " + file.getName(),
                        ioException);
            }
        }
    }

    FeatureTypeInfo lookupFeatureType(SimpleFeatureCollection features) {
        FeatureType featureType = features.getSchema();
        if (featureType != null) {
            Catalog cat = gs.getCatalog();
            FeatureTypeInfo meta = cat.getFeatureTypeByName(featureType.getName());
            if (meta != null) {
                return meta;
            }

            LOGGER.fine("Unable to load feature type metadata for: " + featureType.getName());
        } else {
            LOGGER.fine("No feature type for collection, unable to load metadata");
        }

        return null;
    }

    String abstractOrDescription(FeatureTypeInfo meta) {
        return meta.getAbstract() != null ? meta.getAbstract() : meta.getDescription();
    }
}
