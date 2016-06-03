/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.awt.Color;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.renderer.lite.gridcoverage2d.GradientColorMapGenerator;
import org.geotools.styling.ColorMap;
import org.opengis.filter.capability.FunctionName;

/**
 * Filter function to generate a {@link ColorMap} on top of an SVG file contained within the styles data folder
 * 
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class FilterFunction_svgColorMap extends FunctionExpressionImpl {
    
    public static final Color TRANSPARENT_COLOR = new Color(0,0,0,0); 
    
    public static FunctionName NAME = new FunctionNameImpl("colormap",
            parameter("colormap", ColorMap.class),
            parameter("name", String.class),
            parameter("min", Number.class),
            parameter("max", Number.class),
            parameter("beforeColor", String.class, 0, 1),
            parameter("afterColor", String.class, 0, 1));

    public FilterFunction_svgColorMap() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String colorMap = getParameters().get(0).evaluate(feature, String.class);
        double min = getParameters().get(1).evaluate(feature, Double.class).doubleValue();
        double max = getParameters().get(2).evaluate(feature, Double.class).doubleValue();
        String beforeColor = null;
        String afterColor = null;
        int expressionCount = getParameters().size();
        if(expressionCount >= 4) {
            beforeColor = getParameters().get(3).evaluate(feature, String.class);
        }
        if(expressionCount >= 5) {
            afterColor = getParameters().get(4).evaluate(feature, String.class);
        }
        return evaluate(colorMap, min, max, beforeColor, afterColor);
    }

    public Object evaluate(String colorMap, final double min, final double max, String beforeColor, String afterColor) {
        GradientColorMapGenerator generator = null;
        Resource xmlFile = null;
        if (!colorMap.startsWith(GradientColorMapGenerator.RGB_INLINEVALUE_MARKER)
                && !colorMap.startsWith(GradientColorMapGenerator.RGBA_INLINEVALUE_MARKER)
                && !colorMap.startsWith(GradientColorMapGenerator.HEX_INLINEVALUE_MARKER)) {
            
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            colorMap = colorMap.replace('\\', '/');
            
            String path = Paths.path("styles", "ramps", colorMap + ".svg");
            
            xmlFile = loader.get( path );
            if( xmlFile.getType() != Type.RESOURCE ){
                throw new IllegalArgumentException(
                        "The specified colorMap do not exist in the styles/ramps folder\n"
                                + "Check that "
                                + path
                                + " exists and is an .svg file");
            }
        }
        try {
            if (xmlFile != null) {
                generator = GradientColorMapGenerator.getColorMapGenerator(xmlFile.file());
            } else {
                generator = GradientColorMapGenerator.getColorMapGenerator(colorMap);
            }
            generator.setBeforeColor(beforeColor);
            generator.setAfterColor(afterColor);
            return generator.generateColorMap(min, max);
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException("Filter Function problem for function colormap", e);
        }
    }
}
