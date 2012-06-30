/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.xml.transform.TransformerException;

import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.DataUtilities;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.sld.v1_1.SLDConfiguration;
import org.geotools.styling.*;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Parser;
import org.vfny.geoserver.util.SLDValidator;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Provides methods to parse/encode style documents. 
 * <p>
 * Currently SLD versions 1.0, and 1.1 are supported.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
public class Styles {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wms");
    
    /**
     * number of bytes to "look ahead" when pre parsing xml document.
     * TODO: make this configurable, and possibley link it to the same value 
     * used by the ows dispatcher.
     */
    static int XML_LOOKAHEAD = 8192;
    
    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    /**
     * Parses a style document specifying the format.
     *
     * @param input Style input, see {@link StyleHandler#toReader(Object)}.
     * @param format Style format, may be null.
     *
     */
    public static StyledLayerDescriptor parse(Object input, String format) throws IOException {
        return parse(input, format, null, null, null);
    }

    /**
     * Parses a style document into a StyledLayerDescriptor object explicitly specifying version.
     * <p>
     * </p>
     * @param input Style input, see {@link StyleHandler#toReader(Object)}.
     * @param format Style format, may be null.
     * @param version The style format version
     *
     * @return The parsed StyleLayerDescriptor.
     *
     * @throws IOException Any parsing errors that occur.
     * @throws IllegalArgumentException If the specified version is not supported.
     */
    public static StyledLayerDescriptor parse(Object input, String format, Version version, ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException {
        return doParse(lookupHandler(format, version), input, resourceLocator, entityResolver);
    }

    /**
     * Parses a style document into a StyleLayerDescriptor determining style type/version
     * from the content itself.
     *
     * @param input Style input, see {@link StyleHandler#toReader(Object)}.
     * @param format The style language
     *
     * @return The parsed style.
     *
     * @throws IOException Any parsing errors that occur.
     * @throws IllegalArgumentException If the type of the style can not be determined.
     */
    public static StyledLayerDescriptor parse(Object input,  String format, ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException {

        return doParse(lookupHandler(format, null), input, resourceLocator, entityResolver);
    }

    /**
     * Encodes a StyledLayerDescriptor object to a style document.
     * <p>
     * </p>
     * @param sld The StyledLayerDescriptor object
     * @param version The SLD version
     * @param format Specifies if the serialized SLD should be formatted or not.
     * @param output The output stream to serialize to.
     * 
     * @throws IOException Any encoding errors that occur.
     * @throws IllegalArgumentException If the specified version is not supported.
     */
    public static void encode(StyledLayerDescriptor sld, String format, Version version, boolean pretty, 
            OutputStream output) throws IOException {

        lookupHandler(format, version).encode(sld, pretty, output);
    }

    /**
     * Performs schema validation on an style document determining style type from the content
     * itself.
     *
     * @param input Style input, see {@link StyleHandler#toReader(Object)}.
     * @para format Style language
     * @return A list of validation exceptions, empty if no errors are present and the document is
     *   valid.
     *
     * @throws IOException Any parsing errors that occur.
     * @throws IllegalArgumentException If the specified version is not supported.
     */
    public static List<Exception> validate(Object input, String format) throws IOException {
        return validate(input, format, null);
    }

    /**
     * Performs schema validation on an style document determining style type from the content
     * itself. 
     * 
     * @param input Style input, see {@link StyleHandler#toReader(Object)}.
     * @para format Style language
     * @return A list of validation exceptions, empty if no errors are present and the document is
     *   valid.
     * 
     * @throws IOException Any parsing errors that occur.
     * @throws IllegalArgumentException If the specified version is not supported.
     */
    public static List<Exception> validate(Object input, String format, EntityResolver entityResolver)
            throws IOException {
        Object[] obj = getVersionAndReader(input);
        return validate(input, format, null, entityResolver);
    }

    /**
     * Performs schema validation on an style document, specifying the version.
     * 
     * @param input Style input, see {@link StyleHandler#toReader(Object)}.
     * @param format The style format
     * @param version The style version
     * 
     * @return A list of validation exceptions, empty if no errors are present and the document is
     *   valid.
     * 
     * @throws IOException Any parsing errors that occur.
     * @throws IllegalArgumentException If the specified version is not supported.
     */
    public static List<Exception> validate(Object input, String format, Version version, EntityResolver entityResolver) throws IOException {
        return doValidate(lookupHandler(format, version), input, entityResolver);
    }

    /**
     * Convenience method to pull a UserSyle from a StyledLayerDescriptor.
     * <p>
     * This method will return the first UserStyle it encounters in the StyledLayerDescriptor tree.
     * </p>
     * @param sld The StyledLayerDescriptor object.
     * 
     * @return The UserStyle, or <code>null</code> if no such style could be found.
     */
    public static Style style(StyledLayerDescriptor sld) {
        for (int i = 0; i < sld.getStyledLayers().length; i++) {
            Style[] styles = null;
            
            if (sld.getStyledLayers()[i] instanceof NamedLayer) {
                NamedLayer layer = (NamedLayer) sld.getStyledLayers()[i];
                styles = layer.getStyles();
            }
            else if(sld.getStyledLayers()[i] instanceof UserLayer) {
                UserLayer layer = (UserLayer) sld.getStyledLayers()[i];
                styles = layer.getUserStyles();
            }
            
            if (styles != null) {
                for (int j = 0; j < styles.length; i++) {
                    if (!(styles[j] instanceof NamedStyle)) {
                        return styles[j];
                    }
                }
            }
            
        }

        return null;
    }

    /**
     * Convenience method to wrap a UserStyle in a StyledLayerDescriptor object.
     * <p>
     * This method wraps the UserStyle in a NamedLayer, and wraps the result in a StyledLayerDescriptor.
     * </p>
     * @param style The UserStyle.
     * 
     * @return The StyledLayerDescriptor.
     */
    public static StyledLayerDescriptor sld(Style style) {
        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();
        
        NamedLayer layer = styleFactory.createNamedLayer();
        layer.setName(style.getName());
        sld.addStyledLayer(layer);
        
        layer.addStyle(style);
        
        return sld;
    }

    public static Version findVersion(Object input) throws IOException{
        Object[] versionAndReader = getVersionAndReader(input);
        return (Version) versionAndReader[0];
    }

    public static StyleHandler lookupHandler(String format, Version version) {
        if (format == null) {
            throw new IllegalArgumentException("Style format must not be null");
        }

        List<StyleHandler> matches = new ArrayList();
        for (StyleHandler h : GeoServerExtensions.extensions(StyleHandler.class)) {
            if (format.equalsIgnoreCase(h.getFormat())) {
                if (version != null && version.equals(h.getVersion())) {
                    //exact match
                    return h;
                }
                matches.add(h);
            }
        }

        if (matches.isEmpty()) {
            throw new RuntimeException(
                "No such style handler: format = " + format + ", version = " + version);
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        //sort, and return lowest version
        //TODO: actually do version negotiation, match the closest version and only return lowest
        // when version is actually null
        Collections.sort(matches, new Comparator<StyleHandler>() {
            @Override
            public int compare(StyleHandler o1, StyleHandler o2) {
                return o1.getVersion().compareTo(o2.getVersion());
            }
        });
        return matches.get(0);
    }

    /**
     * Helper method for finding which style handler/version to use from the actual content.
     */
    static Object[] getVersionAndReader(Object input) throws IOException {
        //need to determine version of sld from actual content
        BufferedReader reader = null;
        
        if (input instanceof InputStream) {
            reader = RequestUtils.getBufferedXMLReader((InputStream) input, 8192);
        }
        else {
            reader = RequestUtils.getBufferedXMLReader(toReader(input), 8192);
        }
            
        if (!reader.ready()) {
            return null;
        }

        String version;
        try {
            //create stream parser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            //parse root element
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(reader);
            parser.nextTag();

            version = null;
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                if ("version".equals(parser.getAttributeName(i))) {
                    version = parser.getAttributeValue(i);
                }
            }

            parser.setInput(null);
        } 
        catch (XmlPullParserException e) {
            throw (IOException) new IOException("Error parsing content").initCause(e);
        }

        //reset input stream
        reader.reset();
        
        if (version == null) {
            LOGGER.warning("Could not determine SLD version from content. Assuming 1.0.0");
            version = "1.0.0";
        }
        
        return new Object[]{new Version(version), reader};
    }
    
    static Reader toReader(Object input) throws IOException {
        if (input instanceof Reader) {
            return (Reader) input;
        }
        
        if (input instanceof InputStream) {
            return new InputStreamReader((InputStream)input);
        }
        
        if (input instanceof File) {
            return new FileReader((File)input);
        }
        
        if (input instanceof Resource) {
            return new InputStreamReader(((Resource)input).in());
        }
        
        throw new IllegalArgumentException("Unable to turn " + input + " into reader");
    }

    static StyledLayerDescriptor doParse(StyleHandler handler, Object input, ResourceLocator resourceLocator,
        EntityResolver entityResolver) throws IOException {

        if (entityResolver != null) {
            return assertXMLStyleHandler(handler).parse(input, resourceLocator, entityResolver);
        } else {
            return handler.parse(input, resourceLocator);
        }
    }

    static List<Exception> doValidate(StyleHandler handler, Object input, EntityResolver entityResolver)
            throws IOException {

        if (entityResolver != null) {
            return assertXMLStyleHandler(handler).validate(input, entityResolver);
        } else {
            return handler.validate(input);
        }
    }

    static XMLStyleHandler assertXMLStyleHandler(StyleHandler handler) {
        if (!(handler instanceof XMLStyleHandler)) {
            if (!(handler instanceof XMLStyleHandler)) {
                //TODO: just log this?
                throw new IllegalStateException("Entity resolver specified for non xml handler");
            }
        }
        return ((XMLStyleHandler)handler);
    }

//    public static enum Handler {
//        SLD_10("1.0.0") {
//
//            @Override
//            public StyledLayerDescriptor parse(Object input, EntityResolver entityResolver) throws IOException {
//                SLDParser p = parser(input, entityResolver);
//                StyledLayerDescriptor sld = p.parseSLD();
//                if (sld.getStyledLayers().length == 0) {
//                    //most likely a style that is not a valid sld, try to actually parse out a
//                    // style and then wrap it in an sld
//                    Style[] style = p.readDOM();
//                    if (style.length > 0) {
//                        NamedLayer l = styleFactory.createNamedLayer();
//                        l.addStyle(style[0]);
//                        sld.addStyledLayer(l);
//                    }
//                }
//
//                return sld;
//            }
//
//            @Override
//            protected List<Exception> validate(Object input, EntityResolver entityResolver) throws IOException {
//                return new SLDValidator().validateSLD(new InputSource(toReader(input)));
//            }
//
//            @Override
//            public void encode(StyledLayerDescriptor sld, boolean format, OutputStream output) throws IOException {
//                SLDTransformer tx = new SLDTransformer();
//                if (format) {
//                    tx.setIndentation(2);
//                }
//                try {
//                    tx.transform( sld, output );
//                }
//                catch (TransformerException e) {
//                    throw (IOException) new IOException("Error writing style").initCause(e);
//                }
//            }
//
//
//            SLDParser parser(Object input, EntityResolver entityResolver) throws IOException {
//                SLDParser parser;
//                if (input instanceof File) {
//                    parser = new SLDParser(styleFactory, (File) input);
//                }
//                else {
//                    parser = new SLDParser(styleFactory, toReader(input));
//                }
//
//                parser.setEntityResolver(entityResolver);
//                return parser;
//            }
//        },
//
//        SLD_11("1.1.0") {
//
//            @Override
//            public StyledLayerDescriptor parse(Object input, EntityResolver entityResolver) throws IOException {
//                SLDConfiguration sld;
//                if (input instanceof File) {
//                    // setup for resolution of relative paths
//                    final java.net.URL surl = DataUtilities.fileToURL((File) input);
//                    sld = new SLDConfiguration() {
//                        protected void configureContext(
//                                org.picocontainer.MutablePicoContainer container) {
//                            DefaultResourceLocator locator = new DefaultResourceLocator();
//                            locator.setSourceUrl(surl);
//                            container.registerComponentInstance(ResourceLocator.class, locator);
//                        };
//                    };
//                } else {
//                    sld = new SLDConfiguration();
//                }
//
//                try {
//                    Parser parser = new Parser(sld);
//                    parser.setEntityResolver(entityResolver);
//                    return (StyledLayerDescriptor) parser.parse(toReader(input));
//                }
//                catch(Exception e) {
//                    if (e instanceof IOException) throw (IOException) e;
//                    throw (IOException) new IOException().initCause(e);
//                }
//            }
//
//            @Override
//            protected List<Exception> validate(Object input, EntityResolver entityResolver) throws IOException {
//                SLDConfiguration sld = new SLDConfiguration();
//                Parser p = new Parser(sld);
//                p.setValidating(true);
//                p.setEntityResolver(entityResolver);
//
//                try {
//                    p.parse(toReader(input));
//                    return p.getValidationErrors();
//                }
//                catch(Exception e) {
//                    e.printStackTrace();
//                    List validationErrors = new ArrayList<Exception>(p.getValidationErrors());
//                    validationErrors.add(0, e);
//                    return validationErrors;
//                }
//            }
//
//            @Override
//            public void encode(StyledLayerDescriptor sld, boolean format, OutputStream output) throws IOException {
//                // TODO Auto-generated method stub
//            }
//        };
//
//        private Version version;
//
//        private Handler(String version) {
//            this.version = new org.geotools.util.Version(version);
//        }
//
//        public Version getVersion() {
//            return version;
//        }
//
//        protected abstract StyledLayerDescriptor parse(Object input, EntityResolver entityResolver) throws IOException;
//
//        protected abstract void encode(StyledLayerDescriptor sld, boolean format, OutputStream output)
//            throws IOException;
//
//        protected abstract List<Exception> validate(Object input, EntityResolver entityResolver) throws IOException;
//
//        public static Handler lookup(Version version) {
//            for (Handler h : values()) {
//                if (h.getVersion().equals(version)) {
//                    return h;
//                }
//            }
//            throw new IllegalArgumentException("No support for SLD " + version);
//        }
//    };

}
