/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import com.google.common.base.Splitter;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.ConstantDescriptor;
import org.apache.commons.io.IOUtils;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.resource.WPSFileResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.data.Query;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.ImageWorker;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.DescribeResults;
import org.geotools.referencing.CRS;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;

/**
 * Georectifies a GridCoverage based on GCPs using gdal_warp under covers
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */
@DescribeProcess(
        title = "Georectify Coverage",
        description = "Georectifies a raster via Ground Control Points using gdal_warp")
public class GeorectifyCoverage implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(GeorectifyCoverage.class);

    private static final Pattern GCP_PATTERN =
            Pattern.compile(
                    "\\[((\\+|-)?[0-9]+(.[0-9]+)?),\\s*((\\+|-)?[0-9]+(.[0-9]+)?)(,\\s*((\\+|-)?[0-9]+(.[0-9]+)?))?\\]");

    GeorectifyConfiguration config;

    WPSResourceManager resourceManager;

    public GeorectifyConfiguration getConfig() {
        return config;
    }

    public void setConfig(GeorectifyConfiguration config) {
        this.config = config;
    }

    public GeorectifyCoverage(GeorectifyConfiguration config) {
        this.config = config;
    }

    public GeorectifyCoverage() {}

    @DescribeResults({
        @DescribeResult(
                name = "result",
                description = "Georectified raster",
                type = GridCoverage2D.class),
        @DescribeResult(
                name = "path",
                description = "Pathname of the generated raster on the server",
                type = String.class)
    })
    public Map<String, Object> execute(
            @DescribeParameter(name = "data", description = "Input raster") GridCoverage2D coverage,
            @DescribeParameter(
                            name = "gcp",
                            description =
                                    "List of Ground control points.  Points are specified as [x,y] or [x,y,z].")
                    String gcps,
            @DescribeParameter(name = "bbox", description = "Bounding box for output", min = 0)
                    Envelope bbox,
            @DescribeParameter(
                            name = "targetCRS",
                            description =
                                    "Coordinate reference system to use for the output raster")
                    CoordinateReferenceSystem crs,
            @DescribeParameter(
                            name = "width",
                            description = "Width of output raster in pixels",
                            min = 0)
                    Integer width,
            @DescribeParameter(
                            name = "height",
                            description = "Height of output raster in pixels",
                            min = 0)
                    Integer height,
            @DescribeParameter(
                            name = "warpOrder",
                            min = 0,
                            description = "Order of the warping polynomial (1 to 3)")
                    Integer warpOrder,
            @DescribeParameter(
                            name = "transparent",
                            min = 0,
                            description = "Force output to have transparent background",
                            defaultValue = "true")
                    Boolean transparent,
            @DescribeParameter(
                            name = "store",
                            min = 0,
                            description =
                                    "Indicates whether to keep the output file after processing",
                            defaultValue = "false")
                    Boolean store,
            @DescribeParameter(
                            name = "outputPath",
                            min = 0,
                            description = "Pathname where the output file is stored")
                    String outputPath)
            throws IOException {

        GeoTiffReader reader = null;
        List<File> removeFiles = new ArrayList<>();
        String location = null;
        try {
            File tempFolder = config.getTempFolder();
            File loggingFolder = config.getLoggingFolder();

            // do we have to add the alpha channel?
            boolean forceTransparent = false;
            if (transparent == null) {
                transparent = true;
            }
            ColorModel cm = coverage.getRenderedImage().getColorModel();
            if (cm.getTransparency() == Transparency.OPAQUE && transparent) {
                forceTransparent = true;
            }

            // //
            //
            // STEP 1: Getting the dataset to be georectified
            //
            // //
            final Object fileSource =
                    coverage.getProperty(GridCoverage2DReader.FILE_SOURCE_PROPERTY);
            if (fileSource != null && fileSource instanceof String) {
                location = (String) fileSource;
            }
            if (location == null) {
                RenderedImage image = coverage.getRenderedImage();
                if (forceTransparent) {
                    ImageWorker iw = new ImageWorker(image);
                    iw.forceComponentColorModel();
                    final ImageLayout tempLayout = new ImageLayout(image);
                    tempLayout
                            .unsetValid(ImageLayout.COLOR_MODEL_MASK)
                            .unsetValid(ImageLayout.SAMPLE_MODEL_MASK);
                    RenderedImage alpha =
                            ConstantDescriptor.create(
                                    Float.valueOf(image.getWidth()),
                                    Float.valueOf(image.getHeight()),
                                    new Byte[] {Byte.valueOf((byte) 255)},
                                    new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tempLayout));
                    iw.addBand(alpha, false);
                    image = iw.getRenderedImage();
                    cm = image.getColorModel();
                }
                File storedImageFile = storeImage(image, tempFolder);
                location = storedImageFile.getAbsolutePath();
                removeFiles.add(storedImageFile);
            }

            // //
            //
            // STEP 2: Adding Ground Control Points
            //
            // //
            final int[] gcpNum = new int[1];
            final List<String> gcp = parseGcps(gcps, gcpNum);
            File vrtFile =
                    addGroundControlPoints(
                            location, gcp, splitToList(config.getGdalTranslateParameters()));
            if (vrtFile == null || !vrtFile.exists() || !vrtFile.canRead()) {
                throw new IOException(
                        "Unable to get a valid file with attached Ground Control Points");
            }
            removeFiles.add(vrtFile);

            // //
            //
            // STEP 3: Warping
            //
            // //
            File warpedFile =
                    warpFile(
                            vrtFile,
                            bbox,
                            crs,
                            width,
                            height,
                            warpOrder,
                            tempFolder,
                            loggingFolder,
                            config.getExecutionTimeout(),
                            splitToList(config.getGdalTranslateParameters()));
            if (warpedFile == null || !warpedFile.exists() || !warpedFile.canRead()) {
                throw new IOException("Unable to get a valid georectified file");
            }

            boolean expand = false;
            if (cm instanceof IndexColorModel) {
                expand = true;
            } else if (cm instanceof ComponentColorModel
                    && cm.getNumComponents() == 1
                    && cm.getComponentSize()[0] == 1) {
                expand = true;
            }
            if (expand) {
                removeFiles.add(warpedFile);
                warpedFile = expandRgba(warpedFile.getAbsolutePath());
            }

            // if we have the output path move the final file there
            if (Boolean.TRUE.equals(store) && outputPath != null) {
                try {
                    File output = resourceManager.getExternalOutputFile(outputPath, null);
                    if (output.exists() && !output.delete()) {
                        throw new WPSException(
                                "Output file " + outputPath + " exists but cannot be overwritten");
                    }
                    if (!warpedFile.renameTo(output)) {
                        throw new WPSException(
                                "Could not move "
                                        + warpedFile.getAbsolutePath()
                                        + " to "
                                        + outputPath
                                        + ", it's likely a permission issue");
                    }
                    warpedFile = output;
                } catch (Exception e) {
                    removeFiles.add(warpedFile);
                    throw e;
                }
            }

            // mark the output file for deletion at the end of request
            if (resourceManager != null && !Boolean.TRUE.equals(store)) {
                resourceManager.addResource(new WPSFileResource(warpedFile));
            }

            // //
            //
            // FINAL STEP: Returning the warped gridcoverage
            //
            // //
            reader = new GeoTiffReader(warpedFile);
            GridCoverage2D cov = addLocationProperty(reader.read(null), warpedFile);

            Map<String, Object> result = new HashMap<>();
            result.put("result", cov);
            result.put("path", warpedFile.getAbsolutePath());
            return result;
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }

            for (File file : removeFiles) {
                deleteFile(file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    GridCoverage2D addLocationProperty(GridCoverage2D coverage, File warpedFile) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(GridCoverage2DReader.FILE_SOURCE_PROPERTY, warpedFile.getAbsolutePath());
        properties.putAll(coverage.getProperties());

        return new GridCoverageFactory()
                .create(
                        coverage.getName(),
                        coverage.getRenderedImage(),
                        coverage.getGridGeometry(),
                        coverage.getSampleDimensions(),
                        null,
                        properties);
    }

    /**
     * Given a target query and a target grid geometry returns the query to be used to read the
     * input data of the process involved in rendering. This method will be called only if the input
     * data is a feature collection.
     *
     * @return The transformed query, or null if no inversion is possible/meaningful
     */
    public Query invertQuery(Query targetQuery, GridGeometry gridGeometry) {
        return targetQuery;
    }

    /**
     * Given a target query and a target grid geometry returns the grid geometry to be used to read
     * the input data of the process involved in rendering. This method will be called only if the
     * input data is a grid coverage or a grid coverage reader
     *
     * @return The transformed query, or null if no inversion is possible/meaningful
     */
    public GridGeometry invertGridGeometry(Query targetQuery, GridGeometry targetGridGeometry) {
        // we need the entire image, we don't know how to invert the warping
        return null;
    }

    /**
     * Store a GridCoverage2D and returns the file where the underlying image have been stored.
     *
     * @param image the to be stored.
     * @return the {@link File} storing the image.
     */
    private File storeImage(final RenderedImage image, final File tempFolder) throws IOException {
        File file = File.createTempFile("readCoverage", ".tif", tempFolder);
        new ImageWorker(image).writeTIFF(file, null, 0, 256, 256);
        return file;
    }

    /**
     * @param originalFile {@link File} referring the dataset to be warped
     * @param targetEnvelope the target envelope
     * @param width the final image's width
     * @param height the final image's height
     * @param targetCRS the target coordinate reference system
     */
    private File warpFile(
            final File originalFile,
            final Envelope targetEnvelope,
            final CoordinateReferenceSystem targetCRS,
            final Integer width,
            final Integer height,
            final Integer order,
            final File tempFolder,
            final File loggingFolder,
            final Long timeOut,
            final List<String> warpingParameters)
            throws IOException {
        final File file = File.createTempFile("warped", ".tif", tempFolder);
        final String vrtFilePath = originalFile.getAbsolutePath();
        final String outputFilePath = file.getAbsolutePath();
        final List<String> tEnvelope = parseBBox(targetEnvelope);
        final String tCrs = parseCrs(targetCRS);
        final List<String> arguments =
                buildWarpArguments(
                        tEnvelope,
                        width,
                        height,
                        tCrs,
                        order,
                        vrtFilePath,
                        outputFilePath,
                        warpingParameters);
        final String gdalCommand = config.getWarpingCommand();

        executeCommand(gdalCommand, arguments, loggingFolder, config.getEnvVariables());
        return file;
    }

    /**
     * A simple utility method setting up the command arguments for gdalWarp
     *
     * @param targetEnvelope the target envelope in the form: xmin ymin xmax ymax
     * @param width the target image width
     * @param height the target image height
     * @param targetCrs the target crs
     * @param order the warping polynomial order
     * @param inputFilePath the path of the file referring to the dataset to be warped
     * @param outputFilePath the path of the file referring to the produced dataset
     */
    @SuppressWarnings("serial")
    private static final List<String> buildWarpArguments(
            final List<String> targetEnvelope,
            final Integer width,
            final Integer height,
            final String targetCrs,
            final Integer order,
            final String inputFilePath,
            final String outputFilePath,
            final List<String> warpingParameters) {
        List<String> result = new ArrayList<>();

        if (targetEnvelope != null && !targetEnvelope.isEmpty()) {
            result.add("-te");
            result.addAll(targetEnvelope);
        }
        if (width != null && height != null) {
            result.add("-ts");
            result.add(Integer.toString(width));
            result.add(Integer.toString(height));
        }
        result.add("-t_srs");
        result.add(targetCrs);
        if (order != null) {
            result.add("-order");
            result.add(Integer.toString(order));
        }
        result.addAll(warpingParameters);
        result.add(inputFilePath);
        result.add(outputFilePath);

        return result;
    }

    private static String getError(File logFile) throws IOException {
        StringBuilder message = new StringBuilder();
        try (InputStream stream = new FileInputStream(logFile);
                InputStreamReader streamReader = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(streamReader)) {
            String strLine;
            while ((strLine = reader.readLine()) != null) {
                message.append(strLine);
            }
            return message.toString();
        } finally {
            // TODO: look for a better delete
            deleteFile(logFile);
        }
    }

    /** Parse the bounding box to be used by gdalwarp command */
    @SuppressWarnings("serial")
    private static List<String> parseBBox(Envelope re) {
        if (re == null) {
            return Collections.emptyList();
        } else {
            return DoubleStream.of(re.getMinX(), re.getMinY(), re.getMaxX(), re.getMaxY())
                    .mapToObj(Double::toString)
                    .collect(Collectors.toList());
        }
    }

    private static String parseCrs(CoordinateReferenceSystem crs) {
        Utilities.ensureNonNull("coordinateReferenceSystem", crs);
        try {
            return CRS.lookupIdentifier(crs, true);
        } catch (FactoryException e) {
            throw new WPSException("Error occurred looking up target SRS");
        }
    }

    /**
     * First processing step which setup a VRT by adding ground control points to the specified
     * input file.
     *
     * @param originalFilePath the path of the file referring to the original image.
     * @param gcp the Ground Control Points option to be attached to the translating command.
     * @return a File containing the translated dataset.
     */
    private File addGroundControlPoints(
            final String originalFilePath, final List<String> gcp, final List<String> parameters)
            throws IOException {
        final File vrtFile = File.createTempFile("vrt_", ".vrt", config.getTempFolder());
        @SuppressWarnings("serial")
        final List<String> arguments = new ArrayList<>();
        arguments.add("-of");
        arguments.add("VRT");
        arguments.addAll(parameters);
        arguments.addAll(gcp);
        arguments.add(originalFilePath);
        arguments.add(vrtFile.getAbsolutePath());

        final String gdalCommand = config.getTranslateCommand();
        executeCommand(gdalCommand, arguments, config.getLoggingFolder(), config.getEnvVariables());
        if (vrtFile != null && vrtFile.exists() && vrtFile.canRead()) {
            return vrtFile;
        }
        return vrtFile;
    }

    private File expandRgba(final String originalFilePath) throws IOException {
        final File expandedFile = File.createTempFile("rgba", ".tif", config.getTempFolder());
        final List<String> arguments = new ArrayList<>();
        arguments.addAll(splitToList("-expand RGBA -co TILED=yes -co COMPRESS=LZW"));
        arguments.add(originalFilePath);
        arguments.add(expandedFile.getAbsolutePath());

        final String gdalCommand = config.getTranslateCommand();
        executeCommand(gdalCommand, arguments, config.getLoggingFolder(), config.getEnvVariables());
        return expandedFile;
    }

    /**
     * Execute the following command, given the specified argument and return the File storing
     * logged error messages (if any).
     */
    private static void executeCommand(
            final String gdalCommand,
            final List<String> arguments,
            final File loggingFolder,
            final Map<String, String> envVars)
            throws IOException {

        final File logFile = File.createTempFile("LOG", ".log", loggingFolder);

        // run the process and grab the output for error reporting purposes
        List<String> commands = new ArrayList<>(arguments);
        commands.add(0, gdalCommand);
        ProcessBuilder builder = new ProcessBuilder(commands);
        if (envVars != null) {
            builder.environment().putAll(envVars);
        } else {
            builder.environment().putAll(System.getenv());
        }
        builder.redirectErrorStream(true);

        int exitValue = 0;
        try (OutputStream log = new FileOutputStream(logFile)) {
            Process p = builder.start();
            IOUtils.copy(p.getInputStream(), log);

            p.waitFor();
            log.flush();
            exitValue = p.exitValue();
        } catch (Exception e) {
            throw new WPSException(
                    "Error launching OS command: "
                            + gdalCommand
                            + " with arguments "
                            + arguments
                            + " and env vars "
                            + envVars,
                    e);
        } finally {
            if (exitValue != 0) {
                if (logFile.exists() && logFile.canRead()) {
                    String error = getError(logFile);
                    throw new WPSException(
                            "Error launching OS command: '"
                                    + gdalCommand
                                    + "' with arguments '"
                                    + arguments
                                    + "' and env vars '"
                                    + envVars
                                    + "': \n"
                                    + error);
                }
            }

            if (logFile != null) {
                logFile.delete();
            }
        }
    }

    public boolean isAvailable() {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        try {
            executeCommand(
                    GeorectifyConfiguration.GRDefaults.GDAL_TRANSLATE_COMMAND,
                    Collections.singletonList("--version"),
                    tmp,
                    config.getEnvVariables());
            executeCommand(
                    GeorectifyConfiguration.GRDefaults.GDAL_WARP_COMMAND,
                    Collections.singletonList("--version"),
                    tmp,
                    config.getEnvVariables());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "GDAL utilities are not available", e);
            return false;
        } finally {
            tmp.delete();
        }
    }

    /** */
    private List<String> parseGcps(String gcps, int[] gcpNum) {
        Matcher gcpMatcher = GCP_PATTERN.matcher(gcps);
        // if(!gcpMatcher.matches()) {
        // throw new WPSException("Invalid GCP syntax:" + gcps);
        // }
        List<String> gcpCommand = new ArrayList<>();
        int gcpPoints = 0;
        // Setting up gcp command arguments
        while (gcpMatcher.find()) {
            @SuppressWarnings("serial")
            List<String> gcp = new ArrayList<>();
            gcp.add("-gcp");

            String pixels = gcpMatcher.group(0);
            gcpMatcher.find();
            String lines = gcpMatcher.group(0);
            gcp.addAll(splitToList(pixels.replace("[", "").replace("]", "").replace(",", "")));
            gcp.addAll(splitToList(lines.replace("[", "").replace("]", "").replace(",", "")));
            gcpCommand.addAll(gcp);
            gcpPoints++;
        }
        gcpNum[0] = gcpPoints;
        return gcpCommand;
    }

    private static void deleteFile(final File file) {
        if (file != null && file.exists() && file.canRead()) {
            file.delete();
        }
    }

    public WPSResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(WPSResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Split a whitespace-separated list into a list of strings, discarding all excess whitespace.
     * No empty strings are returned. If the string is empty or contains only whitespace, the
     * returned list will be empty.
     */
    private static List<String> splitToList(String s) {
        return Splitter.onPattern("\\s+").omitEmptyStrings().splitToList(s);
    }
}
