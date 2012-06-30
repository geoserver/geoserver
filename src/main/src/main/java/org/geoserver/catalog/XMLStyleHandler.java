package org.geoserver.catalog;

import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Subclass of StyleHandler for XML based handlers.
 * <p>
 * This class exposes an {@link org.xml.sax.EntityResolver} for parsing operations.
 * </p>
 */
public abstract class XMLStyleHandler extends StyleHandler {

    protected XMLStyleHandler(String format, Version version) {
        super(format, version);
    }

    @Override
    public final StyledLayerDescriptor parse(Object input, ResourceLocator resourceLocator) throws IOException {
        return parse(input, resourceLocator, null);
    }

    public abstract StyledLayerDescriptor parse(Object input, ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException;

    @Override
    public final List<Exception> validate(Object input) throws IOException {
        return validate(input, null);
    }

    public abstract List<Exception> validate(Object input, EntityResolver entityResolver) throws IOException;
}
