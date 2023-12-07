package org.geoserver.catalog;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.ModuleStatus;
import org.geotools.api.style.ResourceLocator;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

public class GraticuleHandler extends StyleHandler implements ModuleStatus {

    public static final String FORMAT = "grids";
    private SLDHandler sldHandler;
    public static final Logger log = Logger.getLogger("GraticuleHandler");

    protected GraticuleHandler(SLDHandler sldHandler) {
        super("grid", FORMAT);
        this.sldHandler = sldHandler;
        try {
            log.info("Adding to template?");
            sldHandler.TEMPLATES.put(
                    StyleType.GRID,
                    IOUtils.toString(
                            Objects.requireNonNull(
                                    GraticuleHandler.class.getResourceAsStream(
                                            "template_grid.sld")),
                            UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the css style templates", e);
        }
    }
    /**
     * Module identifier based on artifact bundle Example: <code>gs-main</code>, <code>gs-oracle
     * </code>
     */
    @Override
    public String getModule() {
        return "gs-graticule";
    }

    /** Optional component identifier within module. Example: <code>rendering-engine</code> */
    @Override
    public Optional<String> getComponent() {
        return Optional.of("GeoServer Graticule Extension");
    }

    /** Human readable version, ie. for geotools it would be 15-SNAPSHOT * */
    @Override
    public Optional<String> getVersion() {
        return Optional.empty();
    }

    /** Returns whether the module is available to GeoServer * */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /** Returns whether the module is enabled in the current GeoServer configuration. * */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Optional status message such as what Java rendering engine is in use, or the library path if
     * the module/driver is unavailable
     */
    @Override
    public Optional<String> getMessage() {
        return Optional.of("Add grids or graticules to WMS maps");
    }

    /** Optional relative link to GeoServer user manual */
    @Override
    public Optional<String> getDocumentation() {
        return Optional.empty();
    }

    /**
     * Parses a style resource.
     *
     * @param input The style input, see {@link #toReader(Object)} for accepted inputs.
     * @param version Optional version of the format, maybe <code>null</code>
     * @param resourceLocator Optional locator for resources (icons, etc...) referenced by the
     *     style, may be <code>null</code>.
     * @param entityResolver Optional entity resolver for XML based formats, may be <code>null
     *                        </code>.
     */
    @Override
    public StyledLayerDescriptor parse(
            Object input,
            Version version,
            ResourceLocator resourceLocator,
            EntityResolver entityResolver)
            throws IOException {
        return sldHandler.parse(input, version, resourceLocator, entityResolver);
    }

    /**
     * Encodes a style.
     *
     * <p>Handlers that don't support encoding should throw {@link UnsupportedOperationException}.
     *
     * @param sld The style to encode.
     * @param version The version of the format to use to encode the style, may be <code>null</code>
     *     .
     * @param pretty Flag controlling whether the style should be encoded in pretty form.
     * @param output The stream to write the encoded style to.
     */
    @Override
    public void encode(
            StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        sldHandler.encode(sld, version, pretty, output);
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "text/xml";
    }
    /**
     * Validates a style resource.
     *
     * <p>For handlers that don't support an extended form of validation (like against an XML
     * schema) this implementation should at a minimum attempt to parse the input and return any
     * parsing errors.
     *
     * @param input The style input, see {@link #toReader(Object)} for accepted inputs.
     * @param version The version of the format to use to validate the style, may be <code>null
     *                       </code>.
     * @param entityResolver
     * @return Any validation errors, or empty list if the style is valid.
     */
    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        return sldHandler.validate(input, version, entityResolver);
    }

    /**
     * Returns the format mime type for the specified version.
     *
     * @param version
     */
    @Override
    public String mimeType(Version version) {
        return sldHandler.mimeType(version);
    }
}
