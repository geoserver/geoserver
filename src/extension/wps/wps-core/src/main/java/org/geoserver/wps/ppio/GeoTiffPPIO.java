/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageWriteParam;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wcs.responses.GeoTiffWriterHelper;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.resource.GridCoverageReaderResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.process.ProcessException;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Decodes/encodes a GeoTIFF file
 *
 * @author Andrea Aime - OpenGeo
 * @author Simone Giannecchini, GeoSolutions
 * @author Daniele Romagnoli, GeoSolutions
 */
public class GeoTiffPPIO extends BinaryPPIO implements ExtensionPriority {

    protected static final String TILE_WIDTH_KEY = "tilewidth";
    protected static final String TILE_HEIGHT_KEY = "tileheight";
    protected static final String COMPRESSION_KEY = "compression";
    protected static final String WRITENODATA_KEY = "writenodata";

    private static final Set<String> SUPPORTED_PARAMS = new HashSet<>();

    private static final String SUPPORTED_PARAMS_LIST;

    private static final String DEFAULT_COMPRESSION = "Deflate";

    static {
        SUPPORTED_PARAMS.add(TILE_WIDTH_KEY);
        SUPPORTED_PARAMS.add(TILE_HEIGHT_KEY);
        SUPPORTED_PARAMS.add(COMPRESSION_KEY);
        SUPPORTED_PARAMS.add(QUALITY_KEY);
        SUPPORTED_PARAMS.add(WRITENODATA_KEY);

        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (String param : SUPPORTED_PARAMS) {
            sb.append(prefix).append(param);
            prefix = " / ";
        }
        SUPPORTED_PARAMS_LIST = sb.toString();
    }

    private static final Logger LOGGER = Logging.getLogger(GeoTiffPPIO.class);

    private final WPSResourceManager resources;

    protected GeoTiffPPIO(WPSResourceManager resources) {
        super(GridCoverage2D.class, GridCoverage2D.class, "image/tiff");
        this.resources = resources;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        // in order to read a grid coverage we need to first store it on disk
        File root = new File(System.getProperty("java.io.tmpdir", "."));
        File f = File.createTempFile("wps", "tiff", root);
        GridCoverageReaderResource resource = null;
        try {
            FileUtils.copyInputStreamToFile(input, f);
            AbstractGridFormat format = GridFormatFinder.findFormat(f);
            if (format instanceof UnknownFormat) {
                throw new WPSException(
                        "Could not find the GeoTIFF GT2 format, please check it's in the classpath");
            }
            AbstractGridCoverage2DReader reader = format.getReader(f);
            resource = new GridCoverageReaderResource(reader, f);
            return reader.read(null);
        } finally {
            if (resource != null) {
                resources.addResource(resource);
            } else {
                f.delete();
            }
        }
    }

    private Map<String, Object> getDefaultWritingParams(Object value) throws IOException {
        GridCoverage2D coverage = (GridCoverage2D) value;
        final RenderedImage renderedImage = coverage.getRenderedImage();
        int tileWidth = renderedImage.getTileWidth();
        int tileHeight = renderedImage.getTileHeight();

        // avoid tiles bigger than the image
        final GridEnvelope gr = coverage.getGridGeometry().getGridRange();
        if (gr.getSpan(0) < tileWidth) {
            tileWidth = gr.getSpan(0);
        }
        if (gr.getSpan(1) < tileHeight) {
            tileHeight = gr.getSpan(1);
        }
        Map<String, Object> defaultsMap = new HashMap<>();
        defaultsMap.put(TILE_WIDTH_KEY, String.valueOf(tileWidth));
        defaultsMap.put(TILE_HEIGHT_KEY, String.valueOf(tileHeight));
        defaultsMap.put(COMPRESSION_KEY, DEFAULT_COMPRESSION);
        return defaultsMap;
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        encode(value, getDefaultWritingParams(value), os);
    }

    @Override
    public void encode(Object value, Map<String, Object> encodingParameters, OutputStream os)
            throws Exception {
        GridCoverage2D coverage = (GridCoverage2D) value;
        GeoTiffWriterHelper helper = new GeoTiffWriterHelper(coverage);
        setEncodingParams(helper, encodingParameters);

        try {
            helper.write(os);
        } catch (IOException e) {
            throw new ProcessException(e);
        }
    }

    private void setEncodingParams(
            GeoTiffWriterHelper helper, Map<String, Object> encodingParameters) {
        if (encodingParameters != null && !encodingParameters.isEmpty()) {
            for (String encodingParam : encodingParameters.keySet()) {
                if (!SUPPORTED_PARAMS.contains(encodingParam)) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(
                                "The specified parameter will be ignored: "
                                        + encodingParam
                                        + " Supported parameters are in the list: "
                                        + SUPPORTED_PARAMS_LIST);
                    }
                }
            }

            GeoTiffWriteParams writeParams = helper.getImageIoWriteParams();
            if (writeParams != null) {

                // Inner Tiling Settings
                if (encodingParameters.containsKey(TILE_WIDTH_KEY)
                        && encodingParameters.containsKey(TILE_HEIGHT_KEY)) {
                    String tileWidth = (String) encodingParameters.get(TILE_WIDTH_KEY);
                    String tileHeight = (String) encodingParameters.get(TILE_HEIGHT_KEY);
                    try {
                        int tw = Integer.parseInt(tileWidth);
                        int th = Integer.parseInt(tileHeight);
                        writeParams.setTilingMode(ImageWriteParam.MODE_EXPLICIT);
                        writeParams.setTiling(tw, th);

                    } catch (NumberFormatException nfe) {
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.info(
                                    "Specified tiling parameters are not valid. tileWidth = "
                                            + tileWidth
                                            + " tileHeight = "
                                            + tileHeight);
                        }
                    }
                }

                // COMPRESSION Settings
                if (encodingParameters.containsKey(COMPRESSION_KEY)) {
                    String compressionType = (String) encodingParameters.get(COMPRESSION_KEY);
                    writeParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParams.setCompressionType(compressionType);
                    if (encodingParameters.containsKey(QUALITY_KEY)) {
                        String compressionQuality = (String) encodingParameters.get(QUALITY_KEY);
                        try {
                            writeParams.setCompressionQuality(Float.parseFloat(compressionQuality));

                        } catch (NumberFormatException nfe) {
                            if (LOGGER.isLoggable(Level.INFO)) {
                                LOGGER.info(
                                        "Specified quality is not valid (it should be in the range [0,1])."
                                                + " compressionQuality = "
                                                + compressionQuality);
                            }
                        }
                    }
                }
            }
            ParameterValueGroup geotoolsWriteParams = helper.getGeotoolsWriteParams();
            if (geotoolsWriteParams != null && encodingParameters.containsKey(WRITENODATA_KEY)) {
                geotoolsWriteParams
                        .parameter(GeoTiffFormat.WRITE_NODATA.getName().toString())
                        .setValue(
                                Boolean.parseBoolean(
                                        (String) encodingParameters.get(WRITENODATA_KEY)));
            }
        }
    }

    @Override
    public String getFileExtension() {
        return "tiff";
    }

    // Make GeoTIFF the default GridCoverage2D PPIO for backwards compatibility
    @Override
    public int getPriority() {
        return HIGHEST;
    }
}
