/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.javascript;

import java.io.InputStream;
import java.io.OutputStream;

import org.restlet.data.MediaType;
import org.restlet.resource.StreamRepresentation;

/**
 * The JavaScriptStreamRepresentation class provides a default constructor so that Rhino can wrap
 * it.  Other than in Rhino scripts, StreamRepresentation, OutputRepresentation, or
 * InputRepresentation should be preferred over this class.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public abstract class JavaScriptStreamRepresentation extends StreamRepresentation {
    public JavaScriptStreamRepresentation() {
        super(MediaType.APPLICATION_OCTET_STREAM);
    }
}
