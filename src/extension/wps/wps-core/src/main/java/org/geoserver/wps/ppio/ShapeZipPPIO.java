/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.response.ShapeZipOutputFormat;
import org.geoserver.wps.resource.ShapefileResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;

/**
 * Handles input and output of feature collections as zipped shapefiles
 *
 * @author Andrea Aime - OpenGeo
 */
public class ShapeZipPPIO extends BinaryPPIO {

    private static final Logger LOGGER = Logging.getLogger(ShapeZipPPIO.class);

    private final GeoServer gs;
    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;
    WPSResourceManager resources;

    protected ShapeZipPPIO(
            WPSResourceManager resources,
            GeoServer gs,
            Catalog catalog,
            GeoServerResourceLoader resourceLoader) {
        super(FeatureCollection.class, FeatureCollection.class, "application/zip");
        this.resources = resources;
        this.gs = gs;
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) value;
        ShapeZipOutputFormat of = new ShapeZipOutputFormat(gs, catalog, resourceLoader);
        of.write(Collections.singletonList(fc), getCharset(), os, null);
    }

    private Charset getCharset() {
        final String charsetName =
                GeoServerExtensions.getProperty(
                        ShapeZipOutputFormat.GS_SHAPEFILE_CHARSET, (ServletContext) null);
        if (charsetName != null) {
            return Charset.forName(charsetName);
        } else {
            // if not specified let's use the shapefile default one
            return StandardCharsets.ISO_8859_1;
        }
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // create the temp directory and register it as a temporary resource
        File tempDir = IOUtils.createTempDirectory("shpziptemp");
        ShapefileResource resource = null;
        try {
            // unzip to the temporary directory
            File shapeFile = null;
            try (ZipInputStream zis = new ZipInputStream(input)) {
                ZipEntry entry = null;
                while ((entry = zis.getNextEntry()) != null) {
                    File file = IOUtils.getZipOutputFile(tempDir, entry);
                    if (entry.isDirectory()) {
                        file.mkdir();
                    } else {
                        if (file.getName().toLowerCase().endsWith(".shp")) {
                            shapeFile = file;
                        }
                        FileUtils.copyToFile(zis, file);
                    }
                    zis.closeEntry();
                }
            }

            if (shapeFile == null) {
                throw new IOException(
                        "Could not find any file with .shp extension in the zip file");
            }
            ShapefileDataStore store = new ShapefileDataStore(URLs.fileToUrl(shapeFile));
            resource = new ShapefileResource(store, tempDir);
            return store.getFeatureSource().getFeatures();
        } finally {
            if (resource != null) {
                resources.addResource(resource);
            } else {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException e) {
                    LOGGER.warning(
                            "Could not delete temp directory: "
                                    + tempDir.getAbsolutePath()
                                    + " due to: "
                                    + e.getMessage());
                }
            }
        }
    }

    @Override
    public String getFileExtension() {
        return "zip";
    }
}
