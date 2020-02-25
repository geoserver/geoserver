/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.OutputStream;
import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.xml.sax.ContentHandler;

/**
 * Process parameter input / output for objects which are XML based.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public abstract class XMLPPIO extends ComplexPPIO {

    /** */
    protected QName element;

    protected volatile EntityResolverProvider resolverProvider;

    /** Constructor specifying 'text/xml' as mime type. */
    protected XMLPPIO(Class externalType, Class internalType, QName element) {
        this(externalType, internalType, "text/xml", element);
    }

    /** Constructor explicitly specifying mime type. */
    protected XMLPPIO(Class externalType, Class internalType, String mimeType, QName element) {
        super(externalType, internalType, mimeType);
        if (element == null) {
            throw new NullPointerException("element must not be null");
        }
        this.element = element;
    }

    /** The qualified name of the XML element in the XML representation of the object. */
    public QName getElement() {
        return element;
    }

    /**
     * Encodes the internal representation of the object to an XML stream.
     *
     * @param object An object of type {@link #getType()}.
     * @param handler An XML content handler.
     */
    public abstract void encode(Object object, ContentHandler handler) throws Exception;

    /** Encodes the internal object representation of a parameter as a string. */
    public void encode(Object value, OutputStream os) throws Exception {
        // create the document serializer
        TransformerHandler serializer =
                ((SAXTransformerFactory) SAXTransformerFactory.newInstance())
                        .newTransformerHandler();
        serializer.setResult(new StreamResult(os));

        // cascade on the other encode method
        encode(value, serializer);
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    /** Returns a parser using the given configuration */
    protected Parser getParser(Configuration configuration) {
        Parser parser = new Parser(configuration);
        // spring injection would have been preferred, but it changes the order in which PPIO
        // are listed, which in turn chances the behavior of WPS (e.g., default mimeType)
        parser.setEntityResolver(getResolveProvider().getEntityResolver());
        return parser;
    }

    private EntityResolverProvider getResolveProvider() {
        // double checked locking with volatile, works fine since Java 5.0
        // (http://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java, scroll down towards
        // the end)
        if (resolverProvider == null) {
            synchronized (this) {
                if (resolverProvider == null) {
                    resolverProvider = GeoServerExtensions.bean(EntityResolverProvider.class);
                }
            }
        }

        return resolverProvider;
    }
}
