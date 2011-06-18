/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import javax.xml.namespace.QName;

import org.xml.sax.ContentHandler;

/**
 * Process parameter input / output for objects which are XML based.
 *  
 * @author Justin Deoliveira, OpenGEO
 */
public abstract class XMLPPIO extends ComplexPPIO {

    /** */
    protected QName element;
    
    /**
     * Constructor specifying 'text/xml' as mime type.
     */
    protected XMLPPIO(Class externalType, Class internalType, QName element) {
        this( externalType, internalType, "text/xml", element );
    }
    
    /**
     * Constructor explicitly specifying mime type.  
     */
    protected XMLPPIO(Class externalType, Class internalType, String mimeType, QName element) {
        super( externalType, internalType, mimeType);
        this.element = element;
    }

    /**
     * The qualified name of the XML element in the XML representation of the object.  
     */
    public QName getElement() {
        return element;
    }
    
    /**
     * Encodes the internal representation of the object to an XML stream.
     * 
     * @param object An object of type {@link #getType()}.
     * @param handler An XML content handler.
     */
    public abstract void encode( Object object, ContentHandler handler ) throws Exception;

}
