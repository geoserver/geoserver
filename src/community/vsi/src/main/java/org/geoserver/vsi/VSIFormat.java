/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import it.geosolutions.imageio.plugins.vrt.VRTImageReaderSpi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogRepository;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.wicket.browser.FileRootsFinder;
import org.geotools.coverageio.gdal.BaseGDALGridFormat;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;

/**
 * Wrapper around BaseGDALGridFormat defining the VRTFile format which uses VRTReader from the
 * GeoServer's GDAL extension
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public final class VSIFormat extends BaseGDALGridFormat implements Format {

    public static final String[] VSI_TYPES = {
        "zip",
        "gzip",
        "tar",
        "stdin",
        "stdout",
        "mem",
        "subfile",
        "sparse",
        "crypt",
        "curl",
        "curl_strreaming",
        "s3",
        "s3_streaming",
        "gs",
        "gs_streaming",
        "az",
        "az_streaming",
        "adls",
        "oss",
        "oss_streaming",
        "swift",
        "swift_streaming",
        "hdfs",
        "webhdfs"
    };

    protected static final String NAME = "VSI Virtual File System";
    protected static final String DESCRIPTION =
            "Raster data accessible via a virtual file system supported by GDAL";

    private static final Logger LOGGER = Logger.getLogger(VSIFormat.class.getName());
    private static final String VSI_PATTERN =
            String.format(
                    "^(/vsi(%s)/)+[\\w\\d_\\-\\.]+/[\\w\\d_\\-\\./]+$",
                    String.join("|", Arrays.asList(VSI_TYPES)));

    public VSIFormat() {
        super(new VRTImageReaderSpi());

        setInfo();

        try {
            VSIProperties.sync();
        } catch (IOException ex) {
            LOGGER.debug(ex.getMessage());
        }
    }

    @Override
    protected void setInfo() {
        setInfo(new InfoWrapper(DESCRIPTION, NAME));
    }

    /**
     * Fetch a Reader object compatible with this resource type
     *
     * @param source Input path representing the location of the virtual file system
     * @param hints Hints object if available
     */
    @Override
    public VSIReader getReader(Object source, Hints hints) {

        String path = null;

        if (source instanceof String) {
            path = (String) source;
        } else if (source instanceof File) {
            path = ((File) source).toString();
        }

        if (!acceptsPath(path)) {
            throw new RuntimeException("Source object is not a valid path: " + path);
        }

        // Add '/' where appropriate to make path valid to GDAL
        path = fixPath(path);

        VRTFile vrt = null;
        final Catalog catalog = ((CatalogRepository) hints.get(Hints.REPOSITORY)).getCatalog();
        final String dataDir = new FileRootsFinder(false, true).getDataDirectory().toString();
        final String fileName = path.substring(path.lastIndexOf("/") + 1);

        // Find an existing VRT file
        for (StoreInfo storeInfo : catalog.getStores(StoreInfo.class)) {
            if (NAME.equals(storeInfo.getType())) {
                File file =
                        Paths.get(
                                        dataDir,
                                        "workspaces",
                                        storeInfo.getWorkspace().getName(),
                                        storeInfo.getName(),
                                        fileName)
                                .toFile();

                if (file.exists()) {
                    vrt = new VRTFile(path, storeInfo);
                    break;
                }
            }
        }

        // If no VRT exists, assume this is a newly created store
        if (vrt == null) {
            vrt = new VRTFile(path, VSIState.getStoreInfo());
        }

        return getReaderFromVRTFile(vrt, hints);
    }

    /** Fetch a VSIReader object */
    public VSIReader getReaderFromVRTFile(VRTFile vrt, Hints hints) {
        try {
            return new VSIReader(vrt, hints);
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Verifies a path is parsable by this class
     *
     * @param path String representing a path to a virtual file system
     * @return true if the path is valid input to this class
     */
    public boolean acceptsPath(String path) {
        return path != null && path.matches(VSI_PATTERN);
    }

    /**
     * Amends a file-like path by inserting '/' characters to be parsable by GDAL This method
     * assumes that the input path is corrected formatted and is only missing '/' characters between
     * its preceeding vsi-* filters
     *
     * @param path String representing a path to a virtual file system
     * @return amended String representing a path to a virtual file system which is parsable by GDAL
     */
    protected String fixPath(String path) {
        String[] parts = path.split("/");
        boolean isPrefix = true;

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].startsWith("vsi")) {
                isPrefix = false;
            } else if (isPrefix) {
                parts[i] = "/" + parts[i];
            }
        }

        return String.join("/", parts).substring(1); // omit first '/'
    }

    /**
     * Return true if this class' constructor accepts the given object
     *
     * @param o input object
     * @param hints Hints object if available
     * @return true if this class' constructor accepts the given object
     */
    @Override
    public boolean accepts(Object o, Hints hints) {
        String path = null;

        if (o instanceof String) {
            path = (String) o;
        } else if (o instanceof File) {
            path = ((File) o).toString();
        }

        return path == null ? false : acceptsPath(path);
    }

    /**
     * Return true if this class' constructor accepts the given object
     *
     * @param o input object
     * @return true if this class' constructor accepts the given object
     */
    @Override
    public boolean accepts(Object o) {
        return accepts(o, null);
    }
}
