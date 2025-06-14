/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.style.ResourceLocator;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.styling.css.CssParser;
import org.geotools.styling.css.CssTranslator;
import org.geotools.styling.css.Stylesheet;
import org.geotools.styling.zoom.ZoomContextFinder;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;
import org.xml.sax.EntityResolver;

/** Style handler for geocss. Justin Deoliveira, Boundless */
public class CssHandler extends StyleHandler implements ModuleStatus {

    public static final String FORMAT = "css";

    public static final String MIME_TYPE = "application/vnd.geoserver.geocss+css";

    static final Map<StyleType, String> TEMPLATES = new HashMap<>();

    static {
        try {
            TEMPLATES.put(
                    StyleType.POINT,
                    IOUtils.toString(CssHandler.class.getResourceAsStream("template_point.css"), UTF_8));
            TEMPLATES.put(
                    StyleType.POLYGON,
                    IOUtils.toString(CssHandler.class.getResourceAsStream("template_polygon.css"), UTF_8));
            TEMPLATES.put(
                    StyleType.LINE, IOUtils.toString(CssHandler.class.getResourceAsStream("template_line.css"), UTF_8));
            TEMPLATES.put(
                    StyleType.RASTER,
                    IOUtils.toString(CssHandler.class.getResourceAsStream("template_raster.css"), UTF_8));
            TEMPLATES.put(
                    StyleType.GENERIC,
                    IOUtils.toString(CssHandler.class.getResourceAsStream("template_generic.css"), UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the css style templates", e);
        }
    }

    private final List<ZoomContextFinder> zoomContextFinders;

    private SLDHandler sldHandler;

    protected CssHandler(GeoServerExtensions extensions, SLDHandler sldHandler) {
        super("CSS", FORMAT);
        this.sldHandler = sldHandler;
        this.zoomContextFinders = extensions.extensions(ZoomContextFinder.class);
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

    @Override
    public String mimeType(Version version) {
        return MIME_TYPE;
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input, Version version, ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {
        // see if we can use the SLD cache, some conversions are expensive.
        if (input instanceof File) {
            // convert to resource, to avoid code duplication (the code for file would be very
            // similar to the resource one, but unfortunately using an unrelated set of classes
            File cssFile = (File) input;
            input = new FileSystemResourceStore(cssFile.getParentFile()).get(cssFile.getName());
        }

        if (input instanceof Resource) {
            Resource cssResource = (Resource) input;
            Resource sldResource = cssResource.parent().get(FilenameUtils.getBaseName(cssResource.name()) + ".sld");
            if (sldResource.getType() != Resource.Type.UNDEFINED
                    && sldResource.lastmodified() > cssResource.lastmodified()) {
                return sldHandler.parse(sldResource, SLDHandler.VERSION_10, resourceLocator, entityResolver);
            } else {
                // otherwise convert and write the cache
                try (Reader reader = toReader(input)) {
                    StyledLayerDescriptor sld = convertToSLD(reader);
                    try (OutputStream fos = sldResource.out()) {
                        sldHandler.encode(sld, SLDHandler.VERSION_10, true, fos);
                    }
                    // be consistent, have the SLD always be generated from and SLD parse,
                    // different code paths could result in different defaults/results due
                    // to inconsistencies/bugs happening over time
                    return sldHandler.parse(sldResource, SLDHandler.VERSION_10, resourceLocator, entityResolver);
                }
            }
        }

        // in this case, just do a plain on the fly conversion
        try (Reader unusedReader = toReader(input)) {
            return convertToSLD(toReader(input));
        }
    }

    StyledLayerDescriptor convertToSLD(String css) throws IOException {
        return convertToSLD(new StringReader(css));
    }

    private StyledLayerDescriptor convertToSLD(Reader cssReader) throws IOException {
        Stylesheet styleSheet = CssParser.parse(IOUtils.toString(cssReader));
        CssTranslator translator = new CssTranslator();
        translator.setZoomContextFinders(zoomContextFinders);
        StyledLayerDescriptor sld = translator.translateMultilayer(styleSheet);
        return sld;
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        try (Reader unusedReader = toReader(input)) {
            // full parse to perform the validation
            convertToSLD(toReader(input));
            return Collections.emptyList();
        } catch (Exception e) {
            return List.of(e);
        }
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "text/geocss";
    }

    @Override
    public URL getSpecification(Version version) throws MalformedURLException {
        return new URL("https://docs.geoserver.org/latest/en/user/styling/css/index.html");
    }

    @Override
    public String getFileExtension() {
        return "css";
    }

    @Override
    public String getModule() {
        return "gs-css";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.of("GeoServer CSS Styling");
    }

    @Override
    public Optional<String> getVersion() {
        Version v = GeoTools.getVersion(CssParser.class);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(v.toString());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.of("CSS Styling");
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.of("https://docs.geoserver.org/latest/en/user/styling/css/index.html");
    }
}
