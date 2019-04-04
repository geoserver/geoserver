/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestException;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptSession;
import org.geoserver.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    static Logger LOGGER = Logger.getLogger("org.geoserver.script.rest");

    @Autowired ScriptManager scriptMgr;

    public void getSessions(HttpServletResponse response, String language) {
        JSONArray array = new JSONArray();
        String ext = language;
        for (ScriptSession session : scriptMgr.findSessions(ext)) {
            array.add(toJSON(session));
        }
        JSONObject obj = new JSONObject();
        obj.put("sessions", array);

        try {
            response.getWriter().write(obj.toString(2));
            response.setStatus(200);
        } catch (IOException e) {
            throw new RestException("i/o error", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void getScriptSession(
            HttpServletRequest request, HttpServletResponse response, String language, int id) {
        JSONObject obj = null;
        obj = toJSON(scriptMgr.findSession(Long.valueOf(id)));
        try {
            response.getWriter().write(obj.toString(2));
            response.setStatus(200);
        } catch (IOException e) {
            throw new RestException("i/o error", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void createScriptingSession(
            HttpServletRequest request, HttpServletResponse response, String language) {
        ScriptSession session = scriptMgr.createNewSession(language);
        try {
            if (session == null) {
                response.getWriter().write("Unable to create session");
                response.setStatus(500);
            } else {
                response.setHeader(
                        "Location",
                        URI.create(
                                        request.getRequestURL().toString()
                                                + "/"
                                                + Long.toString(session.getId()))
                                .toString());
                response.getWriter().write(Long.toString(session.getId()));
                response.setStatus(201);
            }
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    public void executeScript(
            HttpServletRequest request, HttpServletResponse response, String language, int id) {
        ScriptSession session = scriptMgr.findSession(Long.valueOf(id));
        ScriptEngine engine = session.getEngine();

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        Writer w = new PrintWriter(output);

        engine.getContext().setWriter(w);
        engine.getContext().setErrorWriter(w);

        try {
            try {
                String x = IOUtils.toString(request.getInputStream());
                Object result = engine.eval(x);
                if (result != null) {
                    w.write(result.toString());
                }
            } catch (ScriptException | IOException e) {
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
        } catch (IOException e) {
            throw new RestException("i/o error", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        try {
            response.getWriter().write(output.toString());
            response.setStatus(200);
        } catch (IOException e) {
            throw new RestException("i/o error", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    JSONObject toJSON(ScriptSession session) {
        RequestInfo pg = RequestInfo.get();

        JSONObject obj = new JSONObject();
        obj.put("id", session.getId());
        obj.put("engine", session.getEngineName());
        obj.put(
                "self",
                pg.getBaseURL()
                        + pg.getPagePath()
                        + "/"
                        + session.getExtension()
                        + "/"
                        + Long.toString(session.getId()));

        return obj;
    }
}
