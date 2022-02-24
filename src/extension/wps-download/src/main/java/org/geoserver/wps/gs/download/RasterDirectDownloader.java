/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.COMPRESSION;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.TILEHEIGHT;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.TILEWIDTH;
import static org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate.TILING;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import it.geosolutions.jaiext.range.NoDataContainer;
import it.geosolutions.jaiext.vectorbin.ROIGeometry;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.RenderedOp;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.Node;

/**
 * Checks if the download process has been effectively returned a source image "as is" (despite all
 * reader configuration, security filtering, security clipping, and so on) and in that case,
 * optimizes out the re-write of such image, providing the original image instead
 */
class RasterDirectDownloader {

    static final Logger LOGGER = Logging.getLogger(RasterDirectDownloader.class);
    private static final String IMAGE_TIFF = "image/tiff";

    private final WPSResourceManager resourceManager;

    RasterDirectDownloader(WPSResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Copies the original file backing the image. Can be invoked only after checking if the copy is
     * viable using {@link #canCopySourceFile(RenderedImage, String, Parameters)}
     */
    Resource copySourceFile(RenderedImage image) throws IOException {
        File sourceFile = getSourceFile(image);
        Resource output =
                resourceManager.getTemporaryResource("." + getExtension(sourceFile.getName()));
        File outputFile = output.file();

        // first try, create a symbolic link, skipping the copy. Not all file systems allow this,
        // but it speeds up the operation very significantly if available.
        try {
            outputFile.delete();
            Files.createSymbolicLink(outputFile.toPath(), sourceFile.toPath());
        } catch (Exception e) {
            // fall back to file copy then
            FileUtils.copyFile(sourceFile, outputFile);
        }
        return output;
    }

    /**
     * We can copy the source file directly if:
     *
     * <ul>
     *   <li>No pixel manipulation has occurred in the middle
     *   <li>There was no request to alter the compression or tiling
     *   <li>ROI isn't specified or it fully contains the image bounds
     * </ul>
     */
    boolean canCopySourceFile(RenderedImage image, String mimeType, Parameters writeParams)
            throws IOException {
        // can we extract a single source file with no pixel related operations
        File file = getSourceFile(image);
        if (file == null) return false;

        // format matches?
        String formatName = getFormatName(file);
        if (formatName == null) return false;
        if ("tif".equals(formatName)) {
            if (((mimeType != null && !IMAGE_TIFF.equals(mimeType)))) {
                LOGGER.fine(
                        "Skipping direct download, a TIFF was requested but the source format is: "
                                + formatName);
                return false;
            } else if (!isGeoTIFF(file)) {
                LOGGER.fine(
                        "Skipping direct download, a TIFF was requested but the source format is not a GeoTIFF");
                return false;
            }
            // heuristic for matching format and mime type (holds for simple cases)
        } else if (mimeType != null && !mimeType.equals("image/" + formatName)) {
            LOGGER.fine(
                    "Skipping direct download, a "
                            + mimeType
                            + " +  was requested but the source format is: "
                            + formatName);
            return false;
        }

        // if there are no write params, we can return whatever structure
        if (!hasWriteParameters(writeParams)) return true;
        // last chance, are we asked for a tiff and the source file structure matches
        boolean result = mimeType.equals(IMAGE_TIFF) && tiffParamsMatch(file, writeParams);
        if (!result)
            LOGGER.fine(
                    "Skipping direct download, write parameters are not a match for the source, or are not recognized: "
                            + writeParams);
        return result;
    }

    private boolean hasWriteParameters(Parameters writeParams) {
        return Optional.ofNullable(writeParams)
                .map(wp -> wp.getParameters())
                .filter(p -> !p.isEmpty())
                .isPresent();
    }

    private boolean tiffParamsMatch(File file, Parameters writeParams) throws IOException {
        Map<String, String> parametersMap = writeParams.getParametersMap();
        GeoTiffReader reader = new GeoTiffReader((file));
        try {
            int matchedParams = 0;

            // match tiling if specified
            ImageLayout layout = reader.getImageLayout();
            if (Boolean.valueOf(parametersMap.get(TILING))
                    && parametersMap.containsKey(TILEHEIGHT)
                    && parametersMap.containsKey(TILEWIDTH)) {
                Integer width = Converters.convert(parametersMap.get(TILEWIDTH), Integer.class);
                Integer height = Converters.convert(parametersMap.get(TILEHEIGHT), Integer.class);
                if ((width != null && layout.getTileWidth(null) != width)
                        || (height != null && layout.getTileHeight(null) != height)) return false;
                matchedParams += 3;
            }

            // match compression if specified
            if (parametersMap.containsKey(COMPRESSION)) {
                String expected = parametersMap.get(COMPRESSION);
                String actual = getCompression(file);
                // oddity of tiff spec, two deflates, one identified as "zlib"
                if ("zlib".equalsIgnoreCase(actual)) actual = "Deflate";
                if (!expected.equalsIgnoreCase(actual)) return false;
                // only match the compression itself, if more compression params were specified
                // like compression level, the match will fail, we cannot match them
                matchedParams++;
            }

            // no other unknown parameters found?
            return parametersMap.size() == matchedParams;
        } finally {
            reader.dispose();
        }
    }

    private String getCompression(File file) throws IOException {
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        try (FileImageInputStream fis = new FileImageInputStream(file)) {
            reader.setInput(fis);

            // compression
            final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
            if (metadata == null) return null;
            IIOMetadataNode root =
                    (IIOMetadataNode)
                            reader.getImageMetadata(0)
                                    .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
            return Optional.ofNullable(getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION))
                    .map(f -> f.getFirstChild())
                    .map(f -> f.getFirstChild())
                    .map(Node::getAttributes)
                    .map(a -> a.item(1))
                    .map(n -> n.getNodeValue())
                    .orElse(null);
        } finally {
            reader.dispose();
        }
    }

    IIOMetadataNode getTiffField(Node rootNode, final int tag) {
        Node node = rootNode.getFirstChild();
        if (node != null) {
            node = node.getFirstChild();
            for (; node != null; node = node.getNextSibling()) {
                Node number = node.getAttributes().getNamedItem(GeoTiffConstants.NUMBER_ATTRIBUTE);
                if (number != null && tag == Integer.parseInt(number.getNodeValue())) {
                    return (IIOMetadataNode) node;
                }
            }
        }
        return null;
    }

    private boolean isGeoTIFF(File file) {
        GeoTiffReader reader = null;
        try {
            reader = new GeoTiffReader(file);
            if (reader == null) return false;
            if (reader.getCoordinateReferenceSystem() == null
                    || DefaultEngineeringCRS.GENERIC_2D.equals(
                            reader.getCoordinateReferenceSystem())) return false;
        } catch (Exception e) {
            LOGGER.log(
                    Level.FINEST,
                    "Could not open reader for tiff file, assuming not GeoTIFF: " + file);
        } finally {
            if (reader != null) reader.dispose();
        }
        return true;
    }

    private String getFormatName(File file) throws IOException {
        try (ImageInputStream is = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            while (readers.hasNext()) {
                return readers.next().getFormatName();
            }
        }
        return null;
    }

    private File getSourceFile(RenderedImage image) {
        RenderedOp read = getImageRead(image);
        if (read == null) return null;

        ParameterBlock params = read.getParameterBlock();
        Object source = params.getObjectParameter(0);

        if (source instanceof FileImageInputStreamExtImpl) {
            return ((FileImageInputStreamExtImpl) source).getFile();
        } else if (source instanceof File) {
            return (File) source;
        } else if (source instanceof String) {
            File file = new File((String) source);
            if (file.exists()) return file;
        }

        LOGGER.fine(
                () ->
                        "Skipping direct download, found a ImageRead but cannot extract a file from the source: "
                                + source);

        return null;
    }

    private RenderedOp getImageRead(RenderedImage image) {
        if (!(image instanceof RenderedOp)) return null;
        RenderedOp op = (RenderedOp) image;

        if ("ImageRead".equals(op.getOperationName())) return op;

        // image mosaic can build a useless mosaic at the end of the chain, check if
        // that's the case (a change to image mosaic or image worker is deemed too risky,
        // would affect code paths of most protocols).
        if ("Mosaic".equals(op.getOperationName()) && canIgnoreMosaic(op)) {
            PlanarImage source = op.getSourceImage(0);
            if (!(source instanceof RenderedOp)) return null;
            // recurse, there might be more than one ignorable mosaic op
            return getImageRead(source);
        }

        LOGGER.fine(
                () ->
                        "Skipping direct download, the final raster is not a direct read from source: "
                                + op.getOperationName());
        return null;
    }

    /**
     * Checks if a mosaic operation can be skipped, as it's returning the same image as its input
     */
    private boolean canIgnoreMosaic(RenderedOp op) {
        // mosaic with just one input?
        if (op.getNumSources() != 1) return false;
        PlanarImage source = op.getSourceImage(0);

        // same image layout?
        if (!new ImageLayout(op).equals(new ImageLayout(source))) return false;

        // same nodata?
        Object sourceNoData = source.getProperty(NoDataContainer.GC_NODATA);
        if (!Objects.equals(sourceNoData, op.getProperty(NoDataContainer.GC_NODATA))) return false;

        // check what the mosaic has been instructed to do
        ParameterBlock pb = op.getParameterBlock();
        ROI[] rois = (ROI[]) pb.getObjectParameter(2);
        if (rois != null && rois.length == 1 && !isFullROI(rois[0], source)) return false;

        // TODO check also background and threshold?

        return true;
    }

    private boolean isFullROI(ROI roi, PlanarImage source) {
        // if the ROI is smaller, it cannot contain
        if (!roi.getBounds().contains(source.getBounds())) return false;

        // quick check for ROIGeometry, rectangular geometry covering the whole source iamge
        if (roi instanceof ROIGeometry) {
            Geometry g = ((ROIGeometry) roi).getAsGeometry();
            if (g.isRectangle()
                    && JTS.toRectangle2D(g.getEnvelopeInternal()).equals(source.getBounds()))
                return true;
        }

        // slightly longer check, getting the minimum of the ROI image
        return new ImageWorker(roi.getAsImage()).getMinimums()[0] > 0;
    }
}
