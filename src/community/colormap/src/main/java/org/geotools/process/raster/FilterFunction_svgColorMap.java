package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.io.File;

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
    
    /** "/ramps/" or "\ramps\" depending on platform */
    private final static String STYLES_RAMP = File.separatorChar + "ramps" + File.separatorChar;

    public static FunctionName NAME = new FunctionNameImpl("colormap",
            parameter("colormap", ColorMap.class),
            parameter("name", String.class),
            parameter("min", Number.class),
            parameter("max", Number.class));

    public FilterFunction_svgColorMap() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String arg0 = (getExpression(0).evaluate(feature, String.class));
        double arg1 = (getExpression(1).evaluate(feature, Double.class)).doubleValue();
        double arg2 = (getExpression(2).evaluate(feature, Double.class)).doubleValue();
        return evaluate(arg0, arg1, arg2);
    }

    public Object evaluate(String colorMap, final double min, final double max) {
        GradientColorMapGenerator generator = null;
        Resource xmlFile = null;
        if (!colorMap.startsWith(GradientColorMapGenerator.RGB_INLINEVALUE_MARKER)
                && !colorMap.startsWith(GradientColorMapGenerator.HEX_INLINEVALUE_MARKER)) {
            
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            colorMap = colorMap.replace('\\', '/');
            
            String path = Paths.path("styles","ramps", colorMap+"svg");
            
            xmlFile = loader.get( path );
            if( xmlFile.getType() != Type.RESOURCE ){
                throw new IllegalArgumentException(
                        "The specified colorMap do not exist in the styles/ramp folder\n"
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
            return generator.generateColorMap(min, max);
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException("Filter Function problem for function colormap", e);
        }
    }
}
