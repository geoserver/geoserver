/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
 package org.opengeo.gsr.ms.resource;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.styling.Style;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.font.Font;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengeo.gsr.core.renderer.ClassBreakInfo;
import org.opengeo.gsr.core.renderer.ClassBreaksRenderer;
import org.opengeo.gsr.core.renderer.Renderer;
import org.opengeo.gsr.core.renderer.SimpleRenderer;
import org.opengeo.gsr.core.renderer.StyleEncoder;
import org.opengeo.gsr.core.renderer.UniqueValueInfo;
import org.opengeo.gsr.core.renderer.UniqueValueRenderer;
import org.opengeo.gsr.core.symbol.MarkerSymbol;
import org.opengeo.gsr.core.symbol.Outline;
import org.opengeo.gsr.core.symbol.PictureMarkerSymbol;
import org.opengeo.gsr.core.symbol.SimpleFillSymbol;
import org.opengeo.gsr.core.symbol.SimpleFillSymbolEnum;
import org.opengeo.gsr.core.symbol.SimpleLineSymbol;
import org.opengeo.gsr.core.symbol.SimpleLineSymbolEnum;
import org.opengeo.gsr.core.symbol.SimpleMarkerSymbol;
import org.opengeo.gsr.core.symbol.SimpleMarkerSymbolEnum;
import org.opengeo.gsr.core.symbol.Symbol;
import org.opengeo.gsr.core.symbol.TextSymbol;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.noelios.restlet.util.Base64;
import com.thoughtworks.xstream.core.BaseException;

public class LegendResource extends Resource {
    private final Catalog catalog;
    private final String format;
    
    private final static Variant JSON = new Variant(MediaType.APPLICATION_JSON);
    
    public LegendResource(Context context, Request request, Response response, Catalog catalog, String format) {
        super(context, request, response);
        this.catalog = catalog;
        this.format = format;
        getVariants().add(JSON);
    }
    
    @Override
    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (IllegalArgumentException e) {
                return buildJsonError(new ServiceError(400, "Invalid arguments from client", Arrays.asList(e.getMessage())));
            }
        }
        return super.getRepresentation(variant);
    }
    
    private Representation buildJsonError(ServiceError error) {
        getResponse().setStatus(new Status(error.getCode()));
        GeoServicesJsonFormat format = new GeoServicesJsonFormat();
        return format.toRepresentation(error);
    }
    
    private Representation buildJsonRepresentation() {
        if (!"json".equals(format)) throw new IllegalArgumentException("json is the only supported format");
        String workspaceName = (String) getRequest().getAttributes().get("workspace");
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        if (workspace == null) {
            throw new NoSuchElementException("No workspace known by name: " + workspaceName);
        }
        
        List<LayerInfo> layersInWorkspace = new ArrayList<LayerInfo>();
        for (LayerInfo l : catalog.getLayers()) {
            if (l.getType() == PublishedType.VECTOR && l.getResource().getStore().getWorkspace().getName().equals(workspaceName)) {
                layersInWorkspace.add(l);
            }
        }
        Collections.sort(layersInWorkspace, LayerNameComparator.INSTANCE);
        return new JsonLegendRepresentation(layersInWorkspace);
    }
    
    private final static class JsonLegendRepresentation extends OutputRepresentation {
        private final List<LayerInfo> layers;
        public JsonLegendRepresentation(List<LayerInfo> layers) {
            super(MediaType.APPLICATION_JSON);
            this.layers = layers;
        }
        
        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            json.object().key("layers").array();
            for (int i = 0; i < layers.size(); i++) {
                json.object();
                json.key("layerId").value(i);
                json.key("layerName").value(layers.get(i).getName());
                json.key("layerType").value("Feature Layer");
                json.key("minScale").value(0);
                json.key("maxScale").value(0);
                json.key("legend").array();
                Renderer renderer = StyleEncoder.effectiveRenderer(layers.get(i));
                if (renderer instanceof SimpleRenderer) {
                    SimpleRenderer simpleRenderer = (SimpleRenderer) renderer;
                    encodeSymbol(json, simpleRenderer.getLabel(), simpleRenderer.getSymbol());
                } else if (renderer instanceof ClassBreaksRenderer) {
                    ClassBreaksRenderer classBreaksRenderer = (ClassBreaksRenderer) renderer;
                    for (ClassBreakInfo classBreakInfo : classBreaksRenderer.getClassBreakInfos()) {
                        encodeSymbol(json, classBreakInfo.getLabel(), classBreakInfo.getSymbol());
                    }
                } else if (renderer instanceof UniqueValueRenderer) {
                    UniqueValueRenderer uniqueValueRenderer = (UniqueValueRenderer) renderer;
                    for (UniqueValueInfo uniqueValueInfo : uniqueValueRenderer.getUniqueValueInfos()) {
                        encodeSymbol(json, uniqueValueInfo.getLabel(), uniqueValueInfo.getSymbol());
                    }
                }
                json.endArray();
                json.endObject();
            }

            json.endArray().endObject();
            writer.flush();
            writer.close();
        }
    }

    private static void encodeSymbol(JSONBuilder json, String label, Symbol symbol) {
        final String encodedImage = encodeImageSymbol(symbol);
        json.object().key("label").value(label)
            .key("contentType").value("image/png")
            .key("imageData").value(encodedImage)
            .endObject();
    }
    
    private static String encodeImageSymbol(Symbol symbol) {
        BufferedImage image = prepareImage();
        Graphics2D canvas = image.createGraphics();
        
        try {
            if (symbol instanceof SimpleMarkerSymbol) {
                SimpleMarkerSymbol simpleMarkerSymbol = (SimpleMarkerSymbol) symbol;
                Shape shape = shapeForStyle(simpleMarkerSymbol.getStyle());
                Color fillColor = colorForRGBA(simpleMarkerSymbol.getColor());
                Color strokeColor = colorForRGBA(simpleMarkerSymbol.getOutline().getColor());
                Stroke stroke = new BasicStroke(simpleMarkerSymbol.getOutline().getWidth());
                
                canvas.setColor(fillColor);
                canvas.fill(shape);
                
                canvas.setColor(strokeColor);
                canvas.setStroke(stroke);
                canvas.draw(shape);
            } else if (symbol instanceof PictureMarkerSymbol) {
                // TODO: Implement image preview
            } else if (symbol instanceof TextSymbol) {
                // TODO: Implement font preview
            } else if (symbol instanceof SimpleFillSymbol) {
                SimpleFillSymbol simpleFillSymbol = (SimpleFillSymbol) symbol;
                final Shape sample = samplePolygon();
                final Color fillColor = colorForRGBA(simpleFillSymbol.getColor());
                final Stroke stroke = strokeForLineSymbol(simpleFillSymbol.getOutline());
                final Color strokeColor = colorForRGBA(simpleFillSymbol.getOutline().getColor());
                
                canvas.setColor(fillColor);
                canvas.fill(sample);
                
                canvas.setStroke(stroke);
                canvas.setColor(strokeColor);
                canvas.draw(sample);
            } else if (symbol instanceof SimpleLineSymbol) {
                SimpleLineSymbol simpleLineSymbol = (SimpleLineSymbol) symbol;
                final Shape sample = sampleLine();
                final Stroke stroke = strokeForLineSymbol(simpleLineSymbol);
                final Color color = colorForRGBA(simpleLineSymbol.getColor());
                
                canvas.setStroke(stroke);
                canvas.setColor(color);
                canvas.draw(sample);
            }
        } finally {
            canvas.dispose();
        }
        byte[] buff = toPNGBytes(image);
        return Base64.encodeBytes(buff, Base64.DONT_BREAK_LINES); // ArcGIS doesn't break at 76 columns, so neither do we.
    }
    
    private static Stroke strokeForLineSymbol(SimpleLineSymbol outline) {
        return new BasicStroke((float) outline.getWidth());
    }

    private static final double MARKER_SIZE = 26;
    private static final double HALF_SIZE = MARKER_SIZE / 2;
    private static final double OFFSET = MARKER_SIZE / 16;
    
    private static Color colorForRGBA(int[] rgba) {
        return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }
    
    private static Shape shapeForStyle(SimpleMarkerSymbolEnum style) {
        switch (style) {
        case CIRCLE: 
            return new Ellipse2D.Double(0, 0, MARKER_SIZE, MARKER_SIZE);
        case CROSS:
            GeneralPath cross = new GeneralPath();
            cross.moveTo(0, HALF_SIZE - OFFSET);
            cross.lineTo(HALF_SIZE - OFFSET, HALF_SIZE - OFFSET);
            cross.lineTo(HALF_SIZE - OFFSET, 0);
            cross.lineTo(HALF_SIZE + OFFSET, 0);
            cross.lineTo(HALF_SIZE + OFFSET, HALF_SIZE - OFFSET);
            cross.lineTo(MARKER_SIZE, HALF_SIZE - OFFSET);
            cross.lineTo(MARKER_SIZE, HALF_SIZE + OFFSET);
            cross.lineTo(HALF_SIZE + OFFSET, HALF_SIZE + OFFSET);
            cross.lineTo(HALF_SIZE + OFFSET, MARKER_SIZE);
            cross.lineTo(HALF_SIZE - OFFSET, MARKER_SIZE);
            cross.lineTo(HALF_SIZE - OFFSET, HALF_SIZE + OFFSET);
            cross.lineTo(0, HALF_SIZE + OFFSET);
            cross.lineTo(0, HALF_SIZE - OFFSET);
            return cross;
        case DIAMOND:
            GeneralPath diamond = new GeneralPath();
            diamond.moveTo(0, HALF_SIZE);
            diamond.lineTo(HALF_SIZE, 0);
            diamond.lineTo(MARKER_SIZE, HALF_SIZE);
            diamond.lineTo(HALF_SIZE, MARKER_SIZE);
            diamond.lineTo(0, HALF_SIZE);
            return diamond;
        case SQUARE:
            return new Rectangle2D.Double(0, 0, MARKER_SIZE, MARKER_SIZE);
        case X:
            GeneralPath x = new GeneralPath();
            x.moveTo(0, OFFSET);
            x.lineTo(OFFSET, 0);
            x.lineTo(HALF_SIZE, HALF_SIZE - OFFSET);
            x.lineTo(MARKER_SIZE - OFFSET, 0);
            x.lineTo(MARKER_SIZE, OFFSET);
            x.lineTo(HALF_SIZE + OFFSET, HALF_SIZE);
            x.lineTo(MARKER_SIZE, MARKER_SIZE - OFFSET);
            x.lineTo(MARKER_SIZE - OFFSET, MARKER_SIZE);
            x.lineTo(HALF_SIZE, HALF_SIZE + OFFSET);
            x.lineTo(OFFSET, MARKER_SIZE);
            x.lineTo(0, MARKER_SIZE - OFFSET);
            x.lineTo(HALF_SIZE - OFFSET, HALF_SIZE);
            x.lineTo(0, OFFSET);
            return x;
        default: throw new IllegalArgumentException("Unknown SimpleMarkerSymbolEnum: " + style);
        }
    }
    
    private static Shape samplePolygon() {
        GeneralPath polygon = new GeneralPath();
        polygon.moveTo(0, OFFSET);
        polygon.lineTo(OFFSET, 0);
        polygon.lineTo(HALF_SIZE, HALF_SIZE - OFFSET);
//        polygon.lineTo(MARKER_SIZE - OFFSET, 0);
        polygon.lineTo(MARKER_SIZE, OFFSET);
        polygon.lineTo(HALF_SIZE + OFFSET, HALF_SIZE);
        polygon.lineTo(MARKER_SIZE, MARKER_SIZE - OFFSET);
        polygon.lineTo(MARKER_SIZE - OFFSET, MARKER_SIZE);
//        polygon.lineTo(HALF_SIZE, HALF_SIZE + OFFSET);
        polygon.lineTo(OFFSET, MARKER_SIZE);
        polygon.lineTo(0, MARKER_SIZE - OFFSET);
        polygon.lineTo(HALF_SIZE - OFFSET, HALF_SIZE);
        polygon.lineTo(0, OFFSET);
        return polygon;
    }
    
    private static Shape sampleLine() {
        GeneralPath line = new GeneralPath();
        line.moveTo(HALF_SIZE, 0);
        line.lineTo(HALF_SIZE - OFFSET, HALF_SIZE);
        line.lineTo(HALF_SIZE + OFFSET, HALF_SIZE);
        line.lineTo(HALF_SIZE, MARKER_SIZE);
        return line;
    }
    
    private static byte[] toPNGBytes(BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        try {
            ImageIO.write(image,  "PNG", bytes);
        } catch (IOException e) {
            throw new RuntimeException("Writing to ByteArrayOutputStream should not throw IOException", e);
        }
        
        return bytes.toByteArray();
    }

    private static BufferedImage prepareImage() {
        return new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
    }
}
