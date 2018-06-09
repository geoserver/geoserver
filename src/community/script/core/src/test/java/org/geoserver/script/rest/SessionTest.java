/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.script.ScriptIntTestSupport;
import org.springframework.mock.web.MockHttpServletResponse;

public class SessionTest extends ScriptIntTestSupport {

    public void testPost() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/script/sessions/js", "");
        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));

        long sessionId = Long.valueOf(response.getContentAsString());
        assertTrue(response.getHeader("Location").endsWith("sessions/js/" + sessionId));
    }

    public void testPut() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/script/sessions/js", "");
        assertEquals(201, response.getStatus());

        long sid = Long.valueOf(response.getContentAsString());
        response =
                putAsServletResponse(
                        "/script/sessions/js/" + sid, "print('Hello World!');", "text/plain");

        assertEquals(
                "Hello World!",
                response.getContentAsString().trim()); // print is a Rhino-specific function

        putAsServletResponse("/script/sessions/js/" + sid, "var x = 3;", "text/plain");

        response = putAsServletResponse("/script/sessions/js/" + sid, "print(x);", "text/plain");
        assertEquals("3", response.getContentAsString().trim());
    }

    public void testGet() throws Exception {
        testPost();
        testPost();

        JSONObject result = (JSONObject) getAsJSON("/script/sessions");
        assertTrue(result.has("sessions"));

        JSONArray sessions = result.getJSONArray("sessions");
        assertEquals(2, sessions.size());

        JSONObject session = sessions.getJSONObject(0);
        assertTrue(session.has("id"));
        assertTrue(session.has("engine"));
        assertTrue(session.has("self"));

        int sid = session.getInt("id");
        assertTrue(session.getString("self").endsWith("/sessions/js/" + sid));

        session = (JSONObject) getAsJSON("/script/sessions/js/" + sid);
        assertTrue(session.has("id"));
        assertTrue(session.has("engine"));
        assertTrue(session.has("self"));

        assertEquals(sid, session.getInt("id"));
    }
}
