package org.geoserver.ysld;

import org.geoserver.catalog.StyleHandler;
import org.geotools.data.DataUtilities;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.ysld.Ysld;
import org.geotools.ysld.parse.WellKnownZoomContextFinder;
import org.xml.sax.EntityResolver;
import org.geotools.ysld.parse.ZoomContextFinder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public class YsldHandler extends StyleHandler {

    public static final String FORMAT = "ysld";
    public static final String MIMETYPE = "application/vnd.geoserver.ysld+yaml";

    /**
     * Creates a new handler with an explicit zoom finder.
     */
    public YsldHandler(ZoomContextFinder zoomFinder) {
        super("Ysld", FORMAT);
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
        this(WellKnownZoomContextFinder.getInstance());
    }

    @Override
    public String getFileExtension() {
        return "yaml";
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "yaml";
    }
    
    ZoomContextFinder zoomFinder;
    
    @Override
    public StyledLayerDescriptor parse(Object input, Version version, @Nullable ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException {
        
        if (resourceLocator == null && input instanceof File) {
            resourceLocator = new DefaultResourceLocator();
            ((DefaultResourceLocator)resourceLocator).setSourceUrl(DataUtilities.fileToURL((File) input));
        }
        
        return Ysld.parse(toReader(input), Collections.singletonList(zoomFinder), resourceLocator);
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output) throws IOException {
        Ysld.encode(sld, output);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        return (List) Ysld.validate(toReader(input), Collections.singletonList(zoomFinder));
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }
}
