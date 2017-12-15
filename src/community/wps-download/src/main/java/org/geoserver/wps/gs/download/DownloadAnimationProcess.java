/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import org.geoserver.ows.kvp.TimeParser;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.DateRange;
import org.geotools.util.DefaultProgressListener;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.opengis.util.ProgressListener;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

@DescribeProcess(title = "Animation Download Process", description = "Builds an animation given a set of layer " +
        "definitions, " +
        "area of interest, size and a series of times for animation frames.")
public class DownloadAnimationProcess implements GeoServerProcess {

    public static final String VIDEO_MP4 = "video/mp4";
    private static final Format MAP_FORMAT;

    static {
        MAP_FORMAT = new Format();
        MAP_FORMAT.setName("image/png");
    }

    private final TimeParser timeParser;
    private final DownloadMapProcess mapper;
    private final WPSResourceManager resourceManager;
    private final DateTimeFormatter formatter;

    public DownloadAnimationProcess(DownloadMapProcess mapper, WPSResourceManager resourceManager) {
        this.mapper = mapper;
        this.timeParser = new TimeParser();
        this.resourceManager = resourceManager;
        // java 8 formatters are thread safe
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withLocale(Locale
                .ENGLISH).withZone(ZoneId.of("GMT"));

    }

    @DescribeResult(name = "result", description = "The animation")
    public RawData execute(
            @DescribeParameter(name = "bbox", min = 1, description = "The map area and output projection")
                    ReferencedEnvelope bbox,
            @DescribeParameter(name = "decoration", min = 0, description = "A WMS decoration layout name to watermark" +
                    " the output") String decorationName,
            @DescribeParameter(name = "time", min = 1, description = "Map time specification (a range with " +
                    "periodicity or a list of time values)") String time,
            @DescribeParameter(name = "width", min = 1, description = "Map width", minValue = 1) int width,
            @DescribeParameter(name = "height", min = 1, description = "Map height", minValue = 1) int height,
            @DescribeParameter(name = "fps", min = 1, description = "Frames per second", minValue = 0, defaultValue =
                    "1") double fps,
            @DescribeParameter(name = "layer", min = 1, description = "The list of layers", minValue = 1) Layer[]
                    layers,
            @DescribeParameter(name = "format", min = 0, description = "The output format") Format format,
            ProgressListener progressListener) throws Exception {

        // default format if missing
        if (format == null) {
            format = new Format();
            format.setName(VIDEO_MP4);
        } else if (!VIDEO_MP4.equalsIgnoreCase(format.getName())) {
            // TODO: allow more formats and codecs?
            throw new WPSException("Currently the only supported format is video/mp4");
        }

        // avoid NPE on progress listener
        if (progressListener == null) {
            progressListener = new DefaultProgressListener();
        }

        final Resource output = resourceManager.getTemporaryResource("mp4");
        Rational frameRate = getFrameRate(fps);

        AWTSequenceEncoder enc = new AWTSequenceEncoder(NIOUtils.writableChannel(output.file()), frameRate);
        Collection parsedTimes = timeParser.parse(time);
        progressListener.started();
        int count = 1;
        for (Object parsedTime : parsedTimes) {
            // turn parsed time into a specification and generate a "WMS" like request based on it
            String mapTime = toWmsTimeSpecification(parsedTime);
            RenderedImage image = mapper.buildImage(bbox, decorationName, mapTime, width, height, layers, format);
            BufferedImage frame = toBufferedImage(image);
            enc.encodeImage(frame);
            progressListener.progress(100 * (parsedTimes.size() / count));
            if (progressListener.isCanceled()) {
                throw new ProcessException("Bailing out due to progress cancellation");
            }
            count++;
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
            mapTime = formatter.format(range.getMinValue().toInstant()) + "/" + formatter.format(range
                    .getMinValue().toInstant());
        } else {
            throw new WPSException("Unexpected parsed date type: " + parsedTime);
        }
        return mapTime;
    }

    public Rational getFrameRate(double fps) {
        if (fps < 0) {
            throw new WPSException("Frames per second must be greater than zero");
        }
        BigDecimal bigDecimal = BigDecimal.valueOf(fps);
        int numerator = (int) bigDecimal.unscaledValue().longValue();
        int denominator = (int) Math.pow(10L, bigDecimal.scale());
        Rational frameRate = new Rational(numerator, denominator);

        return frameRate;
    }

}
