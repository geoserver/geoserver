/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.css.CssParser;
import org.geotools.styling.css.CssTranslator;
import org.geotools.styling.css.Stylesheet;
import org.geotools.util.Version;
import org.xml.sax.EntityResolver;

/**
 * Style handler for geocss. Justin Deoliveira, Boundless
 */
public class CssHandler extends StyleHandler {

    public static final String FORMAT = "css";

    public static final String MIME_TYPE = "application/vnd.geoserver.geocss+css";

    private SLDHandler sldHandler;

    protected CssHandler(SLDHandler sldHandler) {
        super("CSS", FORMAT);
        this.sldHandler = sldHandler;
    }

    @Override
    public String mimeType(Version version) {
        return MIME_TYPE;
    }

    @Override
    public StyledLayerDescriptor parse(Object input, Version version,
            ResourceLocator resourceLocator, EntityResolver entityResolver) throws IOException {
        // see if we can use the SLD cache, some conversions are expensive.
        if (input instanceof File) {
            // convert to resource, to avoid code duplication (the code for file would be very
            // similar to the resource one, but unfortunately using an unrelated set of classes
            File cssFile = (File) input;
            input = new FileSystemResourceStore(cssFile.getParentFile()).get(cssFile.getName());
        }

        if (input instanceof Resource) {
            Resource cssResource = (Resource) input;
            Resource sldResource = cssResource.parent().get(
                    FilenameUtils.getBaseName(cssResource.name()) + ".sld");
            if (sldResource.getType() != Resource.Type.UNDEFINED
                    && sldResource.lastmodified() > cssResource.lastmodified()) {
                return sldHandler.parse(sldResource, SLDHandler.VERSION_10, resourceLocator,
                        entityResolver);
            } else {
                // otherwise convert and write the cache
                StyledLayerDescriptor sld = convertToSLD(toReader(input));
                try (OutputStream fos = sldResource.out()) {
                    sldHandler.encode(sld, SLDHandler.VERSION_10, true, fos);
                }
                // be consistent, have the SLD always be generated from and SLD parse,
                // different code paths could result in different defaults/results due
                // to inconsistencies/bugs happening over time
                return sldHandler.parse(sldResource, SLDHandler.VERSION_10, resourceLocator,
                        entityResolver);
            }

        }

        // in this case, just do a plain on the fly conversion
        try (Reader reader = toReader(input)) {
            StyledLayerDescriptor sld = convertToSLD(toReader(input));
            return sld;
        }
    }

    StyledLayerDescriptor convertToSLD(String css) throws IOException {
        return convertToSLD(new StringReader(css));
    }

    private StyledLayerDescriptor convertToSLD(Reader cssReader) throws IOException {
        Stylesheet styleSheet = CssParser.parse(IOUtils.toString(cssReader));
        Style style = (Style) new CssTranslator().translate(styleSheet);
        StyledLayerDescriptor sld = Styles.sld(style);
        return sld;
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty,
            OutputStream output) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        try (Reader reader = toReader(input)) {
            // full parse to perform the validation
            convertToSLD(toReader(input));
            return Collections.emptyList();
        } catch (Exception e) {
            return Arrays.asList(e);
        }
    }
}
