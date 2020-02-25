/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.org.geoserver.wcs2_0.response;

import static org.junit.Assert.assertTrue;

import com.sun.media.jai.operator.ImageReadDescriptor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.RenderedOp;
import org.geoserver.wcs2_0.response.GranuleStackImpl;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class GranulStackImplTest {

    @Test
    public void testImageDispose() throws Exception {
        // build a stream and a reader on top of a one pixel GIF image,
        // with a check on whether they get disposed of
        AtomicBoolean readerDisposed = new AtomicBoolean(false);
        AtomicBoolean streamDisposed = new AtomicBoolean(false);
        byte[] bytes =
                Base64.getDecoder().decode("R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==");
        MemoryCacheImageInputStream is =
                new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)) {
                    @Override
                    public void close() {
                        streamDisposed.set(true);
                    }
                };
        final ImageReader nativeReader = ImageIO.getImageReadersByFormatName("GIF").next();
        nativeReader.setInput(is);
        ImageReader reader =
                new ImageReader(null) {

                    @Override
                    public int getNumImages(boolean allowSearch) throws IOException {
                        return nativeReader.getNumImages(allowSearch);
                    }

                    @Override
                    public int getWidth(int imageIndex) throws IOException {
                        return nativeReader.getWidth(imageIndex);
                    }

                    @Override
                    public int getHeight(int imageIndex) throws IOException {
                        return nativeReader.getHeight(imageIndex);
                    }

                    @Override
                    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
                            throws IOException {
                        return nativeReader.getImageTypes(imageIndex);
                    }

                    @Override
                    public IIOMetadata getStreamMetadata() throws IOException {
                        return nativeReader.getStreamMetadata();
                    }

                    @Override
                    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
                        return nativeReader.getImageMetadata(imageIndex);
                    }

                    @Override
                    public BufferedImage read(int imageIndex, ImageReadParam param)
                            throws IOException {
                        return nativeReader.read(imageIndex, param);
                    }

                    @Override
                    public void dispose() {
                        nativeReader.dispose();
                        readerDisposed.set(true);
                    }
                };
        // wrap it in a image read
        RenderedOp image =
                ImageReadDescriptor.create(
                        is, 0, false, false, false, null, null, null, reader, null);

        // build a coverage and a granule stack around it
        GridCoverageFactory coverageFactory = CoverageFactoryFinder.getGridCoverageFactory(null);
        GridCoverage2D coverage =
                coverageFactory.create(
                        "foo",
                        image,
                        new ReferencedEnvelope(0, 1, 0, 1, DefaultGeographicCRS.WGS84));
        GranuleStackImpl stack = new GranuleStackImpl("fooBar", DefaultGeographicCRS.WGS84, null);
        stack.addCoverage(coverage);

        // check stream and reader have been properly disposed of on stack dispose
        stack.dispose(true);
        assertTrue(streamDisposed.get());
        assertTrue(readerDisposed.get());
    }
}
