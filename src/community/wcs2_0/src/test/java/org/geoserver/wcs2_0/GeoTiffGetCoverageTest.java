package org.geoserver.wcs2_0;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.io.File;

import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.junit.Ignore;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Node;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoTiffGetCoverageTest extends WCSTestSupport {

    @Test 
    public void testGeotiffExtensionCompressionJPEGWrongQuality1() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>JPEG</wcsgeotiff:compression>\n" + 
                        "    <wcsgeotiff:jpeg_quality>105</wcsgeotiff:jpeg_quality>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/xml", response.getContentType());
        // TODO Fix this test
//        byte[] tiffContents = getBinary(response);
//        File file = File.createTempFile("exception", "xml", new File("./target"));
//        FileUtils.writeByteArrayToFile(file, tiffContents);
//        
//        String ex=FileUtils.readFileToString(file);

     }

    @Test 
    public void testGeotiffExtensionCompressionJPEGWrongQuality2() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>JPEG</wcsgeotiff:compression>\n" + 
                        "    <wcsgeotiff:jpeg_quality>0</wcsgeotiff:jpeg_quality>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/xml", response.getContentType());
        // TODO Fix this test
//        byte[] tiffContents = getBinary(response);
//        File file = File.createTempFile("exception", "xml", new File("./target"));
//        FileUtils.writeByteArrayToFile(file, tiffContents);
//        
//        String ex=FileUtils.readFileToString(file);

     }
    
    @Test 
    public void testGeotiffExtensionCompressionLZW() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>LZW</wcsgeotiff:compression>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));       
        
        // compression
        final TIFFImageMetadata metadata=(TIFFImageMetadata) reader.getImageMetadata(0);
//        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
//                (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));        
//        System.out.println(IIOMetadataDumper.getMetadata());        
        assertNotNull(metadata);
        IIOMetadataNode root = (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals("LZW", field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals("5", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();
     }

    @Test 
    public void testGeotiffExtensionCompressionDEFLATE() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>DEFLATE</wcsgeotiff:compression>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));       
        
        // compression
        TIFFImageMetadata metadata=(TIFFImageMetadata) reader.getImageMetadata(0);
//        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
//                (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));        
//        System.out.println(IIOMetadataDumper.getMetadata());        
        assertNotNull(metadata);
        IIOMetadataNode root = (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals("Deflate", field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals("32946", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());
        
        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();

     }
    
    @Test 
    @Ignore // TODO
    public void testGeotiffExtensionCompressionHuffman() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>Huffman</wcsgeotiff:compression>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));       
        
        // compression
        final TIFFImageMetadata metadata=(TIFFImageMetadata) reader.getImageMetadata(0);
//        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
//                (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));        
//        System.out.println(IIOMetadataDumper.getMetadata());        
        assertNotNull(metadata);
        IIOMetadataNode root = (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals("Deflate", field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals("32946", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();
     }
    
    @Test 
    @Ignore
    public void testGeotiffExtensionCompressionPackBits() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>PackBits</wcsgeotiff:compression>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));       
        
        // compression
        final TIFFImageMetadata metadata=(TIFFImageMetadata) reader.getImageMetadata(0);
//        IIOMetadataDumper IIOMetadataDumper = new IIOMetadataDumper(
//                (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName));        
//        System.out.println(IIOMetadataDumper.getMetadata());        
        assertNotNull(metadata);
        IIOMetadataNode root = (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals("PackBits", field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals("32773", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());

        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();
     }
    
    @Test 
    public void testGeotiffExtensionCompressionWrongCompression() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>OUCH</wcsgeotiff:compression>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        checkOws20Exception(response, 404, WcsExceptionCode.CompressionNotSupported.toString(), "compression");
     }
    
    @Test 
    public void testGeotiffExtensionCompressionJPEG() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>JPEG</wcsgeotiff:compression>\n" + 
                        "    <wcsgeotiff:jpeg_quality>75</wcsgeotiff:jpeg_quality>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));       
        
        // compression
        final TIFFImageMetadata metadata=(TIFFImageMetadata) reader.getImageMetadata(0);
        assertNotNull(metadata);
        IIOMetadataNode root = (IIOMetadataNode)reader.getImageMetadata(0).getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
        IIOMetadataNode field = getTiffField(root, BaselineTIFFTagSet.TAG_COMPRESSION);
        assertNotNull(field);
        assertEquals("JPEG", field.getFirstChild().getFirstChild().getAttributes().item(1).getNodeValue());
        assertEquals("7", field.getFirstChild().getFirstChild().getAttributes().item(0).getNodeValue());
        
        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();
     }
    
    @Test 
    public void testGeotiffExtensionTilingDefault() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:tiling>true</wcsgeotiff:tiling>\n" + // Use default 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
        
        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(512, reader.getTileHeight(0));
        assertEquals(512, reader.getTileWidth(0));
        
        IIOMetadataNode node =((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();
     }
    
    @Test 
    public void testGeotiffExtensionTilingWrong1() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:tiling>true</wcsgeotiff:tiling>\n" + 
                        "    <wcsgeotiff:tileheight>256</wcsgeotiff:tileheight>\n" + 
                        "    <wcsgeotiff:tilewidth>13</wcsgeotiff:tilewidth>\n" + // WRONG!! Must be multiple of 16
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "tilewidth");
     }
    
    @Test 
    public void testGeotiffExtensionTilingWrong2() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                        "<wcs:GetCoverage\n" + 
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
                        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
                        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
                        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
                        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
                        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
                        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
                        "  service=\"WCS\"\n" + 
                        "  version=\"2.0.1\">\n" + 
                        "  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:tiling>true</wcsgeotiff:tiling>\n" + 
                        "    <wcsgeotiff:tileheight>25</wcsgeotiff:tileheight>\n" +  // WRONG!! Must be multiple of 16
                        "    <wcsgeotiff:tilewidth>256</wcsgeotiff:tilewidth>\n" + 
                        "  </wcs:Extension>\n" + 
                        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
                        "  <wcs:format>image/tiff</wcs:format>\n" + 
                        "</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "tileheight");
     }
    
    @Test 
    public void testGeotiffExtensionTiling() throws Exception {
        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        		"<wcs:GetCoverage\n" + 
        		"  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
        		"  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
        		"  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
        		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
        		"  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
        		"  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
        		"  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
        		"  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
        		"  service=\"WCS\"\n" + 
        		"  version=\"2.0.1\">\n" + 
        		"  <wcs:Extension>\n" + 
                        "    <wcsgeotiff:compression>None</wcsgeotiff:compression>\n" + 
        		"    <wcsgeotiff:tiling>true</wcsgeotiff:tiling>\n" + 
        		"    <wcsgeotiff:tileheight>256</wcsgeotiff:tileheight>\n" + 
        		"    <wcsgeotiff:tilewidth>256</wcsgeotiff:tilewidth>\n" + 
        		"  </wcs:Extension>\n" + 
        		"  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
        		"  <wcs:format>image/tiff</wcs:format>\n" + 
        		"</wcs:GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_gtiff.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        // TODO: check the tiff structure is the one requested
        final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
        reader.setInput(new FileImageInputStream(file));
        
        // tiling
        assertTrue(reader.isImageTiled(0));
        assertEquals(256, reader.getTileHeight(0));
        assertEquals(256, reader.getTileWidth(0));
        
        IIOMetadataNode node =((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        // clean up
        reader.dispose();
    }
    
    @Test 
    public void testGeotiffExtensionBanded() throws Exception {

        String request =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<wcs:GetCoverage\n" + 
        "  xmlns:wcs=\"http://www.opengis.net/wcs/2.0\"\n" + 
        "  xmlns:wcsgeotiff=\"http://www.opengis.net/wcs/geotiff/1.0\"\n" + 
        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + 
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
        "  xsi:schemaLocation=\"http://www.opengis.net/wcs/2.0 \n" + 
        "  http://schemas.opengis.net/wcs/2.0/wcsAll.xsd \n" + 
        "  http://www.opengis.net/wcs/geotiff/1.0 \n" + 
        "  http://schemas.opengis.net/wcs/geotiff/1.0/wcsGeotiff.xsd\"\n" + 
        "  service=\"WCS\"\n" + 
        "  version=\"2.0.1\">\n" + 
        "  <wcs:Extension>\n" + 
        "    <wcsgeotiff:compression>None</wcsgeotiff:compression>\n" + 
        "    <wcsgeotiff:interleave>band</wcsgeotiff:interleave>\n" + 
        "  </wcs:Extension>\n" + 
        "  <wcs:CoverageId>wcs__BlueMarble</wcs:CoverageId>\n" + 
        "  <wcs:format>image/tiff</wcs:format>\n" + 
        "</wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/xml", response.getContentType());
        // TODO Fix this test
//        byte[] tiffContents = getBinary(response);
//        File file = File.createTempFile("exception", "xml", new File("./target"));
//        FileUtils.writeByteArrayToFile(file, tiffContents);
//        
//        String ex=FileUtils.readFileToString(file);
    }


    

    // TODO: re-enable when we have subsetting support in GetCoverage
    // @Test
    // public void testBBoxRequest() throws Exception {
    // Document dom = getAsDOM("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=" +
    // getLayerId(TASMANIA_BM) + "&subset=lon(-10,10)&subset=lat(-20,20)");
    // print(dom);
    //
    // // checkFullCapabilitiesDocument(dom);
    // }
    /**
     * Gets a TIFFField node with the given tag number. This is done by searching for a TIFFField
     * with attribute number whose value is the specified tag value.
     * 
     * @param tag DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    private IIOMetadataNode getTiffField(Node rootNode, final int tag) {
        Node node = rootNode.getFirstChild();
        if (node != null){
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
}
