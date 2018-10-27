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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.PlanarImage;
import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.DateRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.opengis.util.ProgressListener;

@DescribeProcess(
    title = "Animation Download Process",
    description =
            "Builds an animation given a set of layer "
                    + "definitions, "
                    + "area of interest, size and a series of times for animation frames."
)
public class DownloadAnimationProcess implements GeoServerProcess {

    static final Logger LOGGER = Logging.getLogger(DownloadAnimationProcess.class);

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

    public DownloadAnimationProcess(
            DownloadMapProcess mapper,
            WPSResourceManager resourceManager,
            DownloadServiceConfigurationGenerator downloadServiceConfigurationGenerator) {
        this.mapper = mapper;
        this.resourceManager = resourceManager;
        this.confiGenerator = downloadServiceConfigurationGenerator;
        // java 8 formatters are thread safe
        this.formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                        .withLocale(Locale.ENGLISH)
                        .withZone(ZoneId.of("GMT"));
    }

    @DescribeResult(
        name = "result",
        description = "The animation",
        meta = {"mimeTypes=" + VIDEO_MP4, "chosenMimeType=format"}
    )
    public RawData execute(
            @DescribeParameter(
                        name = "bbox",
                        min = 1,
                        description = "The map area and output projection"
                    )
                    ReferencedEnvelope bbox,
            @DescribeParameter(
                        name = "decoration",
                        min = 0,
                        description = "A WMS decoration layout name to watermark" + " the output"
                    )
                    String decorationName,
            @DescribeParameter(
                        name = "time",
                        min = 1,
                        description =
                                "Map time specification (a range with "
                                        + "periodicity or a list of time values)"
                    )
                    String time,
            @DescribeParameter(name = "width", min = 1, description = "Map width", minValue = 1)
                    int width,
            @DescribeParameter(name = "height", min = 1, description = "Map height", minValue = 1)
                    int height,
            @DescribeParameter(
                        name = "fps",
                        min = 1,
                        description = "Frames per second",
                        minValue = 0,
                        defaultValue = "1"
                    )
                    double fps,
            @DescribeParameter(
                        name = "layer",
                        min = 1,
                        description = "The list of layers",
                        minValue = 1
                    )
                    Layer[] layers,
            ProgressListener progressListener)
            throws Exception {

        // avoid NPE on progress listener
        if (progressListener == null) {
            progressListener = new DefaultProgressListener();
        }

        // if height and width are an odd number fix them, cannot encode videos otherwise
        if (width % 2 == 1) {
            width++;
        }
        if (height % 2 == 1) {
            height++;
        }

        final Resource output = resourceManager.getTemporaryResource("mp4");
        Rational frameRate = getFrameRate(fps);

        AWTSequenceEncoder enc =
                new AWTSequenceEncoder(NIOUtils.writableChannel(output.file()), frameRate);

        DownloadServiceConfiguration configuration = confiGenerator.getConfiguration();
        TimeParser timeParser = new TimeParser(configuration.getMaxAnimationFrames());
        Collection parsedTimes = timeParser.parse(time);
        progressListener.started();
        int count = 1;
        Map<String, WebMapServer> serverCache = new HashMap<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Future<Void>> futures = new ArrayList<>();
            int totalTimes = parsedTimes.size();
            for (Object parsedTime : parsedTimes) {
                // turn parsed time into a specification and generate a "WMS" like request based on
                // it
                String mapTime = toWmsTimeSpecification(parsedTime);
                LOGGER.log(Level.FINE, "Building frame for time %s", mapTime);
                RenderedImage image =
                        mapper.buildImage(
                                bbox,
                                decorationName,
                                mapTime,
                                width,
                                height,
                                layers,
                                "image/png",
                                new DefaultProgressListener(),
                                serverCache);
                BufferedImage frame = toBufferedImage(image);
                LOGGER.log(Level.FINE, "Got frame %s", frame);
                Future<Void> future =
                        executor.submit(
                                () -> {
                                    enc.encodeImage(frame);
                                    return (Void) null;
                                });
                futures.add(future);
                // checking progress
                progressListener.progress(90 * (((float) count) / totalTimes));
                progressListener.setTask(
                        new SimpleInternationalString(
                                "Generated frames " + count + " out of " + totalTimes));
                if (progressListener.isCanceled()) {
                    throw new ProcessException("Bailing out due to progress cancellation");
                }
                count++;
            }
            for (Future<Void> future : futures) {
                future.get();
            }
            progressListener.progress(100);
        } finally {
            executor.shutdown();
        }
        enc.finish();

        return new ResourceRawData(output, VIDEO_MP4, "mp4");
    }

    private BufferedImage toBufferedImage(RenderedImage image) {
        BufferedImage frame;
        if (image instanceof BufferedImage) {
            frame = (BufferedImage) image;
        } else {
            frame = PlanarImage.wrapRenderedImage(image).getAsBufferedImage();
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
