package org.geoserver.ysld;

import org.geoserver.catalog.StyleHandler;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.ysld.Ysld;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class YsldHandler extends StyleHandler {

    public static final String FORMAT = "ysld";
    public static final String MIMETYPE = "application/vnd.geoserver.ysld+yaml";

    public YsldHandler() {
        super("Ysld", FORMAT);
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
    public StyledLayerDescriptor parse(Object input, Version version, ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException {
        return Ysld.parse(toReader(input));
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output) throws IOException {
        Ysld.encode(sld, output);
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        return (List) Ysld.validate(toReader(input));
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }
}
