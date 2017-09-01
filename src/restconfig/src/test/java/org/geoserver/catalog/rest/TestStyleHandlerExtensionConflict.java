package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geoserver.catalog.StyleHandler;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

/**
 * An implementation of {@link StyleHandler} whose format and extension collide with the extension-based content negotiation, used only in order to
 * verify that the conflicting StyleHandler does *not* take precedence for requests with that extension.
 * 
 * For example, file extension for this class is {@link StyleHandler} "xml", and the point is to verify that it does not take precedence for unrelated
 * style requests that end in ".xml" (like a request for the style info in xml format).
 */
public class TestStyleHandlerExtensionConflict extends StyleHandler {

    protected TestStyleHandlerExtensionConflict() {
        super("testStyleHandler", "xmlFormat");
    }

    @Override
    public String getFormat() {
        return super.getFormat();
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    @Override
    public String mimeType(Version version) {
        return "test.mime.type+xml";
    }

    @Override
    public StyledLayerDescriptor parse(Object input, Version version,
            ResourceLocator resourceLocator, EntityResolver entityResolver) throws IOException {
        return null;
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty,
            OutputStream output) throws IOException {

    }

    @Override
    public List<Exception> validate(Object input, Version version,
            EntityResolver entityResolver) throws IOException {
        return null;
    }
    
}