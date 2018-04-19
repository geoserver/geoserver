/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.geoserver.platform.resource.Resource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

/**
 * Extension point for handling a style of a particular language/version.
 * <p>
 * </p>
 * @author Justin Deoliveira, Boundless
 *
 */
public abstract class StyleHandler {

    protected static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        
    protected static String[] IMAGE_EXTENSIONS = new String[] {"png", "jpg", "jpeg", "gif", "svg"};

    protected static MimetypesFileTypeMap IMAGE_TYPES = new MimetypesFileTypeMap();    
    static {
        IMAGE_TYPES.addMimeTypes("image/png png");
        IMAGE_TYPES.addMimeTypes("image/jpg jpg jpeg");
        IMAGE_TYPES.addMimeTypes("image/gif gif");
        IMAGE_TYPES.addMimeTypes("image/svg+xml svg");
    }

    String name;
    String format;

    protected StyleHandler(String name, String format) {
        this.name = name;
        this.format = format;
    }

    /**
     * Human readable name of the handler.
     */
    public String getName() {
        return name;
    }

    /**
     * Format identifier for the handler.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Supported format versions.
     */
    public List<Version> getVersions() {
        return Arrays.asList(new Version("1.0.0"));
    }

    /**
     * Returns the file extension for the format.
     * <p>
     * Defaults to {@link #getFormat()}.
     * </p>
     */
    public String getFileExtension() {
        return getFormat();
    }

    /**
     * Returns the identifier for the mode used for syntax highlighting
     * in the code mirror editor.
     * <p>
     * Defaults to {@link #getFormat()}
     * </p>
     */
    public String getCodeMirrorEditMode() {
        return getFormat();
    }
    
    /**
     * Generates a style from a template using the provided substitutions.
     *
     * @param type the template type, see {@link org.geoserver.catalog.StyleType}.
     * @param color java.aw.Color to use during substitution
     * @param colorName Human readable color name, for use generating comments
     * @param layerName Layer name, for use generating comments
     * 
     * @return The text content of the style template after performing substitutions
     */
    public String getStyle(StyleType type, Color color, String colorName, String layerName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses a style resource.
     *
     * @param input The style input, see {@link #toReader(Object)} for accepted inputs.
     * @param version Optional version of the format, maybe <code>null</code>
     * @param resourceLocator Optional locator for resources (icons, etc...) referenced by the style, may be
     *                        <code>null</code>.
     * @param entityResolver Optional entity resolver for XML based formats, may be <code>null</code>.
     *
     */
    public abstract StyledLayerDescriptor parse(Object input, Version version, ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException;

    /**
     * Encodes a style.
     * <p>
     * Handlers that don't support encoding should throw {@link java.lang.UnsupportedOperationException}.
     * </p>
     * @param sld The style to encode.
     * @param version The version of the format to use to encode the style, may be <code>null</code>.
     * @param pretty Flag controlling whether or not the style should be encoded in pretty form.
     * @param output The stream to write the encoded style to.
     */
    public abstract void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
        throws IOException;

    /**
     * Validates a style resource.
     * <p>
     * For handlers that don't support an extended form of validation (like against an XML schema) this implementation
     * should at a minimum attempt to parse the input and return any parsing errors.
     * </p>
     * @param input The style input, see {@link #toReader(Object)} for accepted inputs.
     * @param version The version of the format to use to validate the style, may be <code>null</code>.
     *
     * @return Any validation errors, or empty list if the style is valid.
     */
    public abstract List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException;

    /**
     * Returns the format mime type for the specified version.
     *
     */
    public abstract String mimeType(Version version);

    /**
     * Returns the format version for the specified mime type.
     * <p>
     *  This method should only be overriden by formats that support multiple versions. The default
     *  implementation just returns 1.0.0.
     * </p>
     */
    public Version versionForMimeType(String mimeType) {
        return new Version("1.0.0");
    }

    /**
     * Determines the version of the format/language of the specified style resource.
     * <p>
     *  This method should only be overriden by formats that support multiple versions. The default
     *  implementation just returns 1.0.0.
     * </p>
     * @param input The style input, see {@link #toReader(Object)} for accepted inputs.
     */
    public Version version(Object input) throws IOException {
        return new Version("1.0.0");
    }

    /**
     * Turns input into a Reader.
     *
     * @param input A {@link Reader}, {@link java.io.InputStream}, {@link File}, or {@link Resource}.
     */
    protected Reader toReader(Object input) throws IOException {
        if (input instanceof Reader) {
            return (Reader) input;
        }
        
        if (input instanceof InputStream) {
            return new InputStreamReader((InputStream)input);
        }

        if (input instanceof String) {
            return new StringReader((String)input);
        }
        
        if (input instanceof URL) {
            return new InputStreamReader(((URL) input).openStream());
        }

        if (input instanceof File) {
            return new FileReader((File)input);
        }

        if (input instanceof Resource) {
            return toReader(((Resource)input).in());
        }

        throw new IllegalArgumentException("Unable to turn " + input + " into reader");
    }
    
    public String[] imageExtensions() {
        return IMAGE_EXTENSIONS;
    }
    
    public String insertImageCode(String imageFileName) {
        return imageFileName;
    }
}
