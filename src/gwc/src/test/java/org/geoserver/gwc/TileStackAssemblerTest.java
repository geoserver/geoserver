/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.codec.ImageDecoder;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageDecoderImpl;
import org.geowebcache.io.codec.ImageEncoder;
import org.geowebcache.io.codec.ImageEncoderContainer;
import org.geowebcache.io.codec.ImageEncoderImpl;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/** Unit test suite for {@link TileStackAssembler}. */
public class TileStackAssemblerTest {

    private MimeType png;

    private TileStackAssembler assembler;

    @Before
    public void setUp() throws Exception {
        png = MimeType.createFromFormat("image/png");

        ImageDecoderContainer decoders = new ImageDecoderContainer();
        ApplicationContext decoderContext = mock(ApplicationContext.class);
        when(decoderContext.getBeansOfType(ImageDecoder.class))
                .thenReturn(Map.of("d", new ImageDecoderImpl(false, List.of("image/png"))));
        decoders.setApplicationContext(decoderContext);

        ImageEncoderContainer encoders = new ImageEncoderContainer();
        ApplicationContext encoderContext = mock(ApplicationContext.class);
        when(encoderContext.getBeansOfType(ImageEncoder.class))
                .thenReturn(Map.of("e", new ImageEncoderImpl(false, List.of("image/png"), Map.of())));
        encoders.setApplicationContext(encoderContext);

        assembler = new TileStackAssembler(decoders, encoders);
    }

    @Test
    public void testAssembleStacksMembersInLayersOrder() throws Exception {
        CoalescedRequestSplitter.Segment bottom = segment(solidTile(Color.RED));
        CoalescedRequestSplitter.Segment top = segment(solidTile(Color.BLUE));

        BufferedImage result = decode(assembler.assemble(null, null, null, List.of(bottom, top), png, -1));

        assertEquals(Color.BLUE.getRGB(), result.getRGB(0, 0));
    }

    @Test
    public void testAssembleAppliesAlphaOverCompositing() throws Exception {
        CoalescedRequestSplitter.Segment bottom = segment(solidTile(Color.RED));
        CoalescedRequestSplitter.Segment transparentTop = segment(solidTile(new Color(0, 0, 255, 0)));

        BufferedImage result = decode(assembler.assemble(null, null, null, List.of(bottom, transparentTop), png, -1));

        assertEquals(Color.RED.getRGB(), result.getRGB(0, 0));
    }

    @Test
    public void testAssembleFailsWhenDeadlineHasPassed() throws Exception {
        CoalescedRequestSplitter.Segment segment = segment(solidTile(Color.RED));
        long deadlineAlreadyPassed = System.currentTimeMillis() - 1000;

        assertThrows(
                ServiceException.class,
                () -> assembler.assemble(null, null, null, List.of(segment), png, deadlineAlreadyPassed));
    }

    @Test
    public void testAssemblePropagatesPlainRuntimeExceptionFromRender() throws Exception {
        // a getTile() failure (e.g. a storage backend hiccup) must surface as a plain RuntimeException, distinct
        // from the ServiceException the deadline check throws: CoalescedRequestSplitter.dispatchCoalesced relies on
        // that distinction to
        // fall back to a live render instead of failing the whole request outright
        TileLayer tileLayer = mock(TileLayer.class);
        RuntimeException renderFailure = new RuntimeException("cannot access file, used by another process");
        when(tileLayer.getTile(any())).thenThrow(renderFailure);
        ConveyorTile tile = new ConveyorTile(null, "member", "TEST", new long[] {0, 0, 0}, png, Map.of(), null, null);
        CoalescedRequestSplitter.Segment segment = new CoalescedRequestSplitter.CachedSegment(
                new CoalescedRequestSplitter.TileLayerMember(tileLayer, tile));

        RuntimeException thrown = assertThrows(
                RuntimeException.class, () -> assembler.assemble(null, null, null, List.of(segment), png, -1));

        assertSame(renderFailure, thrown);
    }

    @Test
    public void testAssembleFailsWhenASegmentSizeDoesNotMatchTheCanvas() throws Exception {
        CoalescedRequestSplitter.Segment bottom = segment(solidTile(Color.RED, 2, 2));
        CoalescedRequestSplitter.Segment mismatched = segment(solidTile(Color.BLUE, 3, 3));

        assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(null, null, null, List.of(bottom, mismatched), png, -1));
    }

    @Test
    public void testAssemblePeekHitSkipsRender() throws Exception {
        CoalescedRequestSplitter splitter = mock(CoalescedRequestSplitter.class);
        when(splitter.getMetaTilingExecutor(any())).thenReturn(Runnable::run);
        Dispatcher.REQUEST.set(new Request());
        try {
            CoalescedRequestSplitter.CachedSegment bottom = peekHitSegment(solidTile(Color.RED));
            CoalescedRequestSplitter.CachedSegment top = peekHitSegment(solidTile(Color.BLUE));

            BufferedImage result = decode(assembler.assemble(splitter, null, null, List.of(bottom, top), png, -1));

            assertEquals(Color.BLUE.getRGB(), result.getRGB(0, 0));
            // the peek already populated both blobs, so the render loop must never fall back to getTile()
            verify(bottom.member().tileLayer(), never()).getTile(any());
            verify(top.member().tileLayer(), never()).getTile(any());
        } finally {
            Dispatcher.REQUEST.remove();
        }
    }

    @Test
    public void testAssembleRendersOnPeekMiss() throws Exception {
        CoalescedRequestSplitter splitter = mock(CoalescedRequestSplitter.class);
        when(splitter.getMetaTilingExecutor(any())).thenReturn(Runnable::run);
        Dispatcher.REQUEST.set(new Request());
        try {
            CoalescedRequestSplitter.CachedSegment bottom = peekMissSegment(solidTile(Color.RED));
            CoalescedRequestSplitter.CachedSegment top = peekMissSegment(solidTile(Color.BLUE));

            BufferedImage result = decode(assembler.assemble(splitter, null, null, List.of(bottom, top), png, -1));

            assertEquals(Color.BLUE.getRGB(), result.getRGB(0, 0));
            // both peeks missed, so each member must still get rendered exactly once, like before the peek phase
            verify(bottom.member().tileLayer(), times(1)).getTile(any());
            verify(top.member().tileLayer(), times(1)).getTile(any());
        } finally {
            Dispatcher.REQUEST.remove();
        }
    }

    /** A cached segment whose peek is a hit: {@code peekCache} stamps the blob and returns {@code true}. */
    private CoalescedRequestSplitter.CachedSegment peekHitSegment(byte[] pngBytes) throws Exception {
        ConveyorTile tile = new ConveyorTile(null, "member", "TEST", new long[] {0, 0, 0}, png, Map.of(), null, null);
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.peekCache(any())).thenAnswer(invocation -> {
            ConveyorTile t = invocation.getArgument(0);
            t.setBlob(new ByteArrayResource(pngBytes));
            return true;
        });
        return new CoalescedRequestSplitter.CachedSegment(
                new CoalescedRequestSplitter.TileLayerMember(tileLayer, tile));
    }

    /** A cached segment whose peek always misses; {@code getTile} is the only thing that stamps the blob. */
    private CoalescedRequestSplitter.CachedSegment peekMissSegment(byte[] pngBytes) throws Exception {
        ConveyorTile tile = new ConveyorTile(null, "member", "TEST", new long[] {0, 0, 0}, png, Map.of(), null, null);
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.peekCache(any())).thenReturn(false);
        doAnswer(invocation -> {
                    ConveyorTile t = invocation.getArgument(0);
                    t.setBlob(new ByteArrayResource(pngBytes));
                    return t;
                })
                .when(tileLayer)
                .getTile(any(ConveyorTile.class));
        return new CoalescedRequestSplitter.CachedSegment(
                new CoalescedRequestSplitter.TileLayerMember(tileLayer, tile));
    }

    private byte[] solidTile(Color color) throws Exception {
        return solidTile(color, 2, 2);
    }

    private byte[] solidTile(Color color, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private BufferedImage decode(byte[] encoded) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(encoded));
    }

    /** A cached segment whose {@code TileLayer.getTile} just stamps the given PNG bytes as the tile's blob. */
    private CoalescedRequestSplitter.CachedSegment segment(byte[] pngBytes) throws Exception {
        ConveyorTile tile = new ConveyorTile(null, "member", "TEST", new long[] {0, 0, 0}, png, Map.of(), null, null);
        TileLayer tileLayer = mock(TileLayer.class);
        doAnswer(invocation -> {
                    ConveyorTile t = invocation.getArgument(0);
                    t.setBlob(new ByteArrayResource(pngBytes));
                    return t;
                })
                .when(tileLayer)
                .getTile(any(ConveyorTile.class));
        return new CoalescedRequestSplitter.CachedSegment(
                new CoalescedRequestSplitter.TileLayerMember(tileLayer, tile));
    }
}
