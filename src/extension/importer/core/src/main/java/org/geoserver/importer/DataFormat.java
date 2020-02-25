/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Represents a type of data and encapsulates I/O operations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class DataFormat implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    static Logger LOG = Logging.getLogger(DataFormat.class);

    /** looks up a format based on file extension. */
    public static DataFormat lookup(File file) {
        FileData fileData = new FileData(file);
        for (DataFormat df : GeoServerExtensions.extensions(DataFormat.class)) {
            try {
                if (df.canRead(fileData)) {
                    return df;
                }
            } catch (IOException e) {
                LOG.log(
                        Level.FINER,
                        String.format(
                                "Error checking if format %s can read file %s, " + df.getName(),
                                file.getPath()),
                        e);
            }
        }

        // look for a datastore that can handle the file
        String ext = FilenameUtils.getExtension(file.getName());
        FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory(ext);
        if (factory != null) {
            return new DataStoreFormat(factory);
        }

        // look for a gridformat that can handle the file
        Set<AbstractGridFormat> formats = GridFormatFinder.findFormats(file);
        AbstractGridFormat format = null;
        // in the case of 2 formats, let's ensure any ambiguity that cannot
        // be resolved is an error to prevent spurious bugs related to
        // the first format that is found being returned (and this can vary
        // to to hashing in the set)
        if (formats.size() > 1) {
            for (AbstractGridFormat f : formats) {
                // prefer GeoTIFF over WorldImageFormat
                if ("GeoTIFF".equals(f.getName())) {
                    format = f;
                    break;
                }
            }
            if (format == null) {
                throw new RuntimeException("multiple formats found but not handled " + formats);
            }
        } else if (formats.size() == 1) {
            format = formats.iterator().next();
        }
        if (format != null && !(format instanceof UnknownFormat)) {
            return new GridFormat(format);
        }

        return null;
    }

    /** Looks up a format based on a set of connection parameters. */
    public static DataFormat lookup(Map<String, Serializable> params) {
        DataStoreFactorySpi factory = (DataStoreFactorySpi) DataStoreUtils.aquireFactory(params);
        if (factory != null) {
            return new DataStoreFormat(factory);
        }
        return null;
    }

    /**
     * Converts an absolute URL to a resource to be relative to the data directory if applicable.
     *
     * @return The relative path, or the original path if it does not contain the data directory
     */
    protected String relativeDataFileURL(String url, Catalog catalog) {
        if (catalog == null) {
            return url;
        }
        File baseDirectory = catalog.getResourceLoader().getBaseDirectory();
        File f = Files.url(baseDirectory, url);

        return f == null ? url : "file:" + Paths.convert(baseDirectory, f);
    }

    public abstract String getName();

    public abstract boolean canRead(ImportData data) throws IOException;

    public abstract StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog)
            throws IOException;

    public abstract List<ImportTask> list(ImportData data, Catalog catalog, ProgressMonitor monitor)
            throws IOException;

    /**
     * Returns a File from the ImportData, assuming the import data itself is a FileData (a class
     * cast exception will happen otherwise)
     */
    protected File getFileFromData(ImportData data) {
        assert data instanceof FileData;
        FileData fileData = (FileData) data;
        File file = fileData.getFile();
        return file;
    }
}
