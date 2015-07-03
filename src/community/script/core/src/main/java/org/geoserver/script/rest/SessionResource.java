/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptSession;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 * Resource for a script session.
 * <p>
 * This resource provides access to {@link ScriptSession} instances. It allows for the following 
 * operations:
 * <ul>
 *   <li>POST /sessions : create a new session, returning its identifier
 *   <li>GET /sessions/[id] : return info about session [id]
 *   <li>PUT /sessions/[id] : execute a statement in session [id]
 * <ul> 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
public class SessionResource extends Resource {

    static Logger LOGGER = Logger.getLogger("org.geoserver.script.rest");

    ScriptManager scriptMgr;

    public SessionResource(ScriptManager scriptMgr, Request request, Response response) {
        super(null, request, response);
        this.scriptMgr = scriptMgr;
    }

    @Override
    public void handleGet() {
        String s = (String) getRequest().getAttributes().get("session");
        String ext = (String) getRequest().getAttributes().get("ext");

        JSONObject obj = null;
        if (s != null) {
            //return individual
            obj = toJSON(scriptMgr.findSession(Long.valueOf(s)));
        }
        else {
            //return all
            JSONArray array = new JSONArray();
            for (ScriptSession session : scriptMgr.findSessions(ext)) {
                array.add(toJSON(session));
            }

            obj = new JSONObject();
            obj.put("sessions", array);
        }

        getResponse().setEntity(new StringRepresentation(obj.toString(2), MediaType.APPLICATION_JSON));
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    JSONObject toJSON(ScriptSession session) {
        PageInfo pageInfo = (PageInfo) getRequest().getAttributes().get(PageInfo.KEY);

        JSONObject obj = new JSONObject();
        obj.put("id", session.getId());
        obj.put("engine", session.getEngineName());
        obj.put("self", pageInfo.baseURI(
            String.format("sessions/%s/%d", session.getExtension(), session.getId())));

        return obj;
    }

    @Override
    public boolean allowPost() {
        return !getRequest().getAttributes().containsKey("session");
    }

    @Override
    public void handlePost() {
        String ext = (String) getRequest().getAttributes().get("ext");
        ScriptSession session = scriptMgr.createNewSession(ext);
        if (session == null) {
            throw new RestletException("Unable to create session", Status.SERVER_ERROR_INTERNAL);
        }

        PageInfo page = (PageInfo) getRequest().getAttributes().get(PageInfo.KEY);

        getResponse().redirectSeeOther(page.pageURI(String.valueOf(session.getId())));
        getResponse().setEntity(new StringRepresentation(String.valueOf(session.getId())));
        getResponse().setStatus(Status.SUCCESS_CREATED);

    }

    @Override
    public boolean allowPut() {
        return getRequest().getAttributes().containsKey("session");
    }

    @Override
    public void handlePut() {
        long sid = Long.valueOf((String)getRequest().getAttributes().get("session"));
        ScriptSession session = scriptMgr.findSession(sid);
        ScriptEngine engine = session.getEngine();

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer w = new PrintWriter(output);

        engine.getContext().setWriter(w);
        engine.getContext().setErrorWriter(w);

        try {
            try {
                Object result = engine.eval(new InputStreamReader(getRequest().getEntity().getStream()));
                if (result != null) {
                    w.write(result.toString());
                }
            } catch (ScriptException e) {
                Throwable t = e;
                if (t.getCause() != null) {
                    t = t.getCause();
                }
                t.printStackTrace(new PrintWriter(w));
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Script error", e);
                }
            }
            w.flush();
        }
        catch(IOException e) {
            throw new RestletException("i/o error", Status.SERVER_ERROR_INTERNAL, e);
        }
        
        getResponse().setEntity(new OutputRepresentation(MediaType.TEXT_PLAIN) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                outputStream.write(output.toByteArray());
            }
        });
        getResponse().setStatus(Status.SUCCESS_OK);
    }
}
