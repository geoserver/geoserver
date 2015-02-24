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
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;

/**
 * A data format which reads/writes objects from/to a stream.
 *  
 * @author Justin Deoliveira, OpenGEO
 *
 */
public abstract class StreamDataFormat extends DataFormat {

    protected StreamDataFormat(MediaType mediaType) {
        super(mediaType);
    }

   /**
    * Delegates to {@link #read(InputStream)} passing it representation.getStream()
    */
   @Override
   public final Object toObject(Representation representation) {
       try {
           return read( representation.getStream() );
       } 
       catch (IOException e) {
           throw new RuntimeException( e );
       }
   }

   /**
    * Returns an {@link OutputRepresentation} which delegates to {@link #write(Object, OutputStream)}.
    */
   @Override
   public final Representation toRepresentation(final Object object) {
       return new OutputRepresentation(getMediaType()) {
           @Override
           public void write(OutputStream outputStream) throws IOException {
               StreamDataFormat.this.write(object, outputStream);
           }
       };
   }
   
   /**
    * Reads an input stream into an Object.
    * 
    * @param in The input stream whose content is of the type specified by {@link #getMediaType()}.
    *  
    * @return The object.
    * 
    * @throws IOException Any I/O errors that occur reading from the input stream.
    */
   protected abstract Object read( InputStream in ) throws IOException;

   /**
    * Writes an object to an output stream.
    * 
    * @param object The object to write.
    * @param out The output stream whose content is of the type specified by {@link #getMediaType()}.
    *  
    * @throws IOException Any I/O errors that occur writing to the stream.
    */
   protected abstract void write( Object object, OutputStream out ) throws IOException;

}
