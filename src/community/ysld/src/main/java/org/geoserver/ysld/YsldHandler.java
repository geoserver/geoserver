/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 - 2016 Boundless Spatial Inc.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ysld;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleType;
import org.geotools.data.DataUtilities;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.ysld.UomMapper;
import org.geotools.ysld.Ysld;
import org.geotools.ysld.parse.WellKnownZoomContextFinder;
import org.xml.sax.EntityResolver;
import org.geotools.ysld.parse.ZoomContextFinder;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class YsldHandler extends StyleHandler {

    public static final String FORMAT = "ysld";
    public static final String MIMETYPE = "application/vnd.geoserver.ysld+yaml";
    
    static final Map<StyleType, String> TEMPLATES = new HashMap<StyleType, String>();
    static {
        try {
            TEMPLATES.put(StyleType.POINT, IOUtils.toString(YsldHandler.class
                    .getResourceAsStream("template_point.ysld")));
            TEMPLATES.put(StyleType.POLYGON, IOUtils.toString(YsldHandler.class
                    .getResourceAsStream("template_polygon.ysld")));
            TEMPLATES.put(StyleType.LINE, IOUtils.toString(YsldHandler.class
                    .getResourceAsStream("template_line.ysld")));
            TEMPLATES.put(StyleType.RASTER, IOUtils.toString(YsldHandler.class
                    .getResourceAsStream("template_raster.ysld")));
            TEMPLATES.put(StyleType.GENERIC, IOUtils.toString(YsldHandler.class
                    .getResourceAsStream("template_generic.ysld")));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the style templates", e);
        }
    }

    /**
     * Creates a new handler with an explicit zoom finder.
     */
    public YsldHandler(ZoomContextFinder zoomFinder, UomMapper uomMapper) {
        super("YSLD", FORMAT);
        this.zoomFinder = zoomFinder;
    }

    /**
     * Creates a new handler.
     * <p>
     * The instance is created with {@link org.geotools.ysld.parse.WellKnownZoomContextFinder}
     * as the zoom context finder.
     * </p>
     */
    public YsldHandler() {
        this(WellKnownZoomContextFinder.getInstance(), new UomMapper());
    }

    @Override
    public String getFileExtension() {
        return "yaml";
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "yaml";
    }
    
    @Override
    public String getStyle(StyleType type, Color color, String colorName, String layerName) {
        String template = TEMPLATES.get(type);
        String colorCode = Integer.toHexString(color.getRGB());
        colorCode = colorCode.substring(2, colorCode.length());
        return template.replace("${colorName}", colorName).replace(
                "${colorCode}", "#" + colorCode).replace("${layerName}", layerName);
    }
    
    ZoomContextFinder zoomFinder;
    UomMapper uomMapper;
    
    @Override
    public StyledLayerDescriptor parse(Object input, Version version, @Nullable ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException {
        
        if (resourceLocator == null && input instanceof File) {
            resourceLocator = new DefaultResourceLocator();
            ((DefaultResourceLocator)resourceLocator).setSourceUrl(DataUtilities.fileToURL((File) input));
        }
        
        return Ysld.parse(toReader(input), Collections.singletonList(zoomFinder), resourceLocator, uomMapper);
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output) throws IOException {
        Ysld.encode(sld, output, uomMapper);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        return (List) Ysld.validate(toReader(input), Collections.singletonList(zoomFinder), uomMapper);
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }
}
