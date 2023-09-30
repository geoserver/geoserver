/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.PlanarImage;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.DescribeResults;
import org.geotools.util.DateRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.opengis.util.ProgressListener;

@DescribeProcess(
        title = "Animation Download Process",
        description =
                "Builds an animation given a set of layer "
                        + "definitions, "
                        + "area of interest, size and a series of times for animation frames.")
public class DownloadAnimationProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(DownloadAnimationProcess.class);
    private static BufferedImage STOP = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);

    public static final String VIDEO_MP4 = "video/mp4";
    private static final Format MAP_FORMAT;

    static {
        MAP_FORMAT = new Format();
        MAP_FORMAT.setName("image/png");
    }

    private final DownloadMapProcess mapper;
    private final WPSResourceManager resourceManager;
    private final DateTimeFormatter formatter;
    private final DownloadServiceConfigurationGenerator confiGenerator;
    private final HTTPWarningAppender warningAppender;
    private final RasterCleaner rasterCleaner;

    public DownloadAnimationProcess(
            DownloadMapProcess mapper,
            WPSResourceManager resourceManager,
            DownloadServiceConfigurationGenerator downloadServiceConfigurationGenerator,
            HTTPWarningAppender warningAppender,
            RasterCleaner rasterCleaner) {
        this.mapper = mapper;
        this.resourceManager = resourceManager;
        this.confiGenerator = downloadServiceConfigurationGenerator;
        this.warningAppender = warningAppender;
        // java 8 formatters are thread safe
        this.formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withLocale(Locale.ENGLISH)
                        .withZone(ZoneId.of("GMT"));
        this.rasterCleaner = rasterCleaner;
    }

    @DescribeResults({
        @DescribeResult(
                name = "result",
                description = "The animation",
                type = RawData.class,
                meta = {"mimeTypes=" + VIDEO_MP4, "chosenMimeType=format"}),
        @DescribeResult(
                name = "metadata",
                type = AnimationMetadata.class,
                description = "Animation metadata, including dimension match warnings")
    })
    public Map<String, Object> execute(
            @DescribeParameter(
                            name = "bbox",
                            min = 1,
                            description = "The map area and output projection")
                    ReferencedEnvelope bbox,
            @DescribeParameter(
                            name = "decoration",
                            min = 0,
                            description =
                                    "A WMS decoration layout name to watermark" + " the output")
                    String decorationName,
            @DescribeParameter(
                            name = "decorationEnvironment",
                            min = 0,
                            description = "Env parameters used to apply the watermark decoration")
                    String decorationEnvironment,
            @DescribeParameter(name = "headerheight", min = 0, description = "Header height")
                    Integer headerHeight,
            @DescribeParameter(
                            name = "time",
                            min = 1,
                            description =
                                    "Map time specification (a range with "
                                            + "periodicity or a list of time values)")
                    String time,
            @DescribeParameter(name = "width", min = 1, description = "Output width", minValue = 1)
                    int width,
            @DescribeParameter(
                            name = "height",
                            min = 1,
                            description = "Output height",
                            minValue = 1)
                    int height,
            @DescribeParameter(
                            name = "fps",
                            min = 1,
                            description = "Frames per second",
                            minValue = 0,
                            defaultValue = "1")
                    double fps,
            @DescribeParameter(
                            name = "layer",
                            min = 1,
                            description = "The list of layers",
                            minValue = 1)
                    Layer[] layers,
            ProgressListener progressListener)
            throws Exception {

        // avoid NPE on progress listener, make it effectively final for lambda to use below
        ProgressListener listener =
                Optional.ofNullable(progressListener).orElse(new DefaultProgressListener());

        // if height and width are an odd number fix them, cannot encode videos otherwise
        if (width % 2 != 0) {
            width++;
        }
        if (height % 2 != 0) {
            height++;
        }

        final Resource output = resourceManager.getTemporaryResource("mp4");
        Rational frameRate = getFrameRate(fps);

        try (FileChannelWrapper out = NIOUtils.writableChannel(output.file())) {
            AWTSequenceEncoder enc = new AWTSequenceEncoder(out, frameRate);

            DownloadServiceConfiguration configuration = confiGenerator.getConfiguration();
            TimeParser timeParser = new TimeParser(configuration.getMaxAnimationFrames());
            Collection parsedTimes = timeParser.parse(time);
            progressListener.started();
            Map<String, WebMapServer> serverCache = new HashMap<>();

            // Have two threads work on encoding. The current thread builds the frames, and submits
            // them into a small queue that the encoder thread picks from
            BlockingQueue<BufferedImage> renderingQueue = new LinkedBlockingDeque<>(1);
            BasicThreadFactory threadFactory =
                    new BasicThreadFactory.Builder().namingPattern("animation-encoder-%d").build();
            ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
            // a way to get out of the encoding loop in case an exception happens during frame
            // rendering
            AtomicBoolean abortEncoding = new AtomicBoolean(false);
            Future<Void> future =
                    executor.submit(
                            () -> {
                                int totalTimes = parsedTimes.size();
                                int count = 1;
                                BufferedImage frame;
                                while ((frame = renderingQueue.take()) != STOP) {
                                    enc.encodeImage(frame);
                                    RasterCleaner.disposeImage(frame);
                                    listener.progress(90 * (((float) count) / totalTimes));
                                    String message =
                                            "Generated frames " + count + " out of " + totalTimes;
                                    listener.setTask(new SimpleInternationalString(message));
                                    count++;
                                    // handling exit due to WPS cancellation, or to exceptions
                                    if (listener.isCanceled() || abortEncoding.get()) return null;
                                }
                                return null;
                            });
            Request request = Dispatcher.REQUEST.get();
            AnimationMetadata metadata = new AnimationMetadata();
            try {
                int frameCounter = 0;
                for (Object parsedTime : parsedTimes) {
                    // turn parsed time into a specification, generates a "WMS" like request based
                    // on it
                    String mapTime = toWmsTimeSpecification(parsedTime);
                    LOGGER.log(Level.FINE, "Building frame for time %s", mapTime);
                    // clean up eventual previous warnings
                    warningAppender.init(request);

                    RenderedImage image =
                            mapper.buildImage(
                                    bbox,
                                    decorationName,
                                    decorationEnvironment,
                                    mapTime,
                                    width,
                                    height,
                                    headerHeight,
                                    layers,
                                    false,
                                    "image/png",
                                    new DefaultProgressListener(),
                                    serverCache);
                    BufferedImage frame = toBufferedImage(image);
                    LOGGER.log(Level.FINE, "Got frame %s", frame);
                    renderingQueue.put(frame);
                    metadata.accumulateWarnings(frameCounter++);

                    // exit sooner in case of cancellation, encoding abort is handled in finally
                    if (listener.isCanceled()) return null;
                }
                renderingQueue.put(STOP);
                // wait for encoder to finish
                future.get();
            } finally {
                // force encoding thread to stop in case we got here due to an exception
                abortEncoding.set(true);
                executor.shutdown();
                // clean up the images collected during the execution, in case the
                // clean ups above did not do the job
                rasterCleaner.finished(null);
                warningAppender.finished(request);
            }
            progressListener.progress(100);
            enc.finish();

            // it's a derived property, but XStream does not like to use getters
            metadata.setWarningsFound(!metadata.getWarnings().isEmpty());

            Map<String, Object> result = new HashMap<>();
            result.put("result", new ResourceRawData(output, VIDEO_MP4, "mp4"));
            result.put("metadata", metadata);
            return result;
        }
    }

    private BufferedImage toBufferedImage(RenderedImage image) {
        BufferedImage frame;
        if (image instanceof BufferedImage) {
            frame = (BufferedImage) image;
        } else {
            frame = PlanarImage.wrapRenderedImage(image).getAsBufferedImage();
            RasterCleaner.disposeImage(image);
        }
        return frame;
    }

    private String toWmsTimeSpecification(Object parsedTime) {
        String mapTime;
        if (parsedTime instanceof Date) {
            mapTime = formatter.format(((Date) parsedTime).toInstant());
        } else if (parsedTime instanceof DateRange) {
            DateRange range = (DateRange) parsedTime;
            mapTime =
                    formatter.format(range.getMinValue().toInstant())
                            + "/"
                            + formatter.format(range.getMinValue().toInstant());
        } else {
            throw new WPSException("Unexpected parsed date type: " + parsedTime);
        }
        return mapTime;
    }

    private Rational getFrameRate(double fps) {
        if (fps < 0) {
            throw new WPSException("Frames per second must be greater than zero");
        }
        BigDecimal bigDecimal = BigDecimal.valueOf(fps);
        int numerator = (int) bigDecimal.unscaledValue().longValue();
        int denominator = (int) Math.pow(10L, bigDecimal.scale());

        return new Rational(numerator, denominator);
    }
}
