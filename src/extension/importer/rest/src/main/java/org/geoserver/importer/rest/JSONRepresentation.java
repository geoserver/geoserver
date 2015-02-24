/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.sf.json.JSONObject;

import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

/**
 * Output representation that serializes a {@link JSONObject}.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class JSONRepresentation extends OutputRepresentation {

    JSONObject obj;

    public JSONRepresentation(JSONObject obj) {
        super(MediaType.APPLICATION_JSON);
        this.obj = obj;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
        obj.write(w);
        w.flush();
    }

}
