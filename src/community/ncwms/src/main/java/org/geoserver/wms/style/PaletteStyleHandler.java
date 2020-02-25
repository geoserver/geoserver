/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.style;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.geoserver.catalog.StyleHandler;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDTransformer;
import org.xml.sax.EntityResolver;

/**
 * Handler for the dynamic palette style language. See {@link PaletteParser} for details on the
 * grammar
 */
public class PaletteStyleHandler extends StyleHandler {
    static final Logger LOGGER = Logging.getLogger(PaletteStyleHandler.class);

    public static final String MIME_TYPE = "text/vnd.ncwms.palette";

    public static final String FORMAT = "PAL";

    protected PaletteStyleHandler() {
        super("Dynamic palette", FORMAT);
    }

    @Override
    public String getFileExtension() {
        return ".pal";
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input,
            Version version,
            ResourceLocator resourceLocator,
            EntityResolver entityResolver)
            throws IOException {
        try (Reader reader = toReader(input)) {
            StyledLayerDescriptor sld = new PaletteParser().parseStyle(reader);
            if (LOGGER.isLoggable(Level.FINE)) {
                try {
                    LOGGER.fine("Palette has been parsed to " + toSLD(sld));
                } catch (TransformerException e) {
                    LOGGER.log(Level.FINE, "Failed to transform in memory style to SLD", e);
                }
            }

            return sld;
        }
    }

    String toSLD(StyledLayerDescriptor sld) throws TransformerException {
        final SLDTransformer tx = new SLDTransformer();
        tx.setIndentation(2);
        return tx.transform(sld);
    }

    @Override
    public void encode(
            StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        // just check the palette is valid, no need to convert to Style
        try (BufferedReader reader = new BufferedReader(toReader(input))) {
            new PaletteParser().parseColorMap(reader);
        } catch (Exception e) {
            return Arrays.asList(e);
        }

        return Collections.emptyList();
    }

    @Override
    public String mimeType(Version version) {
        return MIME_TYPE;
    }
}
