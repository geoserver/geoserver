/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import javax.imageio.IIOException;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import net.opengis.wcs20.GetCoverageType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Document;

public class GeoTiffKvpTest extends WCSKVPTestSupport {

    static class TiffTagTest {
        String tagString;
        String tagValue;

        public TiffTagTest(String tagString, String tagValue) {
            this.tagString = tagString;
            this.tagValue = tagValue;
        }
    }

    static final TiffTagTest JPEG_TAG =
            new TiffTagTest("JPEG", Integer.toString(BaselineTIFFTagSet.COMPRESSION_JPEG));

    static final TiffTagTest DEFLATE_TAG =
            new TiffTagTest("Deflate", Integer.toString(BaselineTIFFTagSet.COMPRESSION_DEFLATE));

    @Test
    public void extensionGeotiff() throws Exception {
        // complete
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&compression=JPEG&jpeg_quality=75&predictor=None"
                                + "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals("JPEG", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:compression"));
        assertEquals("75", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:jpeg_quality"));
        assertEquals("None", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:predictor"));
        assertEquals("pixel", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:interleave"));
        assertEquals("true", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tiling"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tileheight"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tilewidth"));
    }

    @Test
    public void extensionGeotiff2() throws Exception {
        String request =
                "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                        + "&coverageId=wcs__BlueMarble&compression=Deflate"
                        + "&interleave=Pixel&tiling=true&tileheight=256&tilewidth=256";
        GetCoverageType gc = parse(request);

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals(
                "Deflate", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:compression"));
        assertEquals("Pixel", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:interleave"));
        assertEquals("true", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tiling"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tileheight"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tilewidth"));
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        checkTiff(tiffContents, DEFLATE_TAG);
    }

    @Test
    public void extensionGeotiffPrefixed() throws Exception {
        // complete
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=theCoverage&geotiff:compression=JPEG&geotiff:jpeg_quality=75&geotiff:predictor=None"
                                + "&geotiff:interleave=pixel&geotiff:tiling=true&geotiff:tileheight=256&geotiff:tilewidth=256");

        Map<String, Object> extensions = getExtensionsMap(gc);

        assertEquals("JPEG", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:compression"));
        assertEquals("75", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:jpeg_quality"));
        assertEquals("None", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:predictor"));
        assertEquals("pixel", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:interleave"));
        assertEquals("true", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tiling"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tileheight"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tilewidth"));
    }

    @Test
    public void wrongJPEGQuality() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=-2&predictor=None"
                                + "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "-2");

        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=101&predictor=None"
                                + "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "101");

        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=101&predictor=aaa"
                                + "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "101");
    }

    @Test
    public void jpeg() throws Exception {
        jpeg(false);
    }

    @Test
    public void jpegPrefix() throws Exception {
        jpeg(true);
    }

    private void jpeg(boolean prefix) throws Exception {
        String params = "compression=JPEG&jpeg_quality=75";
        if (prefix) {
            params = "geotiff:compression=JPEG&geotiff:jpeg_quality=75";
        }
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&"
                                + params);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        checkTiff(tiffContents, JPEG_TAG);
    }

    @Test
    public void jpegMediaType() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=75&mediaType=multipart/related");

        assertEquals("multipart/related", response.getContentType());

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart xmlPart = multipart.getBodyPart(0);
        assertEquals("application/gml+xml", xmlPart.getHeader("Content-Type")[0]);
        assertEquals("wcs", xmlPart.getHeader("Content-ID")[0]);
        Document gml = dom(xmlPart.getInputStream());
        // print(gml);

        // check the gml part refers to the file as its range
        assertXpathEvaluatesTo(
                "fileReference", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:arcrole", gml);
        assertXpathEvaluatesTo(
                "cid:/coverages/wcs__BlueMarble.tif",
                "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:href",
                gml);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage",
                "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:role",
                gml);
        assertXpathEvaluatesTo(
                "cid:/coverages/wcs__BlueMarble.tif",
                "//gml:rangeSet/gml:File/gml:fileReference",
                gml);
        assertXpathEvaluatesTo("image/tiff", "//gml:rangeSet/gml:File/gml:mimeType", gml);

        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("/coverages/wcs__BlueMarble.tif", coveragePart.getHeader("Content-ID")[0]);
        assertEquals("image/tiff", coveragePart.getContentType());

        // make sure we can read the coverage back and perform checks on it
        byte[] tiffContents = IOUtils.toByteArray(coveragePart.getInputStream());
        checkTiff(tiffContents, JPEG_TAG);
    }

    private void checkTiff(byte[] tiffContents, TiffTagTest tiffTagTest)
            throws IOException, FileNotFoundException, IIOException {
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                tiffTagTest.tagString,
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                tiffTagTest.tagValue,
                field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void interleaving() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&interleave=pixel");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        assertNotNull(metadata);

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();

        // unsupported or wrong
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&interleave=band");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WcsExceptionCode.InterleavingNotSupported.toString(), "band");

        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&interleave=asds");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.InterleavingInvalid.toString(), "asds");
    }

    @Test
    public void deflate() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=DEFLATE");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "Deflate",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "32946",
                field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void lzw() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=LZW&jpeg_quality=75");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        final TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "LZW",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "5", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void tiling() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&tiling=true&tileheight=256&tilewidth=256");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(256, reader.getTileHeight(0));
        assertEquals(256, reader.getTileWidth(0));

        IIOMetadataNode node =
                ((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // wrong values
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble"
                                + "&interleave=pixel&tiling=true&tileheight=13&tilewidth=256");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "13");

        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble"
                                + "&interleave=pixel&tiling=true&tileheight=13&tilewidth=11");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "11");

        // default
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&tiling=true");

        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        reader.setInput(new FileImageInputStream(file));

        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(368, reader.getTileHeight(0));
        assertEquals(368, reader.getTileWidth(0));

        node = ((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void overviewPolicy() throws Exception {
        // //
        // Different tests reading data to create a coverage which is half the size of the original
        // one
        // across X and Y
        // //
        MockHttpServletResponse response = null;
        byte[] tiffContents = null;

        // Reading native resolution
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&overviewPolicy=IGNORE&scalesize=http://www.opengis.net/def/axis/OGC/1/i(180),"
                                + "http://www.opengis.net/def/axis/OGC/1/j(180)");

        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        File fileNative = File.createTempFile("native", "native.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(fileNative, tiffContents);

        // Reading using overview and TargetSize
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&overviewPolicy=NEAREST&scalesize=http://www.opengis.net/def/axis/OGC/1/i(180),"
                                + "http://www.opengis.net/def/axis/OGC/1/j(180)");
        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        File fileOverviewTS =
                File.createTempFile("overviewTS", "overviewTS.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(fileOverviewTS, tiffContents);

        // Reading using overview and ScaleFactor
        response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&overviewPolicy=NEAREST&SCALEFACTOR=0.5");
        assertEquals("image/tiff", response.getContentType());
        tiffContents = getBinary(response);
        File fileOverviewSF =
                File.createTempFile("overviewSF", "overviewSF.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(fileOverviewSF, tiffContents);

        TIFFImageReaderSpi spi = new TIFFImageReaderSpi();
        TIFFImageReader readerNative = null;
        TIFFImageReader readerOverviewTS = null;
        TIFFImageReader readerOverviewSF = null;
        FileImageInputStream streamNative = null;
        FileImageInputStream streamOverviewTS = null;
        FileImageInputStream streamOverviewSF = null;
        try {
            streamNative = new FileImageInputStream(fileNative);
            readerNative = (TIFFImageReader) spi.createReaderInstance();
            readerNative.setInput(streamNative);

            streamOverviewTS = new FileImageInputStream(fileOverviewTS);
            readerOverviewTS = (TIFFImageReader) spi.createReaderInstance();
            readerOverviewTS.setInput(streamOverviewTS);

            streamOverviewSF = new FileImageInputStream(fileOverviewSF);
            readerOverviewSF = (TIFFImageReader) spi.createReaderInstance();
            readerOverviewSF.setInput(streamOverviewSF);

            // Reading back first image related to native request
            RenderedImage riNative = readerNative.read(0);
            Raster rasterNative = riNative.getData();
            assertEquals(180, rasterNative.getWidth());
            assertEquals(180, rasterNative.getHeight());

            final int refX = 11;
            final int refY = 65;
            final int r1 = rasterNative.getSample(refX, refY, 0);
            final int g1 = rasterNative.getSample(refX, refY, 1);
            final int b1 = rasterNative.getSample(refX, refY, 2);

            // Reading back second image related to request using overviews
            final RenderedImage riOverviewTS = readerOverviewTS.read(0);
            final Raster rasterOverviewTS = riOverviewTS.getData();
            assertEquals(180, rasterOverviewTS.getWidth());
            assertEquals(180, rasterOverviewTS.getHeight());

            final int r2 = rasterOverviewTS.getSample(refX, refY, 0);
            final int g2 = rasterOverviewTS.getSample(refX, refY, 1);
            final int b2 = rasterOverviewTS.getSample(refX, refY, 2);

            // Reading back third image related to request using overviews and scale factor
            final RenderedImage riOverviewSF = readerOverviewSF.read(0);
            final Raster rasterOverviewSF = riOverviewSF.getData();
            assertEquals(180, rasterOverviewSF.getWidth());
            assertEquals(180, rasterOverviewSF.getHeight());

            final int r3 = rasterOverviewSF.getSample(refX, refY, 0);
            final int g3 = rasterOverviewSF.getSample(refX, refY, 1);
            final int b3 = rasterOverviewSF.getSample(refX, refY, 2);

            // Checking the pixels are different
            assertTrue(r1 != r2);
            assertTrue(g1 != g2);
            assertTrue(b1 != b2);

            // Checking the pixels from quality overviews using same layout are equals
            assertEquals(r2, r3);
            assertEquals(g2, g3);
            assertEquals(b2, b3);
        } finally {
            org.geoserver.util.IOUtils.closeQuietly(streamOverviewTS);
            org.geoserver.util.IOUtils.closeQuietly(streamOverviewSF);
            org.geoserver.util.IOUtils.closeQuietly(streamNative);
            if (readerOverviewTS != null) {
                try {
                    readerOverviewTS.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
            if (readerOverviewSF != null) {
                try {
                    readerOverviewSF.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
            if (readerNative != null) {
                try {
                    readerNative.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    @Test
    public void wrongCompression() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&compression=aaaG&predictor=None"
                                + "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.CompressionInvalid.toString(), "aaaG");
    }

    @Test
    public void getFullCoverageKVP() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange(),
                    targetCoverage.getGridGeometry().getGridRange());
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), targetCoverage.getEnvelope());
        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void getFullCoverageLatLon() throws Exception {
        // impose latlon retaining
        final WCSInfo wcsInfo = getWCS();
        final boolean oldLatLon = wcsInfo.isLatLon();
        wcsInfo.setLatLon(true);
        getGeoServer().save(wcsInfo);

        // execute
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final Hints hints = new Hints();
        hints.put(Hints.FORCE_AXIS_ORDER_HONORING, "EPSG");
        hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.FALSE);
        GeoTiffReader readerTarget = new GeoTiffReader(file, hints);
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange(),
                    targetCoverage.getGridGeometry().getGridRange());
            assertEquals(
                    CRS.getAxisOrder(targetCoverage.getCoordinateReferenceSystem()),
                    AxisOrder.NORTH_EAST);

            final GeneralEnvelope transformedEnvelope =
                    CRS.transform(
                            (AffineTransform2D) CoverageUtilities.AXES_SWAP,
                            targetCoverage.getEnvelope());
            transformedEnvelope.setCoordinateReferenceSystem(
                    sourceCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), transformedEnvelope);
        } finally {
            // reinforce old settings
            wcsInfo.setLatLon(oldLatLon);
            getGeoServer().save(wcsInfo);

            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void getFullCoverageLonLat() throws Exception {
        // impose not using latLon retaining
        final WCSInfo wcsInfo = getWCS();
        final boolean oldLatLon = wcsInfo.isLatLon();
        wcsInfo.setLatLon(false);
        getGeoServer().save(wcsInfo);

        // execute
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_fullLonLat.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final Hints hints = new Hints();
        hints.put(Hints.FORCE_AXIS_ORDER_HONORING, "EPSG");
        hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        GeoTiffReader readerTarget = new GeoTiffReader(file, hints);
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);

            // checks
            assertEquals(
                    sourceCoverage.getGridGeometry().getGridRange(),
                    targetCoverage.getGridGeometry().getGridRange());
            assertEquals(
                    CRS.getAxisOrder(targetCoverage.getCoordinateReferenceSystem()),
                    AxisOrder.EAST_NORTH);

            assertEquals(sourceCoverage.getEnvelope(), targetCoverage.getEnvelope());
        } finally {
            // reinforce old settings
            wcsInfo.setLatLon(oldLatLon);
            getGeoServer().save(wcsInfo);

            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
