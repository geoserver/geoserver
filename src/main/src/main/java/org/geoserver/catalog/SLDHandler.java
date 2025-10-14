/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.IOUtils;
import org.geoserver.ows.util.RequestUtils;
import org.geotools.api.style.ResourceLocator;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.brewer.styling.builder.StyledLayerDescriptorBuilder;
import org.geotools.sld.v1_1.SLD;
import org.geotools.sld.v1_1.SLDConfiguration;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.util.URLs;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDParser;
import org.geotools.xml.styling.SLDTransformer;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.vfny.geoserver.util.SLDValidator;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * SLD style handler.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SLDHandler extends StyleHandler {

    static Logger LOGGER = Logging.getLogger(SLDHandler.class);

    /**
     * number of bytes to "look ahead" when pre parsing xml document. TODO: make this configurable, and possibley link
     * it to the same value used by the ows dispatcher.
     */
    static int XML_LOOKAHEAD = 8500;

    public static final String FORMAT = "sld";

    public static final Version VERSION_10 = new Version("1.0.0");
    public static final Version VERSION_11 = new Version("1.1.0");

    public static final String MIMETYPE_10 = "application/vnd.ogc.sld+xml";
    public static final String MIMETYPE_11 = "application/vnd.ogc.se+xml";

    static final Map<StyleType, String> TEMPLATES = new HashMap<>();

    static {
        try {
            TEMPLATES.put(
                    StyleType.POINT,
                    IOUtils.toString(SLDHandler.class.getResourceAsStream("template_point.sld"), UTF_8));
            TEMPLATES.put(
                    StyleType.POLYGON,
                    IOUtils.toString(SLDHandler.class.getResourceAsStream("template_polygon.sld"), UTF_8));
            TEMPLATES.put(
                    StyleType.LINE, IOUtils.toString(SLDHandler.class.getResourceAsStream("template_line.sld"), UTF_8));
            TEMPLATES.put(
                    StyleType.RASTER,
                    IOUtils.toString(SLDHandler.class.getResourceAsStream("template_raster.sld"), UTF_8));
            TEMPLATES.put(
                    StyleType.GENERIC,
                    IOUtils.toString(SLDHandler.class.getResourceAsStream("template_generic.sld"), UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the style templates", e);
        }
    }

    public SLDHandler() {
        super("SLD", FORMAT);
    }

    @Override
    public List<Version> getVersions() {
        return Arrays.asList(VERSION_10, VERSION_11);
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "text/sld10";
    }

    @Override
    public boolean supportsEncoding(Version version) {
        return version == null || VERSION_10.equals(version);
    }

    @Override
    public String getStyle(StyleType type, Color color, String colorName, String layerName) {
        String template = TEMPLATES.get(type);
        String colorCode = Integer.toHexString(color.getRGB());
        colorCode = colorCode.substring(2, colorCode.length());
        return template.replace("${colorName}", colorName)
                .replace("${colorCode}", "#" + colorCode)
                .replace("${layerName}", layerName);
    }

    @Override
    public String mimeType(Version version) {
        if (version != null && version.equals(VERSION_11)) {
            return MIMETYPE_11;
        }
        return MIMETYPE_10;
    }

    @Override
    public Version versionForMimeType(String mimeType) {
        if (mimeType.equals(MIMETYPE_11)) {
            return VERSION_11;
        }
        return VERSION_10;
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input, Version version, ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {
        if (version == null) {
            Object[] versionAndReader = getVersionAndReader(input, true);
            version = (Version) versionAndReader[0];
            input = versionAndReader[1];
        }

        if (VERSION_11.compareTo(version) == 0) {
            return parse11(input, resourceLocator, entityResolver);
        } else {
            return parse10(input, resourceLocator, entityResolver);
        }
    }

    @SuppressWarnings({"PMD.CloseResource", "PMD.UseTryWithResources"})
    StyledLayerDescriptor parse10(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {

        // reader is conditionally initialized, actually gets closed
        Reader reader = null;
        try {
            // we need to close the reader if we grab one, but if it's a file it has
            // to stay as such to allow relative resource resolution during the parse
            if (!(input instanceof File)) {
                reader = toReader(input);
                input = reader;
            }
            SLDParser p = createSld10Parser(input, resourceLocator, entityResolver);
            StyledLayerDescriptor sld = p.parseSLD();
            if (sld.getStyledLayers().length == 0) {
                // most likely a style that is not a valid sld, try to actually parse out a
                // style and then wrap it in an sld
                Style[] style = p.readDOM();
                if (style.length > 0) {
                    StyledLayerDescriptorBuilder sldBuilder = new StyledLayerDescriptorBuilder().reset(sld);
                    sldBuilder.namedLayer().style().reset(style[0]);
                    sld = sldBuilder.build();
                }
            }
            return sld;
        } finally {
            org.geoserver.util.IOUtils.closeQuietly(reader);
        }
    }

    StyledLayerDescriptor parse11(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {
        Parser parser = createSld11Parser(input, resourceLocator, entityResolver);
        try (Reader reader = toReader(input)) {
            parser.setEntityResolver(entityResolver);
            return (StyledLayerDescriptor) parser.parse(reader);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    SLDParser createSld10Parser(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver)
            throws IOException {
        SLDParser parser;
        if (input instanceof File file) {
            parser = new SLDParser(styleFactory, file);
        } else {
            parser = new SLDParser(styleFactory, toReader(input));
        }

        if (resourceLocator != null) {
            parser.setOnLineResourceLocator(resourceLocator);
        }
        if (entityResolver != null) {
            parser.setEntityResolver(entityResolver);
        }
        return parser;
    }

    Parser createSld11Parser(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver) {
        if (resourceLocator == null && input instanceof File file) {
            // setup for resolution of relative paths
            final java.net.URL surl = URLs.fileToUrl(file);
            DefaultResourceLocator defResourceLocator = new DefaultResourceLocator();
            defResourceLocator.setSourceUrl(surl);
            resourceLocator = defResourceLocator;
        }

        final ResourceLocator locator = resourceLocator;
        SLDConfiguration sld;
        if (locator != null) {
            sld = new SLDConfiguration() {
                @Override
                protected void configureContext(org.picocontainer.MutablePicoContainer container) {
                    container.registerComponentInstance(ResourceLocator.class, locator);
                }
            };
        } else {
            sld = new SLDConfiguration();
        }

        Parser parser = new Parser(sld);
        if (entityResolver != null) {
            parser.setEntityResolver(entityResolver);
        }
        return parser;
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        if (version != null && VERSION_11.compareTo(version) == 0) {
            encode11(sld, pretty, output);
        } else {
            encode10(sld, pretty, output);
        }
    }

    void encode10(StyledLayerDescriptor sld, boolean pretty, OutputStream output) throws IOException {
        SLDTransformer tx = new SLDTransformer();
        if (pretty) {
            tx.setIndentation(2);
        }
        try {
            tx.transform(sld, output);
        } catch (TransformerException e) {
            throw (IOException) new IOException("Error writing style").initCause(e);
        }
    }

    void encode11(StyledLayerDescriptor sld, boolean pretty, OutputStream output) throws IOException {
        Encoder e = new Encoder(new SLDConfiguration());
        e.setIndenting(pretty);
        e.encode(sld, SLD.StyledLayerDescriptor, output);
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver) throws IOException {
        if (version == null) {
            Object[] versionAndReader = getVersionAndReader(input, true);
            version = (Version) versionAndReader[0];
            input = versionAndReader[1];
        }

        if (version != null && VERSION_11.compareTo(version) == 0) {
            return validate11(input, entityResolver);
        } else {
            return validate10(input, entityResolver);
        }
    }

    List<Exception> validate10(Object input, EntityResolver entityResolver) throws IOException {
        try (Reader reader = toReader(input)) {
            final SLDValidator validator = new SLDValidator();
            validator.setEntityResolver(entityResolver);
            return new ArrayList<>(validator.validateSLD(new InputSource(reader)));
        }
    }

    List<Exception> validate11(Object input, EntityResolver entityResolver) throws IOException {
        Parser p = createSld11Parser(input, null, entityResolver);
        try (Reader reader = toReader(input)) {
            p.validate(reader);
            return p.getValidationErrors();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Version version(Object input) throws IOException {
        Object[] versionAndReader = getVersionAndReader(input, false);
        return (Version) versionAndReader[0];
    }

    /** Helper method for finding which style handler/version to use from the actual content. */
    @SuppressWarnings("PMD.CloseResource") // reader returned as part of the response, instanceof InputStream stream
    Object[] getVersionAndReader(Object input, boolean needReader) throws IOException {
        // need to determine version of sld from actual content
        BufferedReader reader = null;

        if (input instanceof InputStream stream) {
            reader = RequestUtils.getBufferedXMLReader(stream, XML_LOOKAHEAD);
        } else {
            reader = RequestUtils.getBufferedXMLReader(toReader(input), XML_LOOKAHEAD);
        }

        String version = null;
        XMLStreamReader parser;
        try {
            // create stream parser
            XMLInputFactory factory = XMLInputFactory.newFactory();
            // disable DTDs
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            // disable external entities
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            parser = factory.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new IOException("Error creating xml parser", e);
        }
        try {
            // position at root element
            while (parser.hasNext()) {
                if (START_ELEMENT == parser.next()) {
                    break;
                }
            }

            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if ("version".equals(parser.getAttributeLocalName(i))) {
                    version = parser.getAttributeValue(i);
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException("Error parsing content", e);
        } finally {
            // release parser resources, does not close input stream
            try {
                parser.close();
            } catch (XMLStreamException e) {
                LOGGER.log(Level.WARNING, "Non fatal error closing XML Stream Parser", e);
            }
        }

        // reset input stream
        reader.reset();

        if (version == null) {
            LOGGER.warning("Could not determine SLD version from content. Assuming 1.0.0");
            version = "1.0.0";
        }

        Object[] result;
        if (needReader) {
            result = new Object[] {new Version(version), reader};
        } else {
            reader.close();
            result = new Object[] {new Version(version)};
        }

        return result;
    }

    @Override
    public String insertImageCode(String imageFileName, String styleContent) {
        boolean version11 = false; // by default, we'll assume version 1.0;
        if (styleContent != null) {
            try {
                version11 = VERSION_11.compareTo(version(styleContent)) == 0;
            } catch (IOException e) {
            }
        }
        return new StringBuffer("<ExternalGraphic ")
                .append(version11 ? "xmlns=\"http://www.opengis.net/se\" " : "xmlns=\"http://www.opengis.net/sld\" ")
                .append("xmlns:xlink=\"http://www.w3.org/1999/xlink\">\\n")
                .append("<OnlineResource xlink:type=\"simple\" xlink:href=\"")
                .append(imageFileName)
                .append("\" />\\n")
                .append("<Format>")
                .append(IMAGE_TYPES.getContentType(imageFileName))
                .append("</Format>\\n")
                .append("</ExternalGraphic>\\n")
                .toString();
    }

    @Override
    public URL getSpecification(Version version) throws MalformedURLException {
        if (version != null && VERSION_11.compareTo(version) == 0) {
            return new URL("http://portal.opengeospatial.org/files/?artifact_id=22364");
        } else {
            return new URL("http://portal.opengeospatial.org/files/?artifact_id=1188");
        }
    }
}
