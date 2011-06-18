/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest.format;

import org.restlet.data.MediaType;
import org.restlet.service.ConverterService;


/**
 * Converts back and forth between objects and a representation of a particular format.
 * 
 * @author David Winslow <dwinslow@openplans.org>
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class DataFormat extends ConverterService {

    /** the media / mime type */
    protected final MediaType mediaType;
    
    /**
     * Constructs a new format.
     * 
     * @param mediaType The mime/media type of the format.
     */
    protected DataFormat( MediaType mediaType ) {
        this.mediaType = mediaType;
    }
    
    /**
     * Returns the mime type of the format.
     */
    public final MediaType getMediaType() {
        return mediaType;
    }
}
