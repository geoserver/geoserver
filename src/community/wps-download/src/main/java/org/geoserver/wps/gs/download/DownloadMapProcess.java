/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.ByteArrayRawData;
import org.geoserver.wps.process.RawData;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DescribeProcess(title = "Map Download Process", description = "Builds a large map given a set of layer definitions, " +
        "area of interest, size and eventual target time.")
public class DownloadMapProcess implements GeoServerProcess, ApplicationContextAware {


    private final WMS wms;
    private final GetMapKvpRequestReader getMapReader;
    private Service service;

    public DownloadMapProcess(GeoServer geoServer) {
        // TODO: make these configurable
        this.wms = new WMS(geoServer) {
            @Override
            public int getMaxRenderingTime() {
                return -1;
            }

            @Override
            public int getMaxRenderingErrors() {
                return -1;
            }

            @Override
            public Long getMaxRenderingSize() {
                return null; // 
            }
        };
        this.getMapReader = new GetMapKvpRequestReader(wms);
    }

    /**
     * This process returns a potentially large map
     */
    @DescribeResult(name = "result", description = "The output map")
    public RawData execute(
            @DescribeParameter(name = "bbox", min = 1, description = "The map area and output projection") 
                    ReferencedEnvelope bbox,
            @DescribeParameter(name = "decoration", min = 0, description = "A WMS decoration layout name to watermark the output") String decorationName,
            @DescribeParameter(name = "time", min = 0, description = "Map time specification (a single time value or " +
                    "a range like in WMS time parameter)") String time,
            @DescribeParameter(name = "width", min = 1, description = "Map width", minValue = 1) int width,
            @DescribeParameter(name = "height", min = 1, description = "Map height", minValue = 1) int height,
            @DescribeParameter(name = "layer", min = 1, description = "The list of layers", minValue = 1) Layer[] 
                    layers,
            @DescribeParameter(name = "format", min = 1, description = "The output format", minValue = 1) Format format,
            final ProgressListener progressListener) throws Exception {


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
        template.put("bbox", bbox.getMinX() + "," + bbox.getMinY() + "," + bbox.getMaxX() + "," + bbox.getMaxY());
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        if (crs == null) {
            throw new WPSException("The BBOX parameter must have a coordinate reference system");
        } else {
            // handle possible axis flipping by changing the WMS version accordingly
            Integer code = CRS.lookupEpsgCode(crs, false);
            if (CRS.getAxisOrder(crs) == CRS.AxisOrder.EAST_NORTH) {
                template.put("version", "1.1.0");
                template.put("srs", "EPSG:" + code);
            } else {
                template.put("version", "1.3.0");
                template.put("crs", "EPSG:" + code);
            }
        }

        // loop over layers and accumulate
        RenderedImage result = null;
        for (Layer layer : layers) {
            RenderedImage image;
            if (layer.getCapabilities() == null) {
                RenderedImageMap map = renderInternalLayer(layer, template);
                image = map.getImage();
            } else {
                throw new UnsupportedOperationException("Including cascaded layers is not yet implemented");
            }

            if (result == null) {
                result = image;
            } else {
                result = mergeImage(result, image);

            }
            
            // past the first layer switch transparency on to allow overlaying
            template.put("transparent", "true");
        }

        // Decoration handling, we'll put together a empty GetMap for it
        GetMapRequest request = new GetMapRequest();
        request.setFormat(format.getName());
        if (decorationName != null) {
            request.setFormatOptions(Collections.singletonMap("layout", decorationName));
            WMSMapContent content = new WMSMapContent(request);
            content.setMapWidth(width);
            content.setMapHeight(height);
            content.setTransparent(true);
            RenderedImageMapOutputFormat renderer = new RenderedImageMapOutputFormat(wms);
            RenderedImageMap map = renderer.produceMap(content);
            
            result = mergeImage(result, map.getImage());
        }
        

        // encode the output by faking a normal request
        List<RenderedImageMapResponse> encoders = GeoServerExtensions.extensions(RenderedImageMapResponse.class);
        Operation op = new Operation("GetMap", service, null, new Object[]{request});
        for (RenderedImageMapResponse encoder : encoders) {
            if (encoder.canHandle(op)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                encoder.formatImageOutputStream(result, bos, new WMSMapContent(request));
                return new ByteArrayRawData(bos.toByteArray(), format.getName());
            }
        }

        throw new WPSException("Could not find a map encoder for format: " + format);
    }

    public RenderedImage mergeImage(RenderedImage result, RenderedImage image) {
        // make sure we can paint on it
        if (!(result instanceof BufferedImage)) {
            result = PlanarImage.wrapRenderedImage(image).getAsBufferedImage();
        }

        // could use mosaic here, but would require keeping all images in memory to build the op,
        // this way at most two at any time are around, so uses less memory overall
        BufferedImage bi = (BufferedImage) result;
        Graphics2D graphics = (Graphics2D) bi.getGraphics();
        graphics.drawRenderedImage(image, AffineTransform.getScaleInstance(1, 1));
        graphics.dispose();
        return result;
    }

    public RenderedImageMap renderInternalLayer(Layer layer, Map kvpTemplate) throws Exception {
        WMSMapContent mapContent;
        GetMapRequest request = getMapReader.createRequest();

        // prepare raw and parsed KVP maps to mimick a GetMap request
        CaseInsensitiveMap rawKvp = new CaseInsensitiveMap(new HashMap());
        rawKvp.putAll(kvpTemplate);
        rawKvp.put("format", "image/png"); // fake format, we are building a RenderedImage
        rawKvp.put("layers", layer.getName());
        for (Parameter parameter : layer.getParameters()) {
            rawKvp.putIfAbsent(parameter.key, parameter.value);
        }
        // for merging layers, unless the request stated otherwise
        rawKvp.putIfAbsent("transparent", "true");
        CaseInsensitiveMap kvp = new CaseInsensitiveMap(new HashMap());
        kvp.putAll(rawKvp);
        List<Throwable> exceptions = KvpUtils.parse(kvp);
        if (exceptions != null && exceptions.size() > 0) {
            throw new WPSException("Failed to build map for layer: " + layer.getName(), exceptions.get(0));
        }

        // parse
        getMapReader.read(request, kvp, rawKvp);

        // render
        GetMap mapBuilder = new GetMap(wms);
        RenderedImageMap map = (RenderedImageMap) mapBuilder.run(request);
        return map;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.wms.setApplicationContext(applicationContext);
        List<Service> services = GeoServerExtensions.extensions(Service.class, applicationContext);
        this.service = services.stream().filter(s -> "WMS".equalsIgnoreCase(s.getId())).findFirst().orElse(null);
        if (service == null) {
            throw new RuntimeException("Could not find a WMS service");
        }
    }
}
