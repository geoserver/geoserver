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

    /**
     * This MUST be an environment variable (i.e. not accessible via web ui).
     *
     * <p>Set this environment var to "true" to disable the REJECT_LIST_MIME_TYPES check.
     */
    static final String ENVIRONMENT_VAR_DISABLE_FILETYPES_REJECT_LIST = "GS_DISABLE_FILETYPES_REJECT_LIST";

    static final MediaType ZIP_MEDIA_TYPE = MediaType.parse("application/zip");

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

    /**
     * reject-list of invalid types.
     *
     * <p>NOTE: use #getBaseType() on the type reported by TIKA.
     *
     * <p>i.e. "application/x-msdownload; format=pe32" should be rejected based on the base type
     * "application/x-msdownload"
     */
    static final List<MediaType> REJECT_LIST_MIME_TYPES = List.of(
            MediaType.parse("application/x-msdownload"), // windows: .exe or .dll
            MediaType.parse("application/x-mach-o-universal"), // mac: Universal binary (i.e. application)
            MediaType.parse("application/x-mach-o-dylib"), // mac: dynamic library (i.e. .so)
            MediaType.parse("application/x-executable"), // linux: executables
            MediaType.parse("application/x-elf"), // linux: ELF (exec or lib)
            MediaType.parse("application/x-sharedlib"), // linux: shared lib
            MediaType.parse("application/x-java-archive"), // java: jar
            MediaType.parse("application/x-msi"), // windows: installer
            MediaType.parse("application/x-httpd-jsp"), // java: jsp (note - not robust ident by tika)
            MediaType.parse("application/hta") // windows: HTML app
            );

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
     * validates a SINGLE (not-zip) file to see if it's in the reject-list.
     *
     * <p>use {link validateFileNotInRejectList} if your file might be a zip.
     *
     * <p>NOTE: turn this check off with the ENVIRONMENT_VAR_DISABLE_FILETYPES_REJECT_LIST (set to true).
     *
     * @param stream file (from user)
     * @throws Exception if the file is in the reject-list, or is a .zip file (use validateFileNotInRejectList instead)
     */
    public static void assertSimpleFileNotInRejectList(InputStream stream) throws Exception {
        String disableCheckEnvVarValue = System.getenv(ENVIRONMENT_VAR_DISABLE_FILETYPES_REJECT_LIST);
        if (disableCheckEnvVarValue != null && disableCheckEnvVarValue.equalsIgnoreCase("true")) {
            return; // no check
        }

        // does this stream support mark/rest (required by tika)
        try {
            stream.mark(10);
            stream.reset();
        } catch (IOException e) {
            stream = new BufferedInputStream(stream); // wrap
        }

        MediaType detectedMediaType = tika.getDetector().detect(stream, new Metadata());
        if (REJECT_LIST_MIME_TYPES.contains(detectedMediaType.getBaseType())) {
            throw new IOException("Unsupported media type: " + detectedMediaType);
        }

        if (detectedMediaType.getBaseType().equals(ZIP_MEDIA_TYPE)) {
            throw new IOException(
                    "assertSimpleFileNotInRejectList: was given a .zip file.  Use validateFileNotInRejectList instead");
        }
        // not in reject-list (good)
    }

    /**
     * If this is a simple (non-zip) file, then check that this file isn't in the reject-list.
     *
     * <p>If it is a zip file, check each file inside the zip to see if any of them are in the reject-list.
     *
     * <p>see {link assertSimpleFileNotInRejectList}.
     *
     * <p>NOTE: turn this check off with the ENVIRONMENT_VAR_DISABLE_FILETYPES_REJECT_LIST (set to true).
     *
     * @param stream file from user (could be .zip)
     * @throws Exception either file is in the reject-list, or if it's a .zip file and a contained file is in the
     *     reject-list
     */
    public static void validateFileNotInRejectList(InputStream stream) throws Exception {
        String disableCheckEnvVarValue = System.getenv(ENVIRONMENT_VAR_DISABLE_FILETYPES_REJECT_LIST);
        if (disableCheckEnvVarValue != null && disableCheckEnvVarValue.equalsIgnoreCase("true")) {
            return; // no check
        }

        // does this stream support mark/rest (required by tika)
        try {
            stream.mark(10);
            stream.reset();
        } catch (IOException e) {
            stream = new BufferedInputStream(stream); // wrap
        }

        MediaType detectedMediaType = tika.getDetector().detect(stream, new Metadata());
        if (!detectedMediaType.getBaseType().equals(ZIP_MEDIA_TYPE)) {
            // single file
            assertSimpleFileNotInRejectList(stream);
            return;
        }

        // this is a .zip file.  Look at all the files in the zip (non-recursive)
        try (java.util.zip.ZipInputStream zipStream = new java.util.zip.ZipInputStream(stream)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                assertSimpleFileNotInRejectList(zipStream);
                zipStream.closeEntry();
            }
        }
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
