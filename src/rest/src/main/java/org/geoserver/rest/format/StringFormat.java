/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;

/**
 * Simple format which takes and returns a string.
 * 
 * @author Justin Deoliveira, OpenGEO
 */
public class StringFormat extends DataFormat {

    public StringFormat(MediaType mediaType) {
        super(mediaType);
    }
    
    @Override
    public Representation toRepresentation(Object object) {
        return new StringRepresentation( object.toString(), mediaType );
    }
    
    @Override
    public Object toObject(Representation representation) {
        try {
            return representation.getText();
        } catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

}
