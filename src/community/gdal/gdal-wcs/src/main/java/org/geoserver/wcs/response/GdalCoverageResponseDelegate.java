/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipOutputStream;
import org.geoserver.config.GeoServer;
import org.geoserver.ogr.core.Format;
import org.geoserver.ogr.core.FormatAdapter;
import org.geoserver.ogr.core.FormatConverter;
import org.geoserver.ogr.core.ToolWrapper;
import org.geoserver.ogr.core.ToolWrapperFactory;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.util.Utilities;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Implementation of {@link CoverageResponseDelegate} that leverages the gdal_translate utility to
 * encode coverages in any output format supported by the GDAL library available on the system where
 * GeoServer is running.
 *
 * <p>The encoding process involves two steps:
 *
 * <ol>
 *   <li>the coverage passed as input to the <code>encode()</code> method is dumped to a temporary
 *       file on disk in GeoTIFF format
 *   <li>the <code>gdal_translate</code> command is invoked with the provided options to convert the
 *       dumped GeoTIFF file to the desired format
 * </ol>
 *
 * <p>Configuration for the supported output formats must be passed to the class via its {@link
 * #addFormat(GdalFormat)} method.
 *
 * @author Stefano Costa, GeoSolutions
 */
public class GdalCoverageResponseDelegate implements CoverageResponseDelegate, FormatConverter {

    private static final GeoTiffFormat GEOTIF_FORMAT = new GeoTiffFormat();

    /** Facade to GeoServer's configuration */
    GeoServer geoServer;

    /** Factory to create the gdal_translate wrapper. */
    ToolWrapperFactory gdalWrapperFactory;

    /**
     * The fs path to gdal_translate. If null, we'll assume gdal_translate is in the PATH and that
     * we can execute it just by running gdal_translate
     */
    String gdalTranslatePath = null;

    /** The full path to gdal_translate */
    String gdalTranslateExecutable = "gdal_translate";

    /** The environment variables to set before invoking gdal_translate */
    Map<String, String> environment = null;

    /** Map holding the descriptors of the supported GDAL formats (keyed by format name). */
    static Map<String, Format> formats = new HashMap<String, Format>();
    /** Map holding the descriptors of the supported GDAL formats (keyed by mime type). */
    static Map<String, Format> formatsByMimeType = new HashMap<String, Format>();

    /** Lock guarding concurrent access to the maps holding the format descriptors. */
    private ReadWriteLock formatsLock;

    /** @param gs */
    public GdalCoverageResponseDelegate(GeoServer gs, ToolWrapperFactory wrapperFactory) {
        this.formatsLock = new ReentrantReadWriteLock();
        this.geoServer = gs;
        this.gdalWrapperFactory = wrapperFactory;
        this.environment = new HashMap<String, String>();
    }

    /** Returns the gdal_translate executable full path. */
    @Override
    public String getExecutable() {
        return gdalTranslateExecutable;
    }

    /**
     * Sets the gdal_translate executable full path. The default value is simply "gdal_translate",
     * which will work if gdal_translate is in the path.
     */
    @Override
    public void setExecutable(String gdalTranslate) {
        this.gdalTranslateExecutable = gdalTranslate;
    }

    /** Returns the environment variables that are set prior to invoking gdal_translate. */
    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * Provides the environment variables that are set prior to invoking gdal_translate (notably the
     * GDAL_DATA variable, specifying the location of GDAL's data directory).
     */
    @Override
    public void setEnvironment(Map<String, String> environment) {
        if (environment != null) {
            this.environment.clear();
            this.environment.putAll(environment);
        }
    }

    /** Adds a GDAL format among the supported ones */
    @Override
    public void addFormat(Format format) {
        if (format == null) {
            throw new IllegalArgumentException("No format provided");
        }

        formatsLock.writeLock().lock();
        try {
            addFormatInternal(format);
        } finally {
            formatsLock.writeLock().unlock();
        }
    }

    private void addFormatInternal(Format format) {
        formats.put(format.getGeoserverFormat().toUpperCase(), format);
        if (format.getMimeType() != null) {
            formatsByMimeType.put(format.getMimeType().toUpperCase(), format);
        }
    }

    /**
     * Get a list of supported GDAL formats
     *
     * @return the list of supported formats
     */
    @Override
    public List<Format> getFormats() {
        formatsLock.readLock().lock();
        try {
            return new ArrayList<Format>(formats.values());
        } finally {
            formatsLock.readLock().unlock();
        }
    }

    /** Programmatically removes all formats */
    @Override
    public void clearFormats() {
        formatsLock.writeLock().lock();
        try {
            clearFormatsInternal();
        } finally {
            formatsLock.writeLock().unlock();
        }
    }

    private void clearFormatsInternal() {
        formats.clear();
        formatsByMimeType.clear();
    }

    /** Replaces currently supported formats with the provided list. */
    @Override
    public void replaceFormats(List<Format> formats) {
        if (formats == null || formats.isEmpty()) {
            throw new IllegalArgumentException("No formats provided");
        }

        formatsLock.writeLock().lock();
        try {
            clearFormatsInternal();
            for (Format format : formats) {
                if (format != null) {
                    addFormatInternal(format);
                }
            }
        } finally {
            formatsLock.writeLock().unlock();
        }
    }

    @Override
    public boolean canProduce(String outputFormat) {
        try {
            return getGdalFormat(outputFormat) != null;
        } catch (WcsException e) {
            // format was not found
            return false;
        }
    }

    @Override
    public String getMimeType(String outputFormat) {
        String mimeType = "";

        Format format = getGdalFormat(outputFormat);
        if (format.isSingleFile()) {
            if (format.getMimeType() != null) {
                mimeType = format.getMimeType();
            } else {
                // use a default binary blob
                mimeType = "application/octet-stream";
            }
        } else {
            mimeType = "application/zip";
        }

        return mimeType;
    }

    @Override
    public String getFileExtension(String outputFormat) {
        String extension = "";

        Format format = getGdalFormat(outputFormat);
        if (format.isSingleFile()) {
            if (format.getFileExtension() != null) {
                extension = format.getFileExtension();
            } else {
                // default to .bin
                extension = "bin";
            }
        } else {
            extension = "zip";
        }

        // strip initial '.' character
        if (extension.charAt(0) == '.') {
            extension = extension.substring(1);
        }

        return extension;
    }

    private Format getGdalFormat(String outputFormat) {
        Format format = null;

        formatsLock.readLock().lock();
        try {
            format = formats.get(outputFormat.toUpperCase());
            if (format == null) {
                // try to look it up by mime type
                format = formatsByMimeType.get(outputFormat.toUpperCase());
            }
        } finally {
            formatsLock.readLock().unlock();
        }

        if (format == null) {
            throw new WcsException("Unknown output format: " + outputFormat);
        }

        return format;
    }

    @Override
    public void encode(
            GridCoverage2D coverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        Utilities.ensureNonNull("sourceCoverage", coverage);

        // figure out which output format we're going to generate
        Format format = getGdalFormat(outputFormat);

        for (FormatAdapter adapter : format.getFormatAdapters()) {
            coverage = (GridCoverage2D) adapter.adapt(coverage);
        }

        // create the first temp directory, used for dumping gs generated
        // content
        File tempGS = IOUtils.createTempDirectory("gdaltmpin");
        File tempGDAL = IOUtils.createTempDirectory("gdaltmpout");

        // build the gdal wrapper used to run the gdal_translate commands
        ToolWrapper wrapper =
                gdalWrapperFactory.createWrapper(gdalTranslateExecutable, environment);

        // actually export the coverage
        try {
            File outputFile = null;

            // write out the coverage
            File intermediate = writeToDisk(tempGS, coverage);

            // convert with gdal_translate
            final CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
            outputFile =
                    wrapper.convert(
                            intermediate, tempGDAL, coverage.getName().toString(), format, crs);

            // wipe out the input dir contents
            IOUtils.emptyDirectory(tempGS);

            // was it a single file output?
            if (format.isSingleFile()) {
                try (FileInputStream fis = new FileInputStream(outputFile)) {
                    org.apache.commons.io.IOUtils.copy(fis, output);
                }
            } else {
                // scan the output directory and zip it all
                try (ZipOutputStream zipOut = new ZipOutputStream(output)) {
                    IOUtils.zipDirectory(tempGDAL, zipOut, null);
                    zipOut.finish();
                }
            }
        } catch (Exception e) {
            throw new ServiceException("Exception occurred during output generation", e);
        } finally {
            // delete the input and output directories
            IOUtils.delete(tempGS);
            IOUtils.delete(tempGDAL);
        }
    }

    /** Writes to disk using GeoTIFF format. */
    private File writeToDisk(File tempDir, GridCoverage2D coverage) throws Exception {
        // create the temp file for this output
        // TODO: sanitize temp file name
        File outFile = new File(tempDir, coverage.getName().toString() + ".tiff");

        // write out
        GeoTiffWriter writer = null;
        try {
            writer = (GeoTiffWriter) GEOTIF_FORMAT.getWriter(outFile);

            // using default encoding parameters
            final GeoTiffWriteParams wp = new GeoTiffWriteParams();

            final ParameterValueGroup writerParams = GEOTIF_FORMAT.getWriteParameters();
            writerParams
                    .parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(wp);

            WCSInfo wcsService = geoServer.getService(WCSInfo.class);
            if (wcsService != null && wcsService.isLatLon()) {
                writerParams
                        .parameter(GeoTiffFormat.RETAIN_AXES_ORDER.getName().toString())
                        .setValue(true);
            }

            // write down
            if (writer != null)
                writer.write(
                        coverage,
                        (GeneralParameterValue[])
                                writerParams.values().toArray(new GeneralParameterValue[1]));
        } finally {
            try {
                if (writer != null) writer.dispose();
            } catch (Throwable e) {
                // eating exception
            }
            coverage.dispose(false);
        }

        return outFile;
    }

    @Override
    public List<String> getOutputFormats() {
        List<String> outputFormats = null;
        formatsLock.readLock().lock();
        try {
            outputFormats = new ArrayList<String>(formats.keySet());
        } finally {
            formatsLock.readLock().unlock();
        }
        Collections.sort(outputFormats);

        return outputFormats;
    }

    @Override
    public boolean isAvailable() {
        ToolWrapper gdal = gdalWrapperFactory.createWrapper(gdalTranslateExecutable, environment);
        return gdal.isAvailable();
    }

    @Override
    public String getConformanceClass(String format) {
        return "http://www.opengis.net/spec/WCS_coverage-encoding-x" + getMimeType(format);
    }
}
