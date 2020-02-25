/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import java.io.File;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

public class GeoTiffGetCoverageTest extends WCSTestSupport {

    @Test
    public void testGeotiffExtensionCompressionJPEGWrongQuality1() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/geotiff/geotiffExtensionCompressionJPEGWrongQuality1.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "105");
    }

    @Test
    public void testGeotiffExtensionCompressionJPEGWrongQuality2() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/geotiff/geotiffExtensionCompressionJPEGWrongQuality2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "0");
    }

    @Test
    public void testGeotiffExtensionCompressionLZW() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionLZW.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

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
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
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
    public void testGeotiffExtensionCompressionDeflate() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionDeflate.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // compression
        TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
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
    @Ignore // TODO
    public void testGeotiffExtensionCompressionHuffman() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionHuffman.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

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
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
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
    @Ignore
    public void testGeotiffExtensionCompressionPackBits() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionPackBits.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

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
        //        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
        //
        // (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));
        //        System.out.println(IIOMetadataDumper.getMetadata());
        assertNotNull(metadata);
        IIOMetadataNode root =
                (IIOMetadataNode)
                        reader.getImageMetadata(0)
                                .getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals(
                "PackBits",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "32773",
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
    public void testGeotiffExtensionCompressionWrongCompression() throws Exception {
        final File xml =
                new File(
                        "./src/test/resources/geotiff/geotiffExtensionCompressionWrongCompression.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(response, 404, WcsExceptionCode.CompressionInvalid.toString(), "OUCH");
    }

    @Test
    public void testGeotiffExtensionCompressionJPEG() throws Exception {
        final File xml =
                new File("./src/test/resources/geotiff/geotiffExtensionCompressionJPEG.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

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
                "JPEG",
                field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals(
                "7", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionTilingDefault() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTilingDefault.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader =
                (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));

        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(368, reader.getTileHeight(0));
        assertEquals(368, reader.getTileWidth(0));

        IIOMetadataNode node =
                ((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals(
                "PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionTilingWrong1() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTilingWrong1.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "13");
    }

    @Test
    public void testGeotiffExtensionTilingWrong2() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTilingWrong2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "25");
    }

    @Test
    public void testGeotiffExtensionTiling() throws Exception {
        final File xml = new File("./src/test/resources/geotiff/geotiffExtensionTiling.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
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

        // clean up
        reader.dispose();
    }

    @Test
    public void testGeotiffExtensionBanded() throws Exception {

        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wcs:GetCoverage\n"
                        + "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n"
                        + "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n"
                        + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
                        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n"
                        + "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd\"\n"
                        + "  service=\"WCS\"\n"
                        + "  version=\"2.0.1\">\n"
                        + "  <wcs:Extension>\n"
                        + "    <wcsgeotiff:compression>None</wcsgeotiff:compression>\n"
                        + "    <wcsgeotiff:interleave>band</wcsgeotiff:interleave>\n"
                        + "  </wcs:Extension>\n"
                        + "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n"
                        + "  <wcs:format>image/tiff</wcs:format>\n"
                        + "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        // TODO Fix this test
        //        byte[] tiffContents = getBinary(response);
        //        File file = File.createTempFile("exception", "xml", new File("./target"));
        //        FileUtils.writeByteArrayToFile(file, tiffContents);
        //
        //        String ex=FileUtils.readFileToString(file);
    }
}
