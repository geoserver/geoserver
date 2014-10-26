/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoscript.geocss.compat.CSS2SLD;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

/**
 * Style handler for geocss. Justin Deoliveira, Boundless
 */
public class CssHandler extends StyleHandler {

    public static final String FORMAT = "css";

    public static final String MIME_TYPE = "application/vnd.geoserver.geocss+css";

    protected CssHandler() {
        super("CSS", FORMAT);
    }

    @Override
    public String mimeType(Version version) {
        return MIME_TYPE;
    }

    @Override
    public StyledLayerDescriptor parse(Object input, Version version,
            ResourceLocator resourceLocator, EntityResolver entityResolver) throws IOException {
        Reader reader = null;
        try {
            reader = toReader(input);
            Style style = CSS2SLD.convert(reader);
            return Styles.sld(style);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty,
            OutputStream output) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        Reader reader = null;
        try {
            reader = toReader(input);
            Style style = CSS2SLD.convert(reader);
            Styles.sld(style);
            return Collections.emptyList();
        } catch (Exception e) {
            return Arrays.asList(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
