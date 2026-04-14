/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

/**
 * Utility class for working with file types.
 *
 * <p>This class avoid direct use of Apache Tika and GeoTools when verifying file type (using mime type, file extension,
 * magic, contents ...).
 */
public class FileTypes {

    static final Logger LOGGER = Logging.getLogger(FileTypes.class);

    static TikaConfig tika = null;

    /*
     * only setup TikaConfig once (slow).
     */
    static {
        try {
            tika = new TikaConfig();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize TikaConfig, file checks will not work", e);
        }
    }

    /** allow-list of simple image */
    static final List<MediaType> SIMPLE_IMAGE_MIME_TYPES = List.of(
            MediaType.image("png"),
            MediaType.image("jpeg"),
            MediaType.image("jpg"),
            MediaType.image("bmp"),
            MediaType.image("svg+xml"),
            MediaType.image("gif"));

    /**
     * given an input stream, if it doesn't support mark/reset, wrap it in a BufferedInputStream which does.
     *
     * @param inputStream
     * @return
     */
    public static InputStream wrapIfNotMarkReset(InputStream inputStream) {
        if (inputStream.markSupported()) {
            return inputStream;
        }

        return new BufferedInputStream(inputStream);
    }

    /**
     * Checks that the media type of the stream is a simple image (i.e. png, jpeg, svg+xml) and can be read.
     *
     * <p>stream should support mark/reset. If not, it will be wrapped in BufferedInputStream.
     *
     * @param stream stream containing the image
     * @param validateImage try to parse the image and see if its valid (simple test)
     * @throws IOException problem reading image
     */
    public static void assertSimpleImage(InputStream stream, boolean validateImage) throws Exception {
        // does this support mark/rest (required by tika)
        stream = wrapIfNotMarkReset(stream);

        MediaType detectedMediaType = tika.getDetector().detect(stream, new Metadata());
        if (!SIMPLE_IMAGE_MIME_TYPES.contains(detectedMediaType)) {
            throw new IOException("Unsupported IMAGE media type.  Detected MediaType: " + detectedMediaType);
        }

        if (!validateImage) {
            return;
        }

        // BufferedImage doesn't handle SVG
        if (detectedMediaType == MediaType.image("svg+xml")) {
            assertValidateSvg(stream);
            return; // "good" svg
        }

        // validate the image can be read and it has real dimensions.
        BufferedImage image = ImageIO.read(stream);

        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= 0 || height <= 0) {
            throw new IOException("invalid image dimensions: " + width + " x " + height);
        }
    }

    /**
     * validates an SVG.
     *
     * <p>1. XML should validate 2. main XML tag should be "svg"
     *
     * @param stream stream containing the "SVG" to be validated
     * @throws Exception problem with SVG (do not accept it as "good")
     */
    private static void assertValidateSvg(InputStream stream) throws Exception {
        EntityResolverProvider provider = GeoServerExtensions.bean(EntityResolverProvider.class);

        EntityResolver entityResolver = null;

        if (provider != null) {
            entityResolver = provider.getEntityResolver();
        } else {
            entityResolver = GeoTools.getEntityResolver(new Hints());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(entityResolver);
        Document svgDoc = builder.parse(stream);

        String mainTagName = svgDoc.getDocumentElement().getTagName();
        if (!mainTagName.equals("svg")) {
            throw new Exception("SVG document should start with 'svg'");
        }
    }
}
