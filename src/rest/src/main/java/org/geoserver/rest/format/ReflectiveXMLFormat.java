/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.restlet.data.MediaType;

import com.thoughtworks.xstream.XStream;

/**
 * Data format for serializing and de-serializing an object as XML with XStream.
 * <p>
 * Subclasses should override the {@link #read(InputStream)} and {@link #write(Object, OutputStream)}
 * methods to create a customized xstream instance, or to use some other XML serialization method. 
 * </p>
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class ReflectiveXMLFormat extends StreamDataFormat {

    XStream xstream;
    
    public ReflectiveXMLFormat() {
        super(MediaType.APPLICATION_XML);
        this.xstream = new XStream();
    }
    
    /**
     * Returns the xstream instance used for encoding and decoding.
     */
    public XStream getXStream() {
        return xstream;
    }
    
    /**
     * Reads an XML input stream into an object.
     *  
     * @param in The xml.
     * 
     * @return The object de-serialized from XML.
     */
    protected Object read( InputStream in ) throws IOException {
        return xstream.fromXML( in );
    }

    /**
     * Writes an object as XML to an output stream.
     * 
     * @param data The object.
     * @param output The output stream.
     */
    protected void write( Object data, OutputStream output ) throws IOException {
        xstream.toXML( data, output );
    }

}
