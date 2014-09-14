/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.app;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.python.Python;
import org.geoserver.rest.RestletException;
import org.python.util.PythonInterpreter;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Resource;

public class PythonResource extends Resource {

    Python jython;
    File script;

    public PythonResource(Python jython, File script, Request request, Response response) {
        super(null, request, response);
        this.jython = jython;
        this.script = script;
    }
    
    @Override
    public void handleGet() {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            PythonInterpreter pi = jython.interpreter();
            
            //set arguments as local variables
            Form query = getRequest().getResourceRef().getQueryAsForm();
            for (String name : query.getNames()) {
                pi.set(name, query.getFirstValue(name));
            }
            
            pi.setOut(out);
            pi.execfile(new FileInputStream(script));
            
            getResponse().setEntity(new OutputRepresentation(MediaType.TEXT_PLAIN) {
                @Override
                public void write(OutputStream output) throws IOException {
                    output.write(out.toByteArray());
                }
            });
            getResponse().setStatus(Status.SUCCESS_OK);
        } 
        catch (Exception e) {
            throw new RestletException("Error executing script " + script.getName(), 
                Status.SERVER_ERROR_INTERNAL, e);
        }
        
    }
}
