/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

/**
 * A format that automatically converts a map into JSON and vice versa.
 * <p>
 * The <a href="http://json-lib.sourceforge.net/">json-lib</a> library is used to read and 
 * write JSON.
 * </p>
 * 
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class MapJSONFormat extends StreamDataFormat {

    public MapJSONFormat(){
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        //TODO: character set
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(out));

        //JD: why does this initial flush occur?
        outWriter.flush();

        JSON obj = (JSON)toJSONObject(object);

        obj.write(outWriter);
        outWriter.flush();
    }
    
    public Object toJSONObject(Object obj) {
        if (obj instanceof Map) {
            Map m = (Map) obj;
            JSONObject json = new JSONObject();
            Iterator it = m.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                json.put((String) entry.getKey(), toJSONObject(entry.getValue()));
            }

            return json;
        } else if (obj instanceof Collection) {
            Collection col = (Collection) obj;
            JSONArray json = new JSONArray();
            Iterator it = col.iterator();

            while (it.hasNext()) {
                json.add(toJSONObject(it.next()));
            }

            return json;
        } else if (obj instanceof Number) {
            return obj;
        } else if (obj instanceof Boolean) {
            return obj;
        } else if (obj == null) {
            return JSONNull.getInstance();
        } else {
            return obj.toString();
        }
    }
    
    public Representation createRepresentation(Object data, Resource resource,
            Request request, Response response) {
        return null;
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        //TODO: character set
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        StringBuilder text = new StringBuilder();
        String line = null;
        while( ( line = reader.readLine() ) != null ) {
            text.append( line );
        }
        return JSONObject.fromObject(text.toString());
    }
}
