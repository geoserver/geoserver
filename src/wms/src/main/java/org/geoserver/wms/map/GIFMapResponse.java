/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.RenderedImageList;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geotools.image.ImageWorker;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.util.logging.Logging;

import com.sun.media.imageioimpl.plugins.gif.GIFImageWriter;
import com.sun.media.imageioimpl.plugins.gif.GIFImageWriterSpi;

/**
 * Handles a GetMap request that spects a map in GIF format.
 * 
 * @author Didier Richard
 * @author Simone Giannecchini - GeoSolutions
 * @author Alessio Fabiani - GeoSolutions
 * @version $Id
 */
public final class GIFMapResponse extends RenderedImageMapResponse {

    private final static Logger LOGGER = Logging.getLogger(GIFMapResponse.class);

    private static final String IMAGE_GIF_SUBTYPE_ANIMATED = "image/gif;subtype=animated";

    private static final GIFImageWriterSpi ORIGINATING_PROVIDER = new GIFImageWriterSpi();

    /** the only MIME type this map producer supports */
    static final String MIME_TYPE = "image/gif";

    static final String[] OUTPUT_FORMATS = {MIME_TYPE, IMAGE_GIF_SUBTYPE_ANIMATED };

    /**
     * Default capabilities for GIF .
     * 
     * <p>
     * <ol>
     * <li>tiled = supported</li>
     * <li>multipleValues = unsupported</li>
     * <li>paletteSupported = supported</li>
     * <li>transparency = supported</li>
     * </ol>
     * 
     * <p>
     * We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES = new MapProducerCapabilities(true, false,
            true, true, MIME_TYPE);

    /**
     * Default capabilities for GIF animated.
     * 
     * <p>
     * <ol>
     * <li>tiled = supported</li>
     * <li>multipleValues = supported</li>
     * <li>paletteSupported = supported</li>
     * <li>transparency = supported</li>
     * </ol>
     * 
     * <p>
     * We should soon support multipage tiff.
     */
    private static MapProducerCapabilities CAPABILITIES_ANIM = new MapProducerCapabilities(true,
            true, true, true, MIME_TYPE);

    public GIFMapResponse(WMS wms) {
        super(OUTPUT_FORMATS, wms);
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     * 
     * @param image The image to be formatted.
     * @param outStream The stream to write to.
     * 
     * @throws ServiceException not really.
     * @throws IOException if encoding to <code>outStream</code> fails.
     */
    @Override
    public void formatImageOutputStream(RenderedImage originalImage, OutputStream outStream,
            WMSMapContext mapContent) throws ServiceException, IOException {

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
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("Preparing to write a gif...");
            //
            // Now the magic
            //
            try {
                InverseColorMapOp paletteInverter = mapContent.getPaletteInverter();
                new ImageWorker(super.forceIndexed8Bitmask(originalImage, paletteInverter))
                        .writeGIF(outStream, "LZW", 0.75f);
            } catch (IOException e) {
                throw new ServiceException(e);
            }
            return;
        }

        // check the number is >1
        if (numfiles <= 0)
            throw new ServiceException("The number of frames for this GIF is less than 1");

        final GIFImageWriter gifWriter = new GIFImageWriter(ORIGINATING_PROVIDER);
        // write param
        final ImageWriteParam param = gifWriter.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionType("LZW");
        param.setCompressionQuality(0.75f);

        ImageOutputStream otStream = null;
        try {
            otStream = ImageIO.createImageOutputStream(outStream);
            gifWriter.setOutput(otStream);
            gifWriter.prepareWriteSequence(null);

            // gif params
            final GetMapRequest request = (GetMapRequest) mapContent.getRequest();
            final Boolean loopContinuosly = (request.getFormatOptions().get("gif_loop_continuosly") != null ?
                    Boolean.valueOf((String)request.getFormatOptions().get("gif_loop_continuosly")) : wms.getLoopContinuously());
            final Integer delay = (request.getFormatOptions().get("gif_frames_delay") != null ? 
                    Integer.valueOf((String) request.getFormatOptions().get("gif_frames_delay")) : wms.getFramesDelay());

            // check value
            if (delay <= 0)
                throw new ServiceException("Animate GIF delay invalid: " + delay);

            //
            // Getting input files
            //

            for (int i = 0; i < numfiles; i++) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.fine("Writing image " + i);
                // get the image
                RenderedImage ri = (RenderedImage) ril.get(i);
                // convert it to gif compatible
                InverseColorMapOp paletteInverter = mapContent.getPaletteInverter();
                ri = super.forceIndexed8Bitmask(ri, paletteInverter);
                if (ri != null) {

                    // prepare metadata and write param
                    final IIOMetadata imageMetadata = gifWriter.getDefaultImageMetadata(
                            new ImageTypeSpecifier(ri), param);
                    prepareMetadata(imageMetadata, loopContinuosly, delay);

                    // write
                    gifWriter.writeToSequence(new IIOImage(ri, null, imageMetadata), param);

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

        }

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Done writing animated gif");
    }

    public String getContentDisposition() {
        // can be null
        return null;
    }

    /**
     * Prepare imagemetadata for writing an animated GIF.
     * <p>
     * This process involves setting the continuos looping mode as well the delay between frames
     * 
     * @param imageMetadata original {@link IIOMetadata} instance to modify.
     * @param loopContinuously <code>yes</code> in case we want to loop continuosly,
     *        <code>false</code> otherwise.
     * @param timeBetweenFramesMS the delay in ms between two frames when looping.
     * @throws IOException in case an error occurs.
     */
    private void prepareMetadata(IIOMetadata imageMetadata, boolean loopContinuously,
            int timeBetweenFramesMS) throws IOException {

        String metaFormatName = imageMetadata.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode) imageMetadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer
                .toString(timeBetweenFramesMS / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
        commentsNode.setAttribute("CommentExtension", "Created by MAH");

        IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loop = loopContinuously ? 0 : 1;

        child.setUserObject(new byte[] { 0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF) });
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
        return /* CAPABILITIES */CAPABILITIES_ANIM;
    }

}
