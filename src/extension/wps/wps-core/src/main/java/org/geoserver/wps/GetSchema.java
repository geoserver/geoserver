/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implements schema publishing in support of Complex data types
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class GetSchema {
    public WPSInfo wps;

    public GetSchema(WPSInfo wps) {
        this.wps = wps;
    }

    /** Fetches named schema and writes it to the response stream */
    @SuppressWarnings("unchecked")
    public void run(HttpServletRequest request, HttpServletResponse response) {
        String name = null;

        // Iterate over all parameters looking case insensitively for 'identifier'
        for (Enumeration<String> a = request.getParameterNames(); a.hasMoreElements(); ) {
            String i = a.nextElement();

            if ("identifier".equalsIgnoreCase(i)) {
                name = request.getParameter(i);

                break;
            }
        }

        if (null == name) {
            throw new WPSException("NoApplicableCode", "No Identifier key and value.");
        }

        InputStream stream = org.geoserver.wps.schemas.Stub.class.getResourceAsStream(name);

        if (null == stream) {
            throw new WPSException("NoApplicableCode", "No Schema '" + name + "'.");
        }

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder schema = new StringBuilder();
        String line = null;

        try {
            while (null != (line = bufReader.readLine())) {
                schema.append(line + "\n");
            }

            bufReader.close();
        } catch (Exception e) {
            throw new WPSException("NoApplicableCode", "Error reading schema on server.");
        }

        response.setContentType("text/xml");

        try {
            response.getOutputStream().print(schema.toString());
        } catch (Exception e) {
            throw new WPSException("NoApplicableCode", "Could not write schema to output.");
        }
    }
}
