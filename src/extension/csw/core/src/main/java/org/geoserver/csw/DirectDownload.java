/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.CloseableIterator;
import org.geotools.data.FileGroupProvider;
import org.geotools.data.FileGroupProvider.FileGroup;
import org.geotools.data.FileResourceInfo;
import org.geotools.data.FileServiceInfo;
import org.geotools.data.Query;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.feature.NameImpl;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory2;

/**
 * Runs the DirectDownload request
 *
 * @author Daniele Romagnoli - GeoSolutions
 */
public class DirectDownload {

    private static final FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;

    /**
     * Files collector class which populates a {@link File}s {@link List} by accessing a {@link
     * FileGroupProvider} instance.
     */
    class FilesCollector {

        public FilesCollector(FileGroupProvider fileGroupProvider) {
            this.fileGroupProvider = fileGroupProvider;
        }

        /** The underlying FileGroupProvider used to collect the files */
        private FileGroupProvider fileGroupProvider;

        /**
         * Only collect the subset of files available from the fileGroupProvider, which match the
         * provided fileId.
         *
         * <p>a File Identifier is composed of "hash-baseName". Only the files having same baseName
         * and matching hash will be added to the list
         */
        private void collectSubset(String fileId, List<File> result) {
            CloseableIterator<FileGroup> files = null;
            try {
                String hash = fileId;
                // SHA-1 are 20 bytes in length
                String fileBaseName = hash.substring(41);
                Query query = new Query();

                // Look for files in the catalog having the same base name
                query.setFilter(FF.like(FF.property("location"), "*" + fileBaseName + "*"));
                files = fileGroupProvider.getFiles(query);
                while (files.hasNext()) {
                    FileGroup fileGroup = files.next();
                    File mainFile = fileGroup.getMainFile();
                    String hashedName = DownloadLinkHandler.hashFile(mainFile);

                    // Only files fully matching the current hash will
                    // be added to the download list
                    if (hash.equalsIgnoreCase(hashedName)) {
                        result.add(mainFile);
                        List<File> supportFile = fileGroup.getSupportFiles();
                        if (supportFile != null && !supportFile.isEmpty()) {
                            result.addAll(supportFile);
                        }
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                throw new ServiceException(
                        "Exception occurred while looking for raw files for :" + fileId, e);
            } catch (IOException e) {
                throw new ServiceException(
                        "Exception occurred while looking for raw files for :" + fileId, e);
            } finally {
                closeIterator(files);
            }
        }

        /** Collect all files from the fileGroupProvider */
        void collectFull(List<File> result) {
            CloseableIterator<FileGroup> files = null;
            try {
                files = fileGroupProvider.getFiles(null);
                while (files.hasNext()) {
                    FileGroup fileGroup = files.next();
                    result.add(fileGroup.getMainFile());
                    List<File> supportFile = fileGroup.getSupportFiles();
                    if (supportFile != null && !supportFile.isEmpty()) {
                        result.addAll(supportFile);
                    }
                }
            } finally {
                closeIterator(files);
            }
        }
    }

    private static final int KILO = 1024;

    private static final int MEGA = KILO * KILO;

    private static final int GIGA = MEGA * KILO;

    static final Logger LOGGER = Logging.getLogger(DirectDownload.class);

    static final String LIMIT_MESSAGE = "This request is trying to download too much data. ";

    CSWInfo csw;

    CatalogStore store;

    GeoServer geoserver;

    public DirectDownload(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;
        this.geoserver = csw.getGeoServer();
    }

    /** Prepare the list of files to be downloaded from the current request. */
    public List<File> run(DirectDownloadType request) {
        List<File> result = new ArrayList<File>();
        String resourceId = request.getResourceId();
        String fileId = request.getFile();

        // Extract namespace, layername from the resourceId
        String[] identifiers = resourceId.split(":");
        assert (identifiers.length == 2);
        String nameSpace = identifiers[0];
        String layerName = identifiers[1];

        Name coverageName = new NameImpl(nameSpace, layerName);

        // Get the underlying coverage from the catalog
        CoverageInfo info = geoserver.getCatalog().getCoverageByName(coverageName);
        if (info == null) {
            throw new ServiceException(
                    "No object available for the specified name:" + coverageName);
        }

        // Get the reader to access the coverage
        GridCoverage2DReader reader;
        try {
            reader =
                    (GridCoverage2DReader)
                            info.getGridCoverageReader(null, GeoTools.getDefaultHints());
        } catch (IOException e) {
            throw new ServiceException(
                    "Failed to get a reader for the associated info: " + info, e);
        }

        // Get resources for the specified file
        String name = extractName(info);
        getFileResources(reader, name, fileId, result);

        // Only StructuredGridCoverage2DReader can deal with multiple coverages
        // standard readers return same content for FileInfo and ResourceInfo
        if (fileId == null && reader instanceof StructuredGridCoverage2DReader) {
            // Add the serviceInfo content to the returned files
            // (As an instance, shapefile index, indexers, property files...)
            getExtraFiles(reader, result);
        }
        if (result == null || result.isEmpty()) {
            throw new ServiceException(
                    "Unable to get any data for resourceId=" + resourceId + " and file=" + fileId);
        }
        checkSizeLimit(result, info);
        return result;
    }

    /**
     * Get extra files for the specified reader and add them to the result list. Extra files are
     * usually auxiliary files like, as an instance, indexer, properties, config files for a mosaic.
     */
    private void getExtraFiles(GridCoverage2DReader reader, List<File> result) {
        ServiceInfo info = reader.getInfo();
        if (info instanceof FileServiceInfo) {
            FileServiceInfo fileInfo = (FileServiceInfo) info;
            FileGroupProvider provider = (FileGroupProvider) fileInfo;
            FilesCollector collector = new FilesCollector(provider);
            collector.collectFull(result);
        } else {
            throw new ServiceException(
                    "Unable to get files from the specified ServiceInfo which"
                            + " doesn't implement FileServiceInfo");
        }
    }

    /**
     * Get the data files from the specified {@link GridCoverage2DReader}, related to the provided
     * coverageName, matching the specified fileId and add them to the result list.
     */
    private void getFileResources(
            GridCoverage2DReader reader, String coverageName, String fileId, List<File> result) {
        ResourceInfo resourceInfo = reader.getInfo(coverageName);
        if (resourceInfo instanceof FileResourceInfo) {
            FileResourceInfo fileResourceInfo = (FileResourceInfo) resourceInfo;

            // Get the resource files
            FileGroupProvider fileGroupProvider = (FileGroupProvider) fileResourceInfo;
            FilesCollector collector = new FilesCollector(fileGroupProvider);

            // Only structuredReaders can support multiple coverages
            // Standard readers deal with one coverage
            if (reader instanceof StructuredGridCoverage2DReader && fileId != null) {
                collector.collectSubset(fileId, result);
            } else {
                // Simple reader case.
                collector.collectFull(result);
            }
        } else {
            throw new ServiceException(
                    "Unable to get files from the specified ResourceInfo which"
                            + " doesn't implement FileResourceInfo");
        }
    }

    /**
     * Check the current download is not exceeding the maxDownloadSize limit (if activated). Throws
     * a {@link CSWException} in case the limit is exceeded
     */
    private void checkSizeLimit(List<File> fileList, CoverageInfo info) {
        DirectDownloadSettings settings =
                DirectDownloadSettings.getSettingsFromMetadata(info.getMetadata(), csw);
        long maxSize = settings != null ? settings.getMaxDownloadSize() : 0;
        long sizeLimit = maxSize * 1024;
        if (fileList != null && !fileList.isEmpty() && sizeLimit > 0) {
            long cumulativeSize = 0;
            for (File file : fileList) {
                cumulativeSize += file.length();
            }
            if (cumulativeSize > sizeLimit) {
                throw new CSWException(
                        LIMIT_MESSAGE
                                + "The limit is "
                                + formatBytes(sizeLimit)
                                + " but the amount of raw data to be downloaded is "
                                + formatBytes(cumulativeSize));
            }
        }
    }

    /** Format a size in a human readable way */
    static String formatBytes(long bytes) {
        if (bytes < KILO) {
            return bytes + "B";
        } else if (bytes < MEGA) {
            return new DecimalFormat("#.##").format(bytes / 1024.0) + "KB";
        } else if (bytes < GIGA) {
            return new DecimalFormat("#.##").format(bytes / 1048576.0) + "MB";
        } else {
            return new DecimalFormat("#.##").format(bytes / 1073741824.0) + "GB";
        }
    }

    /** Gently close a {@link CloseableIterator} */
    private void closeIterator(CloseableIterator<FileGroup> files) {
        if (files != null) {
            try {
                // Make sure to close the iterator
                files.close();
            } catch (Throwable t) {
                // Simply log it at finer level
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer(
                            "Exception occurred while closing the file iterator:\n "
                                    + t.getLocalizedMessage());
                }
            }
        }
    }

    public static String extractName(CoverageInfo coverageInfo) {
        String name = null;
        if (coverageInfo != null) {
            name = coverageInfo.getNativeCoverageName();
            if (name == null) {
                name = coverageInfo.getName();
            }
            if (name == null) {
                name = coverageInfo.getNativeName();
            }
        }
        return name;
    }
}
