/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Icon;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.PlanarImage;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.kml.KMLEncoder;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.PNGMapResponse;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.ByteArrayRawData;
import org.geoserver.wps.process.RawData;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.filter.function.EnvFunction;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPClientFinder;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.response.GetMapResponse;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.DescribeResults;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@DescribeProcess(
        title = "Map Download Process",
        description =
                "Builds a large map given a set of layer definitions, "
                        + "area of interest, size and eventual target time.")
public class DownloadMapProcess implements GeoServerProcess, ApplicationContextAware {

    private static final boolean TRANSPARENT_DEFAULT_VALUE =
            Boolean.valueOf(GeoServerExtensions.getProperty("DOWNLOAD_MAP_TRANSPARENT"));

    static final Logger LOGGER = Logging.getLogger(DownloadMapProcess.class);

    private final WMS wms;
    private final GetMapKvpRequestReader getMapReader;
    private final HTTPWarningAppender warningAppender;
    private final RasterCleaner rasterCleaner;
    private Service service;
    // defaulting to a stateless but reliable http client
    private Supplier<org.geotools.http.HTTPClient> httpClientSupplier =
            () -> HTTPClientFinder.createClient();

    public DownloadMapProcess(
            GeoServer geoServer, HTTPWarningAppender warningAppender, RasterCleaner rasterCleaner) {
        // TODO: make these configurable
        this.wms =
                new WMS(geoServer) {
                    @Override
                    public int getMaxRenderingTime() {
                        return -1;
                    }

                    @Override
                    public int getMaxRenderingErrors() {
                        return -1;
                    }
                };
        this.getMapReader = new GetMapKvpRequestReader(wms);
        this.warningAppender = warningAppender;
        this.rasterCleaner = rasterCleaner;
    }

    /** This process returns a potentially large map */
    @DescribeResults({
        @DescribeResult(
                name = "result",
                description = "The output map",
                type = RawData.class,
                meta = {
                    "mimeTypes=image/png,image/png8,"
                            + "image/gif,image/jpeg,image/geotiff,image/geotiff8,image/vnd.jpeg-png,application/vnd.google-earth.kmz",
                    "chosenMimeType=format"
                }),
        @DescribeResult(
                name = "metadata",
                type = DownloadMetadata.class,
                description = "map metadata, including dimension match warnings")
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
                            description = "A WMS decoration layout name to watermark the output")
                    String decorationName,
            @DescribeParameter(
                            name = "decorationEnvironment",
                            min = 0,
                            description = "Env parameters used to apply the watermark decoration")
                    String decorationEnvironment,
            @DescribeParameter(
                            name = "time",
                            min = 0,
                            description =
                                    "Map time specification (a single time value or "
                                            + "a range like in WMS time parameter)")
                    String time,
            @DescribeParameter(name = "width", min = 1, description = "Output width", minValue = 1)
                    int width,
            @DescribeParameter(
                            name = "height",
                            min = 1,
                            description = "Output height",
                            minValue = 1)
                    int height,
            @DescribeParameter(name = "headerheight", min = 0, description = "Header height")
                    Integer headerHeight,
            @DescribeParameter(
                            name = "layer",
                            min = 1,
                            description = "List of layers",
                            minValue = 1)
                    Layer[] layers,
            @DescribeParameter(name = "format", min = 0, defaultValue = "image/png")
                    final String format,
            @DescribeParameter(
                            name = "transparent",
                            min = 0,
                            description = "Map background transparency")
                    Boolean transparent,
            ProgressListener progressListener)
            throws Exception {
        // if kmlOutput, reproject request to WGS84 (test is done indirectly to make the code work
        // should KML not be available)
        AbstractMapOutputFormat kmlOutputFormat =
                (AbstractMapOutputFormat) GeoServerExtensions.bean("KMZMapProducer");
        boolean kmlOutput = kmlOutputFormat.getOutputFormatNames().contains(format);
        if (kmlOutput) {
            bbox = bbox.transform(DefaultGeographicCRS.WGS84, true);
        }

        // avoid NPE on progress listener
        if (progressListener == null) {
            progressListener = new DefaultProgressListener();
        }

        if (transparent == null) {
            transparent = TRANSPARENT_DEFAULT_VALUE;
        }

        try {
            // clean up eventual previous warnings
            warningAppender.init(Dispatcher.REQUEST.get());

            // assemble image
            RenderedImage result =
                    buildImage(
                            bbox,
                            decorationName,
                            decorationEnvironment,
                            time,
                            width,
                            height,
                            headerHeight,
                            layers,
                            transparent,
                            format,
                            progressListener,
                            new HashMap<>());

            // encode output (by faking a normal request)
            GetMapRequest request = new GetMapRequest();
            request.setRawKvp(Collections.emptyMap());
            request.setFormat(format);
            WMSMapContent mapContent = new WMSMapContent(request);
            RawData response = null;
            try {
                mapContent.getViewport().setBounds(bbox);
                Operation operation =
                        new Operation("GetMap", service, null, new Object[] {request});

                if (kmlOutput) {
                    response = buildKMLResponse(bbox, result, mapContent, operation);
                } else {
                    response = buildImageResponse(format, result, mapContent, operation);
                }
            } finally {
                mapContent.dispose();
            }

            if (response != null) {
                DownloadMetadata metadata = new DownloadMetadata();
                metadata.accumulateWarnings();
                metadata.setWarningsFound(!metadata.getWarnings().isEmpty());

                Map<String, Object> processResult = new HashMap<>();
                processResult.put("result", response);
                processResult.put("metadata", metadata);
                return processResult;
            }
        } finally {
            // avoid accumulation of warnings in the executor thread that run this request
            warningAppender.finished(Dispatcher.REQUEST.get());
            // clean up images, this process runs in a background thread, it won't get
            // the callback invoked and the thread locals would accumulate images
            rasterCleaner.finished(null);
            // not wrong, and allows tests to check the raster cleaner has done its job
            progressListener.progress(100);
        }

        // we got here, no supported format found
        throw new WPSException("Could not find a image map encoder for format: " + format);
    }

    private RawData buildImageResponse(
            String format, RenderedImage result, WMSMapContent mapContent, Operation operation)
            throws IOException {
        List<RenderedImageMapResponse> encoders =
                GeoServerExtensions.extensions(RenderedImageMapResponse.class);
        for (RenderedImageMapResponse encoder : encoders) {
            if (encoder.canHandle(operation)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                encoder.formatImageOutputStream(result, bos, mapContent);

                // try to build an extension from the format, pleasing Windows clients as possible
                String extension = encoder.getExtension(result, mapContent);
                return new ByteArrayRawData(bos.toByteArray(), format, extension);
            }
        }
        return null;
    }

    private RawData buildKMLResponse(
            ReferencedEnvelope bbox,
            RenderedImage result,
            WMSMapContent mapContent,
            Operation operation)
            throws IOException {
        // custom KMZ building
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();
        Folder folder = document.createAndAddFolder();
        GroundOverlay go = folder.createAndAddGroundOverlay();
        go.setName("Map");
        Icon icon = go.createAndSetIcon();
        icon.setHref("image.png");
        icon.setViewRefreshMode(ViewRefreshMode.NEVER);
        icon.setViewBoundScale(0.75);

        LatLonBox gobox = go.createAndSetLatLonBox();
        gobox.setEast(bbox.getMinX());
        gobox.setWest(bbox.getMaxX());
        gobox.setNorth(bbox.getMaxY());
        gobox.setSouth(bbox.getMinY());

        // create the outupt zip
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            ZipEntry entry = new ZipEntry("wms.kml");
            zip.putNextEntry(entry);
            KMLEncoder kmlEncoder = GeoServerExtensions.bean(KMLEncoder.class);
            kmlEncoder.encode(kml, zip, new KmlEncodingContext(mapContent, wms, true));

            // build and encode the image
            final PNGMapResponse pngEncoder = new PNGMapResponse(wms);
            entry = new ZipEntry("image.png");
            zip.putNextEntry(entry);
            pngEncoder.write(new RenderedImageMap(mapContent, result, "image/png"), zip, operation);
            zip.closeEntry();

            zip.finish();
            zip.flush();
        }

        return new ByteArrayRawData(
                bos.toByteArray(), org.geoserver.kml.KMZMapOutputFormat.MIME_TYPE, "kmz");
    }

    RenderedImage buildImage(
            ReferencedEnvelope bbox,
            String decorationName,
            String decorationEnvironment,
            String time,
            int width,
            int height,
            Integer headerHeight,
            Layer[] layers,
            boolean transparent,
            String format,
            ProgressListener progressListener,
            Map<String, WebMapServer> serverCache)
            throws Exception {
        // going to update the env local values as the frames are rendered
        // grab these to allow restore
        Map<String, Object> localValuesBackup = new HashMap<>(EnvFunction.getLocalValues());
        try {
            return buildImageInternal(
                    bbox,
                    decorationName,
                    decorationEnvironment,
                    time,
                    width,
                    height,
                    headerHeight,
                    layers,
                    transparent,
                    format,
                    progressListener,
                    serverCache);
        } finally {
            EnvFunction.setLocalValues(localValuesBackup);
        }
    }

    @SuppressWarnings("unchecked")
    private RenderedImage buildImageInternal(
            ReferencedEnvelope bbox,
            String decorationName,
            String decorationEnvironment,
            String time,
            int width,
            int height,
            Integer headerHeight,
            Layer[] layers,
            boolean transparent,
            String format,
            ProgressListener progressListener,
            Map<String, WebMapServer> serverCache)
            throws Exception {
        // build GetMap template parameters
        CaseInsensitiveMap template = new CaseInsensitiveMap(new HashMap());
        template.put("service", "WMS");
        template.put("request", "GetMap");
        template.put("transparent", "false");
        template.put("width", String.valueOf(width));
        template.put("height", String.valueOf(height));
        if (time != null) {
            template.put("time", time);
        }
        template.put(
                "bbox",
                bbox.getMinX()
                        + ","
                        + bbox.getMinY()
                        + ","
                        + bbox.getMaxX()
                        + ","
                        + bbox.getMaxY());
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        if (crs == null) {
            throw new WPSException("The BBOX parameter must have a coordinate reference system");
        } else {
            // handle possible axis flipping by changing the WMS version accordingly
            String code = ResourcePool.lookupIdentifier(crs, false);
            if (CRS.getAxisOrder(crs) == CRS.AxisOrder.EAST_NORTH) {
                template.put("version", "1.1.0");
                template.put("srs", SrsSyntax.AUTH_CODE.getSRS(code));
            } else {
                template.put("version", "1.3.0");
                template.put("crs", SrsSyntax.AUTH_CODE.getSRS(code));
            }
        }

        int headerHeightSize = 0;
        if (headerHeight != null) {
            headerHeightSize = headerHeight.intValue();
        }

        // prepare the decoration environment, if any
        Map<String, Object> decorationEnv = Collections.emptyMap();
        if (decorationName != null && decorationEnvironment != null) {
            decorationEnv =
                    (Map<String, Object>) new FormatOptionsKvpParser().parse(decorationEnvironment);
        }

        // loop over layers and accumulate
        int mapsImageHeight = height - headerHeightSize;
        template.put("height", String.valueOf(mapsImageHeight));
        template.put("transparent", String.valueOf(transparent));
        RenderedImage result = null;
        progressListener.started();
        int i = 0;
        boolean singleDecorated = layers.length == 1 && decorationName != null;
        for (Layer layer : layers) {
            LOGGER.log(Level.FINE, "Rendering layer %s", layer);
            RenderedImage image;
            if (layer.getCapabilities() == null) {
                GetMapRequest request = produceGetMapRequest(layer, template);
                // keep compatibility for single decoration definition
                if (singleDecorated && mapsImageHeight == height) {
                    request.setFormatOptions(Collections.singletonMap("layout", decorationName));
                    if (time != null) { // allow text decoration timestamping
                        request.getEnv().put("time", time);
                    }
                }
                // required as the dispatcher callback normally doing this won't be invoked here
                Map<String, Object> layerEnv = new HashMap<>(EnvFunction.getLocalValues());
                layerEnv.putAll(request.getEnv());
                EnvFunction.setLocalValues(layerEnv);
                // render
                GetMap mapBuilder = new GetMap(wms);
                RenderedImageMap map = (RenderedImageMap) mapBuilder.run(request);
                image = map.getImage();
                map.getMapContext().dispose();
            } else {
                image = getImageFromWebMapServer(layer, template, bbox, serverCache);
            }

            result = mergeImage(result, image, layer);

            // past the first layer switch transparency on to allow overlaying
            template.put("transparent", "true");
            // track progress and bail out if necessary
            progressListener.progress(95f * (++i) / layers.length / 2);
        }

        // if header is present
        if (mapsImageHeight < height) {
            // back to original request size
            template.put("height", String.valueOf(height));
            // add empty image
            RenderedImage finalResultImage = getEmptyLayer(format, width, height, bbox);
            // merge previous results into a full sized image
            mergeMapImagesStack(finalResultImage, result, headerHeightSize);
            result = finalResultImage;
        }

        // decoration handling, we'll put together a empty GetMap for it
        GetMapRequest request = new GetMapRequest();
        if (time != null) { // allow text decoration timestamping
            request.getEnv().put("time", time);
        }
        request.setFormat(format);
        if (decorationName != null) {
            request.setFormatOptions(Collections.singletonMap("layout", decorationName));
            // required as the dispatcher callback normally doing this won't be invoked here
            request.getEnv().putAll(decorationEnv);
            EnvFunction.setLocalValues(request.getEnv());
            WMSMapContent content = new WMSMapContent(request);
            try {
                content.setMapWidth(width);
                content.setMapHeight(height);
                content.setTransparent(true);
                content.getViewport().setBounds(bbox);
                RenderedImageMapOutputFormat renderer = new RenderedImageMapOutputFormat(wms);
                RenderedImageMap map = renderer.produceMap(content);

                result = mergeImage(result, map.getImage(), null);
            } finally {
                content.dispose();
            }
        }

        // finally add all legend decorators
        for (Layer layer : layers) {
            LOGGER.log(Level.FINE, "Rendering layer decorations %s", layer);
            RenderedImage image;
            if (layer.getCapabilities() == null) {
                // if layer contains a DecorationName then generate decoration with empty map
                GetMapRequest decoratorMapRequest = produceGetMapRequest(layer, template);
                String layerDecorationName = layer.getDecorationName();
                if (layerDecorationName != null && !layerDecorationName.isEmpty()) {
                    applyDecorations(decoratorMapRequest, layerDecorationName, time);
                    // render
                    GetMap mapBuilder = new GetMap(wms);
                    RenderedImageMap map = (RenderedImageMap) mapBuilder.run(decoratorMapRequest);
                    image = map.getImage();
                    map.getMapContext().dispose();
                    if (result != null) {
                        result = mergeImage(result, image, null);
                    }
                }
            }
            // track progress and bail out if necessary
            progressListener.progress(95f * (++i) / layers.length / 2);
        }

        progressListener.progress(90);

        return result;
    }

    private RenderedImage getEmptyLayer(
            String format, int width, int height, ReferencedEnvelope bbox) {
        // Empty layer for header
        GetMapRequest request = new GetMapRequest();
        request.setFormat(format);
        WMSMapContent content = new WMSMapContent(request);
        try {
            content.setMapWidth(width);
            content.setMapHeight(height);
            content.setTransparent(true);
            content.getViewport().setBounds(bbox);
            RenderedImageMapOutputFormat renderer = new RenderedImageMapOutputFormat(wms);
            RenderedImageMap map = renderer.produceMap(content);
            return map.getImage();
        } finally {
            content.dispose();
        }
    }

    private void applyDecorations(GetMapRequest request, String decoration, String time) {
        Map<String, Object> aMap = new HashMap<>();
        aMap.put("layout", decoration);
        aMap.put(RenderedImageMapOutputFormat.DECORATIONS_ONLY_FORMAT_OPTION, "true");
        request.setFormatOptions(aMap);
        if (time != null) { // allow text decoration timestamping
            request.getEnv().put("time", time);
        }
    }

    /** Retrieves the image from the remote web map server */
    private RenderedImage getImageFromWebMapServer(
            Layer layer,
            Map<String, ?> template,
            ReferencedEnvelope bbox,
            Map<String, WebMapServer> cache)
            throws IOException, ServiceException, FactoryException {
        // using a WMS client so that it respects the GetMap URL from the capabilities
        WebMapServer server = getServer(layer, cache);
        org.geotools.ows.wms.request.GetMapRequest getMap = server.createGetMapRequest();
        String requestFormat = getCascadingFormat(server);

        // going low level to apply all the properties we have verbatim
        template.keySet().stream()
                .filter(
                        k ->
                                !"version".equalsIgnoreCase((String) k)
                                        && !"srs".equalsIgnoreCase((String) k))
                .forEach(key -> getMap.setProperty((String) key, (String) template.get(key)));
        getMap.setProperty("layers", layer.getName());
        getMap.setFormat(requestFormat);
        getMap.setVersion(server.getCapabilities().getVersion());

        // check version, if we are using 1.3 we might need to flip the bbox, if version 1.1 and the
        // original bbox was flipped, we'll need to un-flip (what a mess...)
        String crsId = ResourcePool.lookupIdentifier(bbox.getCoordinateReferenceSystem(), true);
        CoordinateReferenceSystem epsgOrderCrs = CRS.decode(SrsSyntax.OGC_URN.getSRS(crsId));
        CRS.AxisOrder axisOrder = CRS.getAxisOrder(epsgOrderCrs);
        getMap.setSRS(
                SrsSyntax.AUTH_CODE.getSRS(crsId)); // takes into account the version already here
        boolean flipNeeded =
                !template.containsKey("crs")
                        && new Version(server.getCapabilities().getVersion())
                                        .compareTo(new Version("1.3.0"))
                                >= 0;
        boolean unflipNeeded =
                template.containsKey("crs")
                        && new Version(server.getCapabilities().getVersion())
                                        .compareTo(new Version("1.3.0"))
                                < 0;
        if (flipNeeded || unflipNeeded) {
            if (flipNeeded && axisOrder == CRS.AxisOrder.NORTH_EAST) {
                getMap.setBBox(
                        bbox.getMinY()
                                + ","
                                + bbox.getMinX()
                                + ","
                                + bbox.getMaxY()
                                + ","
                                + bbox.getMaxX());
            } else if (unflipNeeded && axisOrder == CRS.AxisOrder.NORTH_EAST) {
                getMap.setBBox(
                        bbox.getMinX()
                                + ","
                                + bbox.getMinY()
                                + ","
                                + bbox.getMaxX()
                                + ","
                                + bbox.getMaxY());
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Requesting external map at " + getMap.getFinalURL().toExternalForm());
        }

        GetMapResponse response = server.issueRequest(getMap);
        try (InputStream is = response.getInputStream()) {
            BufferedImage image = ImageIO.read(new MemoryCacheImageInputStream(is));
            if (image == null) {
                throw new IOException("GetMap failed: " + getMap.getFinalURL());
            }
            return image;
        }
    }

    private WebMapServer getServer(Layer layer, Map<String, WebMapServer> cache)
            throws IOException, ServiceException {
        String capabilitiesUrl = layer.getCapabilities();
        WebMapServer server = cache.get(capabilitiesUrl);
        if (server == null) {
            HTTPClient client = httpClientSupplier.get();
            server = new WebMapServer(new URL(layer.getCapabilities()), client);
            cache.put(capabilitiesUrl, server);
        }

        return server;
    }

    private String getCascadingFormat(WebMapServer server) {
        // best guess at the format with a preference for PNG (since it's normally transparent)
        List<String> formats = server.getCapabilities().getRequest().getGetMap().getFormats();
        String requestFormat = null;
        for (String format : formats) {
            if (format.toLowerCase().contains("image/png") || "png".equalsIgnoreCase(format)) {
                requestFormat = format;
                break;
            }
        }
        // if we did not find any format looking like PNG choose any that ImageIO would likely read
        if (requestFormat == null) {
            for (String format : formats) {
                String loFormat = format.toLowerCase();
                if (loFormat.contains("jpeg")
                        || loFormat.contains("gif")
                        || loFormat.contains("tif")) {
                    requestFormat = format;
                    break;
                }
            }
        }

        if (requestFormat == null) {
            throw new WPSException(
                    "Could not find a suitable WMS cascading format among server supported formats: "
                            + formats);
        }
        return requestFormat;
    }

    private RenderedImage mergeMapImagesStack(
            RenderedImage result, RenderedImage image, int headerHeight) {
        if (!(result instanceof BufferedImage)) {
            result = PlanarImage.wrapRenderedImage(result).getAsBufferedImage();
        }
        BufferedImage bi = (BufferedImage) result;
        Graphics2D graphics = (Graphics2D) bi.getGraphics();
        graphics.drawRenderedImage(image, AffineTransform.getTranslateInstance(0, headerHeight));
        graphics.dispose();
        return result;
    }

    private RenderedImage mergeImage(RenderedImage result, RenderedImage image, Layer layer) {
        if (result == null && layer != null) {
            // assume this is the first layer
            // nothing to do if no opacity is requested
            if (layer.getOpacity() == null) {
                return image;
            } else {
                // if opacity is requested, create an empty image to merge with
                result =
                        new BufferedImage(
                                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
        }
        // make sure we can paint on it
        if (!(result instanceof BufferedImage)) {
            result = PlanarImage.wrapRenderedImage(result).getAsBufferedImage();
        }
        // could use mosaic here, but would require keeping all images in memory to build the op,
        // this way at most two at any time are around, so uses less memory overall
        BufferedImage bi = (BufferedImage) result;
        Graphics2D graphics = (Graphics2D) bi.getGraphics();
        if (layer != null && layer.getOpacity() != null) {
            applyOpacity(image, layer, graphics);
        }
        graphics.drawRenderedImage(image, AffineTransform.getScaleInstance(1, 1));
        graphics.dispose();
        return result;
    }

    private static void applyOpacity(RenderedImage image, Layer layer, Graphics2D graphics) {
        if (layer.getOpacity() < 0 || layer.getOpacity() > 100) {
            throw new WPSException(
                    "Layer: "
                            + layer.getName()
                            + " has opacity set to an invalid value (only 0-100 allowed): "
                            + layer.getOpacity());
        }
        graphics.setComposite(
                java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, layer.getOpacity().floatValue() / 100));
    }

    private GetMapRequest produceGetMapRequest(Layer layer, Map<String, Object> kvpTemplate)
            throws Exception {
        GetMapRequest request = getMapReader.createRequest();

        // prepare raw and parsed KVP maps to mimick a GetMap request
        Map<String, Object> rawKvp = new CaseInsensitiveMap<>(new HashMap<>());
        rawKvp.putAll(kvpTemplate);
        rawKvp.put("format", "image/png"); // fake format, we are building a RenderedImage
        rawKvp.put("layers", layer.getName());
        for (Parameter parameter : layer.getParameters()) {
            rawKvp.put(parameter.key, parameter.value);
        }
        // for merging layers, unless the request stated otherwise
        rawKvp.putIfAbsent("transparent", "true");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Internal render of map with key/value params: " + rawKvp);
        }

        Map<String, Object> kvp = new CaseInsensitiveMap<>(new HashMap<>());
        kvp.putAll(rawKvp);
        List<Throwable> exceptions = KvpUtils.parse(kvp);
        if (exceptions != null && !exceptions.isEmpty()) {
            throw new WPSException(
                    "Failed to build map for layer: " + layer.getName(), exceptions.get(0));
        }

        // parse
        getMapReader.read(request, kvp, rawKvp);
        return request;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.wms.setApplicationContext(applicationContext);
        List<Service> services = GeoServerExtensions.extensions(Service.class, applicationContext);
        this.service =
                services.stream()
                        .filter(s -> "WMS".equalsIgnoreCase(s.getId()))
                        .findFirst()
                        .orElse(null);
        if (service == null) {
            throw new RuntimeException("Could not find a WMS service");
        }
    }

    /**
     * Returns the current {@link Supplier<HTTPClient>} building http clients for remote WMS
     * connection
     */
    public Supplier<HTTPClient> getHttpClientSupplier() {
        return httpClientSupplier;
    }

    /**
     * Sets the {@link Supplier<HTTPClient>} used to build http clients for remote WMS connections
     */
    public void setHttpClientSupplier(Supplier<HTTPClient> httpClientSupplier) {
        this.httpClientSupplier = httpClientSupplier;
    }
}
