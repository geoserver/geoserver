package org.opengeo.gsr.core.renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.opengeo.gsr.core.geometry.GeometryTypeEnum;
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
import org.opengis.filter.expression.Expression;
import org.opengis.style.Fill;
import org.opengis.style.GraphicalSymbol;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import net.sf.json.util.JSONBuilder;

import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

public class StyleEncoder {
//    public static void defaultFillStyle(JSONBuilder json) {
//        json.object()
//          .key("type").value("simple")
//          .key("symbol").object()
//            .key("type").value("esriSFS")
//            .key("style").value("esriSFSSolid")
//            .key("color");
//            color(json, 255, 0, 0, 255);
//            json.key("outline").object()
//              .key("type").value("esriSLS")
//              .key("style").value("esriSLSSolid")
//              .key("color");
//              color(json, 0,  0, 0, 255);
//              json.key("width").value(1)
//            .endObject()
//          .endObject()
//          .key("label").value("")
//          .key("description").value("")
//        .endObject();
//    }
//    
//    public static void defaultRasterStyle(JSONBuilder json) {
//        json.object()
//          .key("type").value("simple")
//          .key("symbol").object()
//            .key("type").value("esriSFS")
//            .key("style").value("esriSFSSolid")
//            .key("color").value(null)
//            .key("outline").object()
//              .key("type").value("esriSLS")
//              .key("style").value("esriSLSSolid")
//              .key("color").value(null)
//              .key("width").value(1)
//            .endObject()
//          .endObject()
//          .key("label").value("")
//          .key("description").value("")
//        .endObject();
//    }
//
//    public static void defaultLineStyle(JSONBuilder json) {
//        json.object()
//          .key("type").value("simple")
//          .key("symbol");
//          encodeLineStyle(json, new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, components(Color.RED, 1d), 1d));
//          json.key("label").value("")
//          .key("description").value("");
//        json.endObject();
//    }
//
//    public static void defaultMarkStyle(JSONBuilder json) {
//        json.object()
//          .key("type").value("simple")
//          .key("symbol").object()
//            .key("type").value("esriSMS")
//            .key("style").value("esriSMSCircle")
//            .key("color");
//            color(json, 255, 0, 0, 255);
//            json.key("outline").object()
//              .key("type").value("esriSLS")
//              .key("style").value("esriSLSSolid")
//              .key("color");
//              color(json, 0, 0, 0, 255);
//              json.key("width").value("1");
//            json.endObject()
//          .endObject()
//          .key("label").value("")
//          .key("description").value("")
//        .endObject();
//    }
//    
//    private static void color(JSONBuilder json, int r, int g, int b, int a) {
//        json.array().value(r).value(g).value(b).value(a).endArray();
//    }
    
    public static Renderer styleToRenderer(Style style) {
                
        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        if (featureTypeStyles == null || featureTypeStyles.size() != 1) return null;
        
        FeatureTypeStyle featureTypeStyle = featureTypeStyles.get(0);
        if (featureTypeStyle == null) return null; 
        
        List<Rule> rules = featureTypeStyle.rules();
        if (rules == null || rules.size() == 0) return null;
        
        Renderer render = rulesToUniqueValueRenderer(rules);
        if (render != null) return render;
        
        render = rulesToClassBreaksRenderer(rules);
        if (render != null) return render;

        final Symbolizer symbolizer = getSingleSymbolizer(style);
        if (symbolizer != null) {
            final Symbol symbol = symbolizerToSymbol(symbolizer);
            if (symbol != null) {
                return new SimpleRenderer(symbol, "", "");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    private static Renderer rulesToClassBreaksRenderer(List<Rule> rules) {
        List<Rule> rulesOther = new LinkedList<Rule>();
        Map<String, ClassBreaksRenderer> map = new LinkedHashMap<String, ClassBreaksRenderer>();
        for (Rule rule : rules) {
            ClassBreakInfoMeta meta = ruleToClassBreakInfoMeta(rule);
            if (meta != null) {
                ClassBreaksRenderer renderer = map.get(meta.propertyName);
                if (renderer == null) {
                    double minValue = 0;
                    renderer = new ClassBreaksRenderer(meta.propertyName, 
                            minValue,
                            new LinkedList<ClassBreakInfo>());
                    map.put(meta.propertyName, renderer);
                }
                renderer.getClassBreakInfos().add(meta.classBreakInfo);
            } else {
                rulesOther.add(rule);
            }
        }

        if (map.size() == 1 && rulesOther.size() <= 1) {
            ClassBreaksRenderer classBreaksRenderer = map.values().iterator().next();
            if (rulesOther.size() == 1) {
                // assuming remaining rule is the default.
                Rule rule = rulesOther.get(0);
                String title = rule.getTitle();
                if (title == null) title = "";
                // classBreaksRenderer.setDefaultLabel(title);
                // classBreaksRenderer.setDefaultSymbol(symbolizerToSymbol(rule.symbolizers().get(0)));
            }
            return classBreaksRenderer;
        }

        return null;
    }

    private static class ClassBreakInfoMeta {
        final String propertyName;
        final ClassBreakInfo classBreakInfo;
        public ClassBreakInfoMeta(String propertyName, ClassBreakInfo classBreakInfo) {
            this.propertyName = propertyName;
            this.classBreakInfo = classBreakInfo;
        }
    }

    private static ClassBreakInfoMeta ruleToClassBreakInfoMeta(Rule rule) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        if (symbolizers == null || symbolizers.size() != 1) return null;

        Symbolizer symbolizer = symbolizers.get(0);
        if (symbolizer == null) return null;

        Filter filter = rule.getFilter();
        if (!(filter instanceof And)) return null;

        And classBreakFilter = (And) filter;
        List<Filter> children = classBreakFilter.getChildren();

        if (children == null || children.size() != 2) return null;

        Filter child1 = children.get(0);
        if (!(child1 instanceof PropertyIsGreaterThanOrEqualTo || child1 instanceof PropertyIsGreaterThan)) return null;
        BinaryComparisonOperator lowerBound = (BinaryComparisonOperator) child1;

        Filter child2 = children.get(1);
        if (!(child2 instanceof PropertyIsLessThanOrEqualTo || child2 instanceof PropertyIsLessThan)) return null;
        BinaryComparisonOperator upperBound = (BinaryComparisonOperator) child2;
        Expression property1 = lowerBound.getExpression1();
        Expression property2 = upperBound.getExpression1();

        if (property1 == null || property2 == null || !(property1.equals(property2))) {
            return null;
        }
        if (!(property1 instanceof PropertyName)) {
            return null;
        }
        String propertyName = ((PropertyName) property1).getPropertyName();

        Expression min = lowerBound.getExpression2();
        if (!(min instanceof Literal)) {
            return null;
        }
        Double minAsDouble = ((Literal)min).evaluate(null, double.class);

        Expression max = upperBound.getExpression2();
        if (!(max instanceof Literal)) {
            return null;
        }
        Double maxAsDouble = ((Literal)max).evaluate(null, double.class);
        return new ClassBreakInfoMeta(propertyName, new ClassBreakInfo(maxAsDouble, "", "", symbolizerToSymbol(symbolizer)));
    }
    
    private static Renderer rulesToUniqueValueRenderer(List<Rule> rules) {
        List<Rule> rulesOther = new LinkedList<Rule>();
        Map<String, UniqueValueRenderer> map = new LinkedHashMap<String, UniqueValueRenderer>();
        for (Rule rule : rules) {
            UniqueValueInfoMeta meta = ruleToUniqueValueInfoMeta(rule);
            if (meta != null) {
                UniqueValueRenderer renderer = map.get(meta.propertyName);
                if (renderer == null) {
                    renderer = new UniqueValueRenderer(
                            meta.propertyName,
                            null, // field 2
                            null, // field 3
                            ", ", // delimiter, required even with single field
                            null, // default symbol (set later)
                            null, // default label (set later)
                            new LinkedList<UniqueValueInfo>());
                    map.put(meta.propertyName, renderer);
                }
                renderer.getUniqueValueInfos().add(meta.uniqueValueInfo);
            } else {
                rulesOther.add(rule);
            }
        }
        if (map.size() == 1 && rulesOther.size() <= 1) {
            UniqueValueRenderer uniqueValueRenderer = map.values().iterator().next();
            if (rulesOther.size() == 1) {
                // an assumption here: if there's one rule left over after
                // a bunch of PropertyEqualTo let's assume it's the default.
                // Robust programatic parsing of typical defaults written in SLD
                // will be a PITA, let's defer.
                Rule rule = rulesOther.get(0);
                String title = rule.getTitle();
                if (title == null) title = "";
                uniqueValueRenderer.setDefaultLabel(title);
                uniqueValueRenderer.setDefaultSymbol(symbolizerToSymbol(rule.symbolizers().get(0)));
            }
            return uniqueValueRenderer;
        }
        return null;
    }
    
    private static class UniqueValueInfoMeta {
        final String propertyName;
        final UniqueValueInfo uniqueValueInfo;
        public UniqueValueInfoMeta(String propertyName, UniqueValueInfo uniqueValueInfo) {
            this.propertyName = propertyName;
            this.uniqueValueInfo = uniqueValueInfo;
        }
    }
    
    private static UniqueValueInfoMeta ruleToUniqueValueInfoMeta(Rule rule) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        if (symbolizers == null || symbolizers.size() != 1) return null;
        
        Symbolizer symbolizer = symbolizers.get(0);
        if (symbolizer == null) return null;
        
        Filter filter = rule.getFilter();
        if (!(filter instanceof PropertyIsEqualTo)) return null;
        
        PropertyIsEqualTo uniqueValueFilter = (PropertyIsEqualTo)filter;
        
        Expression expression1 = uniqueValueFilter.getExpression1();
        String propertyName = expression1 instanceof PropertyName ?
                ((PropertyName)expression1).getPropertyName() : null;
        if (propertyName == null) return null;
        
        Expression expression2 = uniqueValueFilter.getExpression2();
        String valueAsString = expression2 instanceof Literal ?
                ((Literal)expression2).getValue().toString() : null;
        if (valueAsString == null) return null;
        
        String title = rule.getTitle();
        if (title == null) title = "";
        String description = rule.getAbstract();
        if (description == null) description = "";
        
        return new UniqueValueInfoMeta(propertyName,
                new UniqueValueInfo(valueAsString, title, description,
                    symbolizerToSymbol(symbolizer)));    
    }
    
    private static Renderer defaultPolyRenderer() {
        SimpleLineSymbol outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, new int[] { 0, 0, 0, 255 }, 1);
        Symbol symbol = new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, new int[] { 255, 0, 0, 255 }, outline);
        return new SimpleRenderer(symbol, "Polygon", "Default polygon renderer");
    }
    
    private static Renderer defaultRasterRenderer() {
        SimpleLineSymbol outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, null, 1);
        Symbol symbol = new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, null, outline);
        return new SimpleRenderer(symbol, "Raster", "Default raster renderer");
    }
    
    private static Renderer defaultLineRenderer() {
        SimpleLineSymbol outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, new int[] { 0, 0, 0, 255 }, 1);
        return new SimpleRenderer(outline, "Line", "Default line renderer");
    }
    
    private static Renderer defaultMarkRenderer() {
        Outline outline = new Outline(new int[] { 0, 0, 0, 255 }, 1);
        SimpleMarkerSymbol marker = new SimpleMarkerSymbol(SimpleMarkerSymbolEnum.SQUARE, new int[] { 255, 0, 0, 255 }, 24, 0, 0, 0, outline);
        return new SimpleRenderer(marker, "Marker", "Default marker renderer");
    }
    
    public static Renderer effectiveRenderer(LayerInfo layer) throws IOException {
        Renderer renderer = null;
        
        StyleInfo styleInfo = layer.getDefaultStyle();
        if (styleInfo != null) {
            Style style = styleInfo.getStyle();
            if (style != null) {
                renderer = styleToRenderer(style);
            }
        }
        
        if (renderer == null) {
            GeometryTypeEnum gtype = GeometryTypeEnum.forResourceDefaultGeometry(layer.getResource());
            switch (gtype) {
            case ENVELOPE:
            case POLYGON:
                if (layer.getResource() instanceof CoverageInfo) {
                    renderer = defaultRasterRenderer();
                } else {
                    renderer = defaultPolyRenderer(); // TODO: Generate default polygon style
                }
                break;
            case MULTIPOINT:
            case POINT:
                renderer = defaultMarkRenderer(); // TODO: Generate default point style
                break;
            case POLYLINE:
                renderer = defaultLineRenderer(); // TODO: Generate default line style;
                break;
            default:
                renderer = null;
            }
        }
        
        return renderer;
    }
    
    private static Symbol symbolizerToSymbol(Symbolizer sym) {
        if (sym instanceof PointSymbolizer) {
            return pointSymbolizerToMarkSymbol((PointSymbolizer)sym);
        } else if (sym instanceof LineSymbolizer) {
            return lineSymbolizerToLineSymbol((LineSymbolizer)sym);
        } else if (sym instanceof PolygonSymbolizer) {
            return polygonSymbolizerToFillSymbol((PolygonSymbolizer)sym);
        } else return null; // TODO: Should we throw here?
    }
    
    private static Symbol polygonSymbolizerToFillSymbol(PolygonSymbolizer sym) {
        final Fill fill = sym.getFill();
        final Stroke stroke = sym.getStroke();
        final Color color;
        final double opacity;
        final SimpleLineSymbol outline;
        if (fill != null) {
            color = evaluateWithDefault(fill.getColor(), Color.GRAY);
            opacity = evaluateWithDefault(fill.getOpacity(), 1d);
        } else {
            color = Color.GRAY;
            opacity = 1d;
        }
        if (stroke != null) {
            Color strokeColor = evaluateWithDefault(stroke.getColor(), Color.BLACK);
            double strokeOpacity = evaluateWithDefault(stroke.getOpacity(), 1d);
            double strokeWidth = evaluateWithDefault(stroke.getWidth(), 1d);
            outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, components(strokeColor, strokeOpacity), strokeWidth);
        } else {
            outline = new SimpleLineSymbol(
                SimpleLineSymbolEnum.SOLID,
                components(Color.BLACK, 1),
                1);
        }
        
        return new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, components(color, opacity), outline);
    }

    private static MarkerSymbol pointSymbolizerToMarkSymbol(PointSymbolizer sym) {
        if (sym.getGraphic() == null) return null;
        if (sym.getGraphic().graphicalSymbols().size() != 1) return null; // REVISIT: should we throw instead?
        GraphicalSymbol symbol = sym.getGraphic().graphicalSymbols().get(0);
        if (symbol instanceof Mark) {
            Mark mark = (Mark)symbol;
            String markName = evaluateWithDefault(mark.getWellKnownName(), "circle");
            final Color color;
            final double opacity;
            final double size;
            final double angle = evaluateWithDefault(sym.getGraphic().getRotation(), 0d);
            final double xoffset;
            final double yoffset;
            Fill fill = mark.getFill();
            if (fill != null) {
                color = evaluateWithDefault(fill.getColor(), Color.GRAY);
                opacity = evaluateWithDefault(fill.getOpacity(), 1d);
                size = evaluateWithDefault(sym.getGraphic().getSize(), 16);
            } else {
                color = Color.GRAY;
                opacity = 1d;
                size = 16d;
            }
            Displacement displacement = sym.getGraphic().getDisplacement();
            if (displacement != null) {
                xoffset = evaluateWithDefault(displacement.getDisplacementX(), 0d);
                yoffset = evaluateWithDefault(displacement.getDisplacementY(), 0d);
            } else {
                xoffset = 0d;
                yoffset = 0d;
            }
            
            final Outline outline;
            final Stroke stroke = mark.getStroke();
            if (stroke != null) {
                Color strokeColor = evaluateWithDefault(stroke.getColor(), Color.BLACK);
                double strokeOpacity = evaluateWithDefault(stroke.getOpacity(), 1d);
                double strokeWidth = evaluateWithDefault(stroke.getWidth(), 1d);
                outline = new Outline(components(strokeColor, strokeOpacity), (int)Math.round(strokeWidth));
            } else {
                outline = new Outline(components(Color.BLACK, 1d), 1);
            }
            return new SimpleMarkerSymbol(
                equivalentSMS(markName),
                components(color, opacity),
                size,
                angle,
                xoffset,
                yoffset,
                outline);
        } else if (symbol instanceof ExternalGraphic) {
            ExternalGraphic exGraphic = (ExternalGraphic) symbol;
            URI resourceURI = exGraphic.getOnlineResource().getLinkage();
            byte[] rawData = new byte[4096];
            InputStream stream = null;
            try {
                stream = resourceURI.toURL().openStream();
                int pos = 0;
                int read = 0;
                while ((read = stream.read(rawData, pos, rawData.length - pos)) >= 0) {
                    pos += read;
                    if (pos == rawData.length) {
                        byte[] grown = new byte[rawData.length * 2];
                        for (int i = 0; i < pos; i++) {
                            grown[i] = rawData[i];
                        }
                        rawData = grown;
                    }
                }
                rawData = Arrays.copyOfRange(rawData, 0, pos);
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                throw new RuntimeException("IO Error while loading image icon.", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // squelch
                    }
                }
            }

            String contentType = exGraphic.getFormat();
            Color color = Color.GRAY;
            Double width = evaluateWithDefault(sym.getGraphic().getSize(), 0d);
            Double height = width;
            if (width == null) {
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(rawData));
                    width = (double) image.getWidth();
                    height = (double) image.getHeight();
                } catch (IOException e) {
                    width = 16d;
                    height = width;
                }
            }
            double angle = evaluateWithDefault(sym.getGraphic().getRotation(), 0d);
            Displacement displacement = sym.getGraphic().getDisplacement();
            int xoffset = displacement != null ? evaluateWithDefault(sym.getGraphic().getDisplacement().getDisplacementX(), 0) : 0;
            int yoffset = displacement != null ? evaluateWithDefault(sym.getGraphic().getDisplacement().getDisplacementY(), 0) : 0;

            String url = relativizeExternalGraphicImageResourceURI(resourceURI);
            return new PictureMarkerSymbol(rawData, url, contentType, components(color, 1), width, height, angle, xoffset, yoffset);
        }
        return null;
    }
    
    public static SimpleLineSymbol lineSymbolizerToLineSymbol(LineSymbolizer sym) {
        Stroke stroke = sym.getStroke();
        return strokeToLineSymbol(stroke);
    }

    private static SimpleLineSymbol strokeToLineSymbol(Stroke stroke) {
        final Color color;
        final double opacity; 
        final double width; 
        float[] dashArray;
        final SimpleLineSymbolEnum lineStyle;
        if (stroke != null) {
            opacity = evaluateWithDefault(stroke.getOpacity(), 1d);
            color = evaluateWithDefault(stroke.getColor(), Color.BLACK);
            width = evaluateWithDefault(stroke.getWidth(), 1d);
            dashArray = stroke.getDashArray();
        } else {
            color = Color.BLACK;
            opacity = 1d;
            width = 1d;
            dashArray = null;
        }
        if (dashArray == null || dashArray.length == 1) {
            lineStyle = SimpleLineSymbolEnum.SOLID;
        } else {
            Set<Float> uniqueValues = new java.util.HashSet<Float>();
            for (float f : dashArray) {
                uniqueValues.add(f);
            }
            if (uniqueValues.size() == 1) {
                lineStyle = SimpleLineSymbolEnum.DASH;
            } else {
                lineStyle = SimpleLineSymbolEnum.DASH_DOT;
            }
        }
        return new SimpleLineSymbol(lineStyle, components(color, opacity), width);
    }


    public static void encodeStyle(JSONBuilder json, Style style) {
        Symbolizer sym = getSingleSymbolizer(style);
        if (sym instanceof PointSymbolizer) {
            encodePointSymbolizer(json, (PointSymbolizer) sym);
        } else if (sym instanceof LineSymbolizer) {
            encodeLineSymbolizer(json, (LineSymbolizer) sym);
        } else if (sym instanceof PolygonSymbolizer) {
            encodePolygonSymbolizer(json, (PolygonSymbolizer) sym);
        }
    }
    
    public static void encodeSymbol(JSONBuilder json, Symbol symbol) {
        if (symbol instanceof SimpleMarkerSymbol) {
            encodeMarkerSymbol(json, (SimpleMarkerSymbol) symbol);
        } else if (symbol instanceof PictureMarkerSymbol) {
            encodePictureMarkerSymbol(json, (PictureMarkerSymbol) symbol);
        } else if (symbol instanceof SimpleFillSymbol) {
            encodeFillSymbol(json, (SimpleFillSymbol) symbol);
        } else if (symbol instanceof SimpleLineSymbol) {
            encodeLineStyle(json, (SimpleLineSymbol) symbol);
        } else {
            json.value(null);
        }
    }
    
    private static void encodePolygonSymbolizer(JSONBuilder json, PolygonSymbolizer sym) {
        final Fill fill = sym.getFill();
        final Stroke stroke = sym.getStroke();
        final Color color;
        final double opacity;
        final SimpleLineSymbol outline;
        if (fill != null) {
            color = evaluateWithDefault(fill.getColor(), Color.GRAY);
            opacity = evaluateWithDefault(fill.getOpacity(), 1d);
        } else {
            color = Color.GRAY;
            opacity = 1d;
        }
        if (stroke != null) {
            Color strokeColor = evaluateWithDefault(stroke.getColor(), Color.BLACK);
            double strokeOpacity = evaluateWithDefault(stroke.getOpacity(), 1d);
            double strokeWidth = evaluateWithDefault(stroke.getWidth(), 1d);
            outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, components(strokeColor, strokeOpacity), strokeWidth);
        } else {
            outline = new SimpleLineSymbol(
                SimpleLineSymbolEnum.SOLID,
                components(Color.BLACK, 1),
                1);
        }
        
        encodeFillSymbol(json, 
                new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, components(color, opacity), outline));
    }

    private static void encodeFillSymbol(JSONBuilder json, SimpleFillSymbol sym) {
        json.object()
          .key("type").value("esriSFS")
          .key("style").value(sym.getStyle().getStyle())
          .key("color");
          writeInts(json, sym.getColor());
          json.key("outline");
          encodeLineStyle(json, sym.getOutline());
        json.endObject();
    }

    private static void encodeLineSymbolizer(JSONBuilder json, LineSymbolizer sym) {
        SimpleLineSymbol symbol = lineSymbolizerToLineSymbol(sym);
        encodeLineStyle(json, symbol);
    }
    
    private static int[] components(Color color, double opacity) {
        return new int[] { 
            color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(opacity * 255)
        };
    }

    private static void encodePointSymbolizer(JSONBuilder json, PointSymbolizer sym) {
        MarkerSymbol markSymbol = pointSymbolizerToMarkSymbol(sym);
        if (markSymbol instanceof SimpleMarkerSymbol) {
            encodeMarkerSymbol(json, (SimpleMarkerSymbol)markSymbol);
        }
    }
    
    private static void encodeMarkerSymbol(JSONBuilder json, SimpleMarkerSymbol sms) {
      json.object()
        .key("type").value("esriSMS")
        .key("style").value(sms.getStyle().getStyle())
        .key("color");
        writeInts(json, sms.getColor());
        json.key("outline").object()
          .key("type").value("SLS")
          .key("style").value("SLSSolid");
          json.key("color");
          writeInts(json, sms.getOutline().getColor());
          json.key("width").value(sms.getOutline().getWidth())
        .endObject();
        json.key("angle").value(sms.getAngle());
        json.key("size").value(sms.getSize());
        json.key("xoffset").value(sms.getXoffset());
        json.key("yoffset").value(sms.getYoffset());
      json.endObject();
    }
    
    private static void encodePictureMarkerSymbol(JSONBuilder json, PictureMarkerSymbol symbol) {
        json.object()
            .key("type").value("esriPMS")
            .key("url").value(symbol.getUrl())
            .key("imageData").value(symbol.getImageData())
            .key("contentType").value(symbol.getContentType())
            .key("width").value(symbol.getWidth())
            .key("height").value(symbol.getHeight())
            .key("angle").value(symbol.getAngle())
            .key("xoffset").value(symbol.getXoffset())
            .key("yoffset").value(symbol.getYoffset());
        json.endObject();
    }

    private static void encodeLineStyle(JSONBuilder json, SimpleLineSymbol symbol) {
        json.object()
          .key("type").value("esriSLS")
          .key("style").value(symbol.getStyle().getStyle())
          .key("color");
          writeInts(json, symbol.getColor());
          json.key("width").value(symbol.getWidth())
        .endObject();
    }

    private static void writeInts(JSONBuilder json, int[] color) {
        json.array();
        for (int c : color) json.value(c);
        json.endArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T evaluateWithDefault(Expression exp, T def) {
        if (exp == null || def == null) return def;
        try {
            return (T)exp.evaluate(null, def.getClass());
        } catch (IllegalArgumentException e) {
            return def;
        } catch (ClassCastException e) {
            return def;
        }
    }

    private static Symbolizer getSingleSymbolizer(Style style) {
        if (style.featureTypeStyles() == null) return null;
        if (style.featureTypeStyles().size() != 1) return null;
        FeatureTypeStyle ftStyle = style.featureTypeStyles().get(0);
        if (ftStyle.rules().size() != 1) return null;
        Rule rule = ftStyle.rules().get(0);
        if (rule.getFilter() != null) return null;
        if (rule.symbolizers().size() != 1) return null;
        return rule.symbolizers().get(0);
    }
    
    private static SimpleMarkerSymbolEnum equivalentSMS(String markName) {
        if ("circle".equals(markName)) {
            return SimpleMarkerSymbolEnum.CIRCLE;
        } else if ("x".equals(markName)) {
            return SimpleMarkerSymbolEnum.X;
        } else if ("cross".equals(markName)) {
            return SimpleMarkerSymbolEnum.CROSS;
        } else if ("square".equals(markName)) {
            return SimpleMarkerSymbolEnum.SQUARE;
//          SLD does not define a diamond mark (you can always just rotate the square.)
//        } else if ("diamond".equals(markName)) {
//            return SimpleMarkerSymbolEnum.DIAMOND
        } else {
            return SimpleMarkerSymbolEnum.CIRCLE;
        }
    }

    public static void encodeRenderer(JSONBuilder json, Renderer renderer) {
        if (renderer == null) {
            json.value(null);
        } else if (renderer instanceof SimpleRenderer) {
            encodeSimpleRenderer(json, (SimpleRenderer) renderer);
        } else if (renderer instanceof UniqueValueRenderer) {
            encodeUniqueValueRenderer(json, (UniqueValueRenderer) renderer);
        } else if (renderer instanceof ClassBreaksRenderer) {
        	encodeClassBreaksRenderer(json, (ClassBreaksRenderer) renderer);
        } else throw new IllegalArgumentException("Unhandled renderer " + renderer);
    }
    
    private static void encodeClassBreaksRenderer(JSONBuilder json, ClassBreaksRenderer renderer) {
		// TODO Auto-generated method stub
    	json.object()
    	  .key("type").value("classBreaks")
    	  .key("field").value(renderer.getField())
    	  .key("minValue").value(renderer.getMinValue());
    	
    	json.key("classBreakInfos").array();
    	
    	for (ClassBreakInfo info : renderer.getClassBreakInfos()) {
    		json.object().
    		     key("classMaxValue").value(info.getClassMaxValue())
    		    .key("label").value(info.getLabel())
    		    .key("description").value(info.getDescription())
    		    .key("symbol");
    		encodeSymbol(json, info.getSymbol());
    		json.endObject();
    	}
    	
    	json.endArray().endObject();
	}

	private static void encodeSimpleRenderer(JSONBuilder json, SimpleRenderer renderer) {
        json.object()
          .key("type").value("simple")
          .key("symbol");
          encodeSymbol(json, renderer.getSymbol());
          json.key("label").value(renderer.getLabel())
          .key("description").value(renderer.getDescription())
        .endObject();
    }
    
    private static void encodeUniqueValueRenderer(JSONBuilder json, UniqueValueRenderer renderer) {
        json.object()
          .key("type").value("uniqueValue")
          .key("field1").value(renderer.getField1())
          .key("field2").value(renderer.getField2())
          .key("field3").value(renderer.getField3())
          .key("fieldDelimiter").value(renderer.getFieldDelimiter())
          .key("defaultSymbol");
          encodeSymbol(json, renderer.getDefaultSymbol());
          json.key("defaultLabel").value(renderer.getDefaultLabel())
          .key("uniqueValueInfos");
          json.array();
          for (UniqueValueInfo info : renderer.getUniqueValueInfos()){
            json.object()
            .key("value").value(info.getValue())
            .key("label").value(info.getLabel())
            .key("description").value(info.getDescription())
            .key("symbol");
            encodeSymbol(json, info.getSymbol());
            json.endObject();
          }
          json.endArray()
        .endObject();
    }
	
    static String relativizeExternalGraphicImageResourceURI(URI resourceURI) {
        String path = resourceURI.getPath();
        int index = path.lastIndexOf('/');
        return "images/" + (index < 0 ? path : path.substring(index + 1));
    }
}
