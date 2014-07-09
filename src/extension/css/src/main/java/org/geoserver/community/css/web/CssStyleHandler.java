/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import org.geoscript.geocss.compat.CSS2SLD;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Style handler for geocss.
 *
 * Justin Deoliveira, Boundless
 */
public class CssStyleHandler extends StyleHandler {

    public static final String FORMAT = "css";

    protected CssStyleHandler() {
        super(FORMAT, new Version("1.0.0"));
    }

    @Override
    public StyledLayerDescriptor parse(Object input, ResourceLocator resourceLocator) throws IOException {
        Style style = CSS2SLD.convert(toReader(input));
        return Styles.sld(style);
    }

    @Override
    public void encode(StyledLayerDescriptor sld, boolean pretty, OutputStream output) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input) throws IOException {
        try {
            CSS2SLD.convert(toReader(input));
            return null;
        }
        catch(Exception e) {
            return Arrays.asList(e);
        }
    }
}
