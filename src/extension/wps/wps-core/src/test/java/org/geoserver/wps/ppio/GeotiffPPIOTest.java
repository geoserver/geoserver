/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.metadata.IIOMetadataNode;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.resource.GridCoverageReaderResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.process.raster.CropCoverage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

public class GeotiffPPIOTest {

    File geotiff = new File("./target/test.tiff");
    File target = new File("./target/target.tiff");
    GeoTiffReader reader;
    GridCoverage2D coverage;
    GridCoverageReaderResource resource;

    WPSResourceManager resources;
    GeoTiffPPIO ppio;

    @Before
    public void prepareGeoTiff() throws IOException {
        try (InputStream is = SystemTestData.class.getResourceAsStream("tazbm.tiff")) {
            FileUtils.copyInputStreamToFile(is, geotiff);
        }
        reader = new GeoTiffReader(geotiff);
        resources = mock(WPSResourceManager.class);
        ppio = new GeoTiffPPIO(resources);
    }

    @After
    public void cleanup() {
        if (coverage != null) {
            ImageUtilities.disposeImage(coverage.getRenderedImage());
        }
        if (reader != null) {
            reader.dispose();
        }
        if (resource != null) {
            resource.delete();
        }
    }

    @SuppressWarnings("unchecked")
    private GridCoverage2D getCoverage() throws IOException {
        coverage = reader.read(null);
        Map<String, Object> properties = new HashMap<>(coverage.getProperties());
        properties.put(
                AbstractGridCoverage2DReader.FILE_SOURCE_PROPERTY, geotiff.getCanonicalPath());
        return new GridCoverageFactory()
                .create(
                        coverage.getName(),
                        coverage.getRenderedImage(),
                        coverage.getEnvelope(),
                        coverage.getSampleDimensions(),
                        null,
                        properties);
    }

    @Test
    public void testRawCopy() throws Exception {
        GridCoverage2D coverage = getCoverage();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            ppio.encode(coverage, fos);
        }
        // was a straight copy (a re-encoding would change the size as the input
        // is compressed, the output is not)
        assertEquals(geotiff.length(), target.length());
    }

    @Test
    public void testCropped() throws Exception {
        getCoverage(); // populates this.coverage
        ReferencedEnvelope re = ReferencedEnvelope.reference(coverage.getEnvelope2D());
        re.expandBy(-0.1);
        this.coverage = new CropCoverage().execute(coverage, JTS.toGeometry(re), null);
        try (FileOutputStream fos = new FileOutputStream(target)) {
            ppio.encode(coverage, fos);
        }
        // not a straight copy, size is different
        assertNotEquals(geotiff.length(), target.length());
    }

    @Test
    public void testDecodeValidGeoTIFF() throws Exception {
        try (InputStream is = SystemTestData.class.getResourceAsStream("tazbm.tiff")) {
            doAnswer(
                            inv -> {
                                resource = inv.getArgument(0, GridCoverageReaderResource.class);
                                return null;
                            })
                    .when(resources)
                    .addResource(any(GridCoverageReaderResource.class));
            Object result = ppio.decode(is);
            assertThat(result, instanceOf(GridCoverage2D.class));
            coverage = (GridCoverage2D) result;
            verify(resources).addResource(any(GridCoverageReaderResource.class));
        }
    }

    @Test
    public void testGeoTIFFCompression() throws Exception {
        getCoverage();
        try (FileOutputStream fos = new FileOutputStream(target)) {
            ppio.encode(coverage, fos);
        }
        GeoTiffReader reader = new GeoTiffReader(target);
        GeoTiffIIOMetadataDecoder metadata = reader.getMetadata();
        IIOMetadataNode rootNode = metadata.getRootNode();
        assertEquals(
                "Deflate", getAttributeContent(rootNode, BaselineTIFFTagSet.TAG_COMPRESSION, true));

        // With compression, tile size is multiple of 16
        assertEquals(
                "368", getAttributeContent(rootNode, BaselineTIFFTagSet.TAG_TILE_WIDTH, false));
        assertEquals(
                "16", getAttributeContent(rootNode, BaselineTIFFTagSet.TAG_TILE_LENGTH, false));
    }

    private IIOMetadataNode getTiffField(Node rootNode, final int tag) {
        Node node = rootNode.getFirstChild();
        if (node != null) {
            node = node.getFirstChild();
            for (; node != null; node = node.getNextSibling()) {
                Node number = node.getAttributes().getNamedItem("number");
                if (number != null && tag == Integer.parseInt(number.getNodeValue())) {
                    return (IIOMetadataNode) node;
                }
            }
        }
        return null;
    }

    private String getAttributeContent(Node rootNode, int tifTag, boolean isDescription) {
        IIOMetadataNode metadataNode = getTiffField(rootNode, tifTag);
        Node node =
                ((IIOMetadataNode) metadataNode.getFirstChild())
                        .getElementsByTagName("TIFFShorts")
                        .item(0)
                        .getFirstChild();
        return node.getAttributes()
                .getNamedItem(isDescription ? "description" : "value")
                .getNodeValue();
    }

    @Test
    public void testDecodeValidArcGrid() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("arcGrid.asc")) {
            doAnswer(
                            inv -> {
                                resource = inv.getArgument(0, GridCoverageReaderResource.class);
                                return null;
                            })
                    .when(resources)
                    .addResource(any(GridCoverageReaderResource.class));
            Object result = ppio.decode(is);
            assertThat(result, instanceOf(GridCoverage2D.class));
            coverage = (GridCoverage2D) result;
            verify(resources).addResource(any(GridCoverageReaderResource.class));
        }
    }

    @Test
    public void testDecodeInvalid() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("empty-shapefile.zip")) {
            WPSException exception = assertThrows(WPSException.class, () -> ppio.decode(is));
            assertEquals(
                    "Could not find the GeoTIFF GT2 format, please check it's in the classpath",
                    exception.getMessage());
            verify(resources, never()).addResource(any());
        }
    }
}
