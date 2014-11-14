package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

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
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.coverage.CoverageUtilities;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoTiffKvpTest extends WCSKVPTestSupport {

    @Test
    public void extensionGeotiff() throws Exception {
        // complete
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
        		"&coverageId=theCoverage&compression=JPEG&jpeg_quality=75&predictor=None" +
        		"&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
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
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=-2&predictor=None" +
                        "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "-2");
        
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=101&predictor=None" +
                "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "101");
        
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=101&predictor=aaa" +
                "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.JpegQualityInvalid.toString(), "101");
    }
    
    @Test
    public void jpeg() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=75");
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        checkJpegTiff(tiffContents);
    }
    
    @Test
    public void jpegMediaType() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=JPEG&jpeg_quality=75&mediaType=multipart/related");
        
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
        assertXpathEvaluatesTo("fileReference", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:arcrole", gml);
        assertXpathEvaluatesTo("cid:/coverages/wcs__BlueMarble.tif", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:href", gml);
        assertXpathEvaluatesTo("http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage", "//gml:rangeSet/gml:File/gml:rangeParameters/@xlink:role", gml);
        assertXpathEvaluatesTo("cid:/coverages/wcs__BlueMarble.tif", "//gml:rangeSet/gml:File/gml:fileReference", gml);
        assertXpathEvaluatesTo("image/tiff", "//gml:rangeSet/gml:File/gml:mimeType", gml);
        
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("/coverages/wcs__BlueMarble.tif", coveragePart.getHeader("Content-ID")[0]);
        assertEquals("image/tiff", coveragePart.getContentType());

        // make sure we can read the coverage back and perform checks on it
        byte[] tiffContents = IOUtils.toByteArray(coveragePart.getInputStream());
        checkJpegTiff(tiffContents);
    }

    private void checkJpegTiff(byte[] tiffContents) throws IOException,
            FileNotFoundException, IIOException {
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
    public void interleaving() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&interleave=pixel");
        
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
        
        IIOMetadataNode node = metadata.getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        
        // clean up
        reader.dispose();
        
        //unsupported or wrong
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble&interleave=band");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.InterleavingNotSupported.toString(), "band");      
        
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
        "&coverageId=wcs__BlueMarble&interleave=asds");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.InterleavingInvalid.toString(), "asds");    
    }
    
    @Test
    public void deflate() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=DEFLATE");
        
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
    public void lzw() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=LZW&jpeg_quality=75");
        
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
    public void tiling() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
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

        
        // wrong values
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble" +
                "&interleave=pixel&tiling=true&tileheight=13&tilewidth=256");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "13");
        
        
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__BlueMarble" +
                "&interleave=pixel&tiling=true&tileheight=13&tilewidth=11");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.TilingInvalid.toString(), "11");
        
        // default
        response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
        "&coverageId=wcs__BlueMarble&tiling=true");

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
        
        node =((TIFFImageMetadata) reader.getImageMetadata(0)).getStandardDataNode();
        assertNotNull(node);
        assertEquals("PlanarConfiguration", node.getFirstChild().getNodeName());
        assertEquals("PixelInterleaved", node.getFirstChild().getAttributes().item(0).getNodeValue());
        
        // clean up
        reader.dispose();        
    }
    
    @Test
    public void wrongCompression() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
                        "&coverageId=wcs__BlueMarble&compression=aaaG&predictor=None" +
                        "&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(response, 404, WcsExceptionCode.CompressionInvalid.toString(), "aaaG");
    }
    
    
    @Test
    public void getFullCoverageKVP() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null, sourceCoverage=null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getGridGeometry().getGridRange(), targetCoverage.getGridGeometry().getGridRange());
            assertEquals(sourceCoverage.getCoordinateReferenceSystem(), targetCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), targetCoverage.getEnvelope());
        } finally {
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
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
        final boolean oldLatLon=wcsInfo.isLatLon();
        wcsInfo.setLatLon(true);
        getGeoServer().save(wcsInfo);
        
        // execute
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");
        
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = new File("./target/bm_full.tiff");
        FileUtils.writeByteArrayToFile(file, tiffContents);
        
        final Hints hints= new Hints();
        hints.put(Hints.FORCE_AXIS_ORDER_HONORING, "EPSG");
        hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.FALSE);
        GeoTiffReader readerTarget = new GeoTiffReader(file,hints);
        GridCoverage2D targetCoverage = null, sourceCoverage=null;
        try {
            targetCoverage = readerTarget.read(null);
            sourceCoverage=(GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble").getGridCoverageReader(null, null).read(null);
            
            // checks
            assertEquals(sourceCoverage.getGridGeometry().getGridRange(), targetCoverage.getGridGeometry().getGridRange());
            assertEquals(CRS.getAxisOrder(targetCoverage.getCoordinateReferenceSystem()),AxisOrder.NORTH_EAST);
            
            final GeneralEnvelope transformedEnvelope=CRS.transform((AffineTransform2D)CoverageUtilities.AXES_SWAP,targetCoverage.getEnvelope());
            transformedEnvelope.setCoordinateReferenceSystem(sourceCoverage.getCoordinateReferenceSystem());
            assertEquals(sourceCoverage.getEnvelope(), transformedEnvelope);
        } finally {
            // reinforce old settings
            wcsInfo.setLatLon(oldLatLon);
            getGeoServer().save(wcsInfo);
            
            try{
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
            try{
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
        MockHttpServletResponse response = getAsServletResponse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=wcs__BlueMarble");

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
            sourceCoverage = (GridCoverage2D) this.getCatalog().getCoverageByName("BlueMarble")
                    .getGridCoverageReader(null, null).read(null);

            // checks
            assertEquals(sourceCoverage.getGridGeometry().getGridRange(), targetCoverage
                    .getGridGeometry().getGridRange());
            assertEquals(CRS.getAxisOrder(targetCoverage.getCoordinateReferenceSystem()),
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
