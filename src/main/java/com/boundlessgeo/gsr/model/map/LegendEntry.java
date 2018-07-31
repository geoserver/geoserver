/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.map;

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

import javax.imageio.ImageIO;

import org.geotools.data.Base64;

import com.boundlessgeo.gsr.model.symbol.PictureMarkerSymbol;
import com.boundlessgeo.gsr.model.symbol.SimpleFillSymbol;
import com.boundlessgeo.gsr.model.symbol.SimpleLineSymbol;
import com.boundlessgeo.gsr.model.symbol.SimpleMarkerSymbol;
import com.boundlessgeo.gsr.model.symbol.SimpleMarkerSymbolEnum;
import com.boundlessgeo.gsr.model.symbol.Symbol;
import com.boundlessgeo.gsr.model.symbol.TextSymbol;

class LegendEntry {

    String label;
    String contentType = "image/png";
    String imageData;

    LegendEntry(String label, Symbol symbol) {
        this.label = label;
        this.imageData = encodeImageSymbol(symbol);
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
