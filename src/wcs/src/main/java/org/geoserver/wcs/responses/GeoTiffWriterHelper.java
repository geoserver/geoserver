/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.media.jai.OpImage;
import javax.media.jai.RenderedOp;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;

/**
 * Support class setting up reasonable defaults on the write parameters and centralizing the write
 * code and associated optimizations
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GeoTiffWriterHelper {

    private static final float DEFAULT_QUALITY = 0.75f;

    private static final GeoTiffFormat TIFF_FORMAT = new GeoTiffFormat();

    private GridCoverage2D coverage;

    private File sourceFile;

    private GeoTiffWriteParams imageIoWriteParams;

    private ParameterValueGroup geotoolsWriteParams;

    public GeoTiffWriterHelper(GridCoverage2D coverage) throws IOException {
        this.coverage = coverage;

        // did we get lucky and all we need to do is to copy the original file file over?
        if (isUnprocessed(coverage)) {
            this.sourceFile = getSourceFile(coverage);
        }

        // setup default writing params, respect by default the original tiling structure
        // for optimal extraction performance
        this.imageIoWriteParams = buildWriteParams(coverage);
        this.geotoolsWriteParams = buildGeoToolsWriteParams(imageIoWriteParams);
    }

    /** Returns the original source file, is present in the metadata, and if the coverage */
    private File getSourceFile(GridCoverage2D coverage) {
        final Object fileSource =
                coverage.getProperty(AbstractGridCoverage2DReader.FILE_SOURCE_PROPERTY);
        if (fileSource != null && fileSource instanceof String) {
            File file = new File((String) fileSource);
            if (file.exists()) {
                GeoTiffReader reader = null;
                try {
                    reader = new GeoTiffReader(file);
                    GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
                    Envelope envelope = coverage.getEnvelope();
                    if (originalEnvelope.equals(envelope, 1e-9, false)) {
                        GridCoverage2D test = reader.read(null);
                        ImageUtilities.disposeImage(test.getRenderedImage());
                        return file;
                    }
                } catch (Exception e) {
                    // ok, not a geotiff!
                } finally {
                    if (reader != null) {
                        reader.dispose();
                    }
                }
            }
        }

        return null;
    }

    private GeoTiffWriteParams buildWriteParams(GridCoverage2D coverage) {
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

        GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
        writeParams.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        writeParams.setTiling(tileWidth, tileHeight);
        return writeParams;
    }

    private ParameterValueGroup buildGeoToolsWriteParams(GeoTiffWriteParams writeParams) {
        final ParameterValueGroup wparams = TIFF_FORMAT.getWriteParameters();
        wparams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                .setValue(writeParams);
        return wparams;
    }

    /** Returns the write parameters, allowing their customization */
    public GeoTiffWriteParams getImageIoWriteParams() {
        return imageIoWriteParams;
    }

    /** Returns the GeoTools grid writer params, allowing their customization */
    public ParameterValueGroup getGeotoolsWriteParams() {
        return geotoolsWriteParams;
    }

    /**
     * The code can figure out if it's really just copying over a GeoTiff source file and run a
     * straight file copy, call this method if the source copy should be turned off
     */
    public void disableSourceCopyOptimization() {
        this.sourceFile = null;
    }

    public void write(OutputStream stream) throws IOException {
        if (sourceFile != null) {
            FileUtils.copyFile(sourceFile, stream);
        } else {
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
            boolean unreferenced = crs == null || crs instanceof EngineeringCRS;

            if (unreferenced) {
                RenderedImage ri = coverage.getRenderedImage();
                int tileWidth, tileHeight;
                if (imageIoWriteParams.getTilingMode() == GeoToolsWriteParams.MODE_EXPLICIT) {
                    tileWidth = imageIoWriteParams.getTileWidth();
                    tileHeight = imageIoWriteParams.getTileHeight();
                } else {
                    tileWidth = ri.getTileWidth();
                    tileHeight = ri.getTileHeight();
                }
                float quality = DEFAULT_QUALITY;
                String compression = null;
                if (imageIoWriteParams.getCompressionMode() == GeoToolsWriteParams.MODE_EXPLICIT) {
                    compression = imageIoWriteParams.getCompressionType();
                    quality = imageIoWriteParams.getCompressionQuality();
                }

                new ImageWorker(ri).writeTIFF(stream, compression, quality, tileWidth, tileHeight);
            } else {
                final GeneralParameterValue[] wps =
                        (GeneralParameterValue[])
                                geotoolsWriteParams
                                        .values()
                                        .toArray(
                                                new GeneralParameterValue
                                                        [geotoolsWriteParams.values().size()]);

                // write out the coverage
                AbstractGridCoverageWriter writer =
                        (AbstractGridCoverageWriter) TIFF_FORMAT.getWriter(stream);
                if (writer == null)
                    throw new ServiceException(
                            "Could not find the GeoTIFF writer, please check it's in the classpath");
                try {
                    writer.write(coverage, wps);
                } finally {
                    try {
                        writer.dispose();
                    } catch (Exception e) {
                        // swallow, silent close
                    }
                }
            }
        }
    }

    /** Returns true if the coverage has not been processed in any way since it has been read */
    private boolean isUnprocessed(GridCoverage2D coverage) {
        RenderedImage ri = coverage.getRenderedImage();
        if (ri instanceof RenderedOp) {
            RenderedOp op = (RenderedOp) ri;
            return op.getOperationName().startsWith("ImageRead");
        } else if (ri instanceof OpImage) {
            return ri.getClass().getSimpleName().startsWith("ImageRead");
        } else {
            return true;
        }
    }
}
