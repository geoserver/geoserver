/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 - 2016 Boundless Spatial Inc.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ysld;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleType;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.style.ResourceLocator;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.zoom.WellKnownZoomContextFinder;
import org.geotools.styling.zoom.ZoomContextFinder;
import org.geotools.util.URLs;
import org.geotools.util.Version;
import org.geotools.ysld.UomMapper;
import org.geotools.ysld.Ysld;
import org.xml.sax.EntityResolver;

public class YsldHandler extends StyleHandler {

    public static final String FORMAT = "ysld";
    public static final String MIMETYPE = "application/vnd.geoserver.ysld+yaml";

    static final Map<StyleType, String> TEMPLATES = new HashMap<>();

    static {
        try {
            TEMPLATES.put(
                    StyleType.POINT,
                    IOUtils.toString(YsldHandler.class.getResourceAsStream("template_point.ysld"), UTF_8));
            TEMPLATES.put(
                    StyleType.POLYGON,
                    IOUtils.toString(YsldHandler.class.getResourceAsStream("template_polygon.ysld"), UTF_8));
            TEMPLATES.put(
                    StyleType.LINE,
                    IOUtils.toString(YsldHandler.class.getResourceAsStream("template_line.ysld"), UTF_8));
            TEMPLATES.put(
                    StyleType.RASTER,
                    IOUtils.toString(YsldHandler.class.getResourceAsStream("template_raster.ysld"), UTF_8));
            TEMPLATES.put(
                    StyleType.GENERIC,
                    IOUtils.toString(YsldHandler.class.getResourceAsStream("template_generic.ysld"), UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the style templates", e);
        }
    }

    /** Creates a new handler with an explicit zoom finder. */
    public YsldHandler(List<ZoomContextFinder> zoomFinders, UomMapper uomMapper) {
        super("YSLD", FORMAT);
        this.zoomFinders = zoomFinders;
    }

    /** Creates a new handler with an explicit zoom finder. */
    public YsldHandler(GeoServerExtensions extensions, UomMapper uomMapper) {
        this(lookupZoomContextFinder(extensions), uomMapper);
    }

    private static List<ZoomContextFinder> lookupZoomContextFinder(GeoServerExtensions extensions) {
        List<ZoomContextFinder> finders = extensions.extensions(ZoomContextFinder.class);
        if (finders != null && !finders.isEmpty()) return finders;
        return List.of(WellKnownZoomContextFinder.getInstance());
    }

    /**
     * Creates a new handler.
     *
     * <p>The instance is created with {@link org.geotools.ysld.parse.WellKnownZoomContextFinder} as the zoom context
     * finder.
     */
    public YsldHandler() {
        this(List.of(WellKnownZoomContextFinder.getInstance()), new UomMapper());
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
        return template.replace("${colorName}", colorName)
                .replace("${colorCode}", "#" + colorCode)
                .replace("${layerName}", layerName);
    }

    List<ZoomContextFinder> zoomFinders;
    UomMapper uomMapper;

    @Override
    public StyledLayerDescriptor parse(
            Object input, Version version, @Nullable ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {

        if (resourceLocator == null && input instanceof File file) {
            resourceLocator = new DefaultResourceLocator();
            ((DefaultResourceLocator) resourceLocator).setSourceUrl(URLs.fileToUrl(file));
        }

        return Ysld.parse(toReader(input), zoomFinders, resourceLocator, uomMapper);
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        Ysld.encode(sld, output, uomMapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        return (List) Ysld.validate(toReader(input), zoomFinders, uomMapper);
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }

    @Override
    public boolean supportsEncoding(Version version) {
        return true;
    }

    @Override
    public URL getSpecification(Version version) throws MalformedURLException {
        return new URL("https://docs.geoserver.org/latest/en/user/styling/ysld/index.html");
    }
}
