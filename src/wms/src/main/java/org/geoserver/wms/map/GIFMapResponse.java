/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import com.sun.media.imageioimpl.plugins.gif.GIFImageWriter;
import com.sun.media.imageioimpl.plugins.gif.GIFImageWriterSpi;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageList;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.util.logging.Logging;

/**
 * Handles a GetMap request that spects a map in GIF format.
 *
 * @author Didier Richard
 * @author Simone Giannecchini - GeoSolutions
 * @author Alessio Fabiani - GeoSolutions
 * @version $Id
 */
public final class GIFMapResponse extends RenderedImageMapResponse {

    private static final String GIF_FRAMES_DELAY = "gif_frames_delay";

    /** Old name, with typo, kept for backwards compatibility */
    private static final String GIF_LOOP_CONTINUOSLY = "gif_loop_continuosly";

    private static final String GIF_LOOP_CONTINUOUSLY = "gif_loop_continuously";

    private static final String GIF_DISPOSAL_METHOD = "gif_disposal";

    private static final Logger LOGGER = Logging.getLogger(GIFMapResponse.class);

    public static final String IMAGE_GIF_SUBTYPE_ANIMATED = "image/gif;subtype=animated";

    private static final GIFImageWriterSpi ORIGINATING_PROVIDER = new GIFImageWriterSpi();

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/gif";

    static final String[] OUTPUT_FORMATS = {MIME_TYPE, IMAGE_GIF_SUBTYPE_ANIMATED};

    /**
     * Default capabilities for GIF .
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = supported
     *   <li>multipleValues = unsupported
     *   <li>paletteSupported = supported
     *   <li>transparency = supported
     * </ol>
     *
     * <p>We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, false, true, true, MIME_TYPE);

    /**
     * Default capabilities for GIF animated.
     *
     * <p>
     *
     * <ol>
     *   <li>tiled = supported
     *   <li>multipleValues = supported
     *   <li>paletteSupported = supported
     *   <li>transparency = supported
     * </ol>
     *
     * <p>We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES_ANIM =
            new MapProducerCapabilities(true, true, true, true, MIME_TYPE);

    public GIFMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     *
     * @param originalImage The image to be formatted.
     * @param outStream The stream to write to.
     * @throws ServiceException not really.
     * @throws IOException if encoding to <code>outStream</code> fails.
     */
    @Override
    public void formatImageOutputStream(
            RenderedImage originalImage, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Writing gif image ...");
        }

        // get the one required by the MultidimensionalGetMapRequest
        final String format = mapContent.getRequest().getFormat();
        boolean animatedGIF = false;
        if (format.equalsIgnoreCase(IMAGE_GIF_SUBTYPE_ANIMATED)) {
            animatedGIF = true;
        }
        // the original image should always be a list of rendered images unless metatiling is
        // activated
        int numfiles = 1;
        final RenderedImageList ril;
        if (originalImage instanceof RenderedImageList) {
            // convert to list
            ril = (RenderedImageList) originalImage;

            // get number of images
            numfiles = ril.size();
        } else {
            ril = new RenderedImageList(Arrays.asList(originalImage));
        }
        if (numfiles == 1 || !animatedGIF) {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Preparing to write a gif...");
            //
            // Now the magic
            //
            try {
                originalImage = applyPalette(originalImage, mapContent, MIME_TYPE, false);
                ImageWorker iw = new ImageWorker(originalImage);
                iw.writeGIF(outStream, "LZW", 0.75f);
                RasterCleaner.addImage(iw.getRenderedImage());
            } catch (IOException e) {
                throw new ServiceException(e);
            }
            return;
        }

        // check the number is >1
        if (numfiles <= 0) {
            throw new ServiceException("The number of frames for this GIF is less than 1");
        }

        final GIFImageWriter gifWriter = new GIFImageWriter(ORIGINATING_PROVIDER);
        // write param
        final ImageWriteParam param = gifWriter.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType("LZW");
        param.setCompressionQuality(0.75f);

        ImageOutputStream otStream = null;
        List<RenderedImage> images = new ArrayList<RenderedImage>();
        try {
            otStream = ImageIO.createImageOutputStream(outStream);
            gifWriter.setOutput(otStream);
            gifWriter.prepareWriteSequence(null);

            // gif params
            final GetMapRequest request = mapContent.getRequest();
            Object loopContinuouslyString = request.getFormatOptions().get(GIF_LOOP_CONTINUOUSLY);
            if (loopContinuouslyString == null) {
                loopContinuouslyString = request.getFormatOptions().get(GIF_LOOP_CONTINUOSLY);
            }
            final Boolean loopContinuosly =
                    (loopContinuouslyString != null
                            ? Boolean.valueOf((String) loopContinuouslyString)
                            : wms.getLoopContinuously());
            Object frameDelayString = request.getFormatOptions().get(GIF_FRAMES_DELAY);
            final Integer delay =
                    (frameDelayString != null
                            ? Integer.valueOf((String) frameDelayString)
                            : wms.getFramesDelay());
            final String requestedDisposalMethod =
                    (String) request.getFormatOptions().get(GIF_DISPOSAL_METHOD);
            String disposalMethod = wms.getDisposalMethod();
            if (requestedDisposalMethod != null) {
                for (String method : WMS.DISPOSAL_METHODS) {
                    if (method.equalsIgnoreCase(requestedDisposalMethod.trim())) {
                        disposalMethod = method;
                    }
                }
            }
            // assign the proper well-known value for disposal method option
            if (disposalMethod == "backgroundColor") {
                disposalMethod = "restoreToBackgroundColor";
            } else if (disposalMethod == "previous") {
                disposalMethod = "restoreToPrevious";
            }

            // check value
            if (delay <= 0) throw new ServiceException("Animate GIF delay invalid: " + delay);

            //
            // Getting input files
            //
            for (int i = 0; i < numfiles; i++) {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Writing image " + i);
                // get the image
                RenderedImage ri = (RenderedImage) ril.get(i);
                // convert it to gif compatible
                ri = applyPalette(ri, mapContent, MIME_TYPE, false);
                if (ri != null) {
                    // prepare metadata and write param
                    final IIOMetadata imageMetadata =
                            gifWriter.getDefaultImageMetadata(new ImageTypeSpecifier(ri), param);
                    prepareMetadata(ri, imageMetadata, loopContinuosly, delay, disposalMethod);

                    // write
                    gifWriter.writeToSequence(new IIOImage(ri, null, imageMetadata), param);
                    images.add(ri);
                }
            }

            // close writing sequence
            gifWriter.endWriteSequence();

        } catch (IOException e) {
            throw new ServiceException(e);
        } finally {
            try {
                otStream.flush();
            } catch (Exception e) {
                // swallow
            }

            try {
                otStream.close();
            } catch (Exception e) {
                // swallow
            }

            try {
                gifWriter.dispose();
            } catch (Exception e) {
                // swallow
            }

            // let go of the image chain as soon as possible to free memory
            for (RenderedImage image : images) {
                if (image instanceof PlanarImage) {
                    ImageUtilities.disposePlanarImageChain((PlanarImage) image);
                } else if (image instanceof BufferedImage) {
                    ((BufferedImage) image).flush();
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Done writing animated gif");
    }

    public String getContentDisposition() {
        // can be null
        return null;
    }

    /**
     * Prepare imagemetadata for writing an animated GIF.
     *
     * <p>This process involves setting the continuos looping mode as well the delay between frames
     *
     * @param ri The {@link RenderedImage} for which we are setting metadata.
     * @param imageMetadata original {@link IIOMetadata} instance to modify.
     * @param loopContinuously <code>yes</code> in case we want to loop continuosly, <code>false
     *     </code> otherwise.
     * @param timeBetweenFramesMS the delay in ms between two frames when looping.
     * @param disposalMethod the disposal method for this image.
     * @throws IOException in case an error occurs.
     */
    private static void prepareMetadata(
            RenderedImage ri,
            IIOMetadata imageMetadata,
            boolean loopContinuously,
            int timeBetweenFramesMS,
            String disposalMethod)
            throws IOException {

        String metaFormatName = imageMetadata.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode) imageMetadata.getAsTree(metaFormatName);
        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", disposalMethod);
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute(
                "delayTime", Integer.toString(timeBetweenFramesMS / 10));

        // transparency support
        final IndexColorModel icm = (IndexColorModel) ri.getColorModel();
        int transparentColorIndex = -1;

        if (icm.getTransparency() == Transparency.BITMASK
                && (transparentColorIndex = icm.getTransparentPixel()) >= 0) {
            graphicsControlExtensionNode.setAttribute(
                    "transparentColorIndex", String.valueOf(transparentColorIndex));
            graphicsControlExtensionNode.setAttribute("transparentColorFlag", "TRUE");
        } else {
            graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
            graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        }

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by MAH");

        IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loop = loopContinuously ? 0 : 1;
        final byte[] userObject =
                new byte[] {0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF)};
        child.setUserObject(userObject);
        appEntensionsNode.appendChild(child);

        imageMetadata.setFromTree(metaFormatName, root);
    }

    /**
     * Returns an existing child node, or creates and returns a new child node (if the requested
     * node does not exist).
     *
     * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
     * @param nodeName the name of the child node.
     * @return the child node, if found or a new node created with the given name.
     */
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
                return ((IIOMetadataNode) rootNode.item(i));
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return (node);
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        if (IMAGE_GIF_SUBTYPE_ANIMATED.equals(outputFormat)) {
            return CAPABILITIES_ANIM;
        } else {
            return CAPABILITIES;
        }
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        return "gif";
    }
}
