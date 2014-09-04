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
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Data format for serializing and de-serializing an object as JSON with XStream.
 * <p>
 * Subclasses should override the {@link #read(InputStream)} and {@link #write(Object, OutputStream)}
 * methods to create a customized xstream instance, or to use some other JSON serialization method. 
 * </p>
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class ReflectiveJSONFormat extends StreamDataFormat {

    /**
     * xstream instance for encoding and persisting
     */
    XStream xstream;
    
    public ReflectiveJSONFormat() {
        super(MediaType.APPLICATION_JSON);
        this.xstream = new XStream(new JettisonMappedXmlDriver());
    }

    /**
     * Returns the xstream instance used for encoding and decoding.
     */
    public XStream getXStream() {
        return xstream;
    }
    
    /**
     * Reads an JSON input stream into an object.
     *  
     * @param in The json.
     * 
     * @return The object de-serialized from JSON.
     */
    protected Object read( InputStream input ) throws IOException {
        return xstream.fromXML( input );
    }

    /**
     * Writes an object as JSON to an output stream.
     * 
     * @param data The object.
     * @param output The output stream.
     */
    protected void write( Object data, OutputStream output ) throws IOException {
        xstream.toXML( data, output );
    }
}
