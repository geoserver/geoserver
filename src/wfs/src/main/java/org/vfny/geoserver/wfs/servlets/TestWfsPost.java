/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wfs.servlets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;


/**
 * Simple tester for WFS post requests. Can be called two ways. If called with
 * no parameters, it displays the form, otherwise it displays the result page.
 *
 * @author Doug Cates: Moxi Media Inc.
 * @version 1.0
 */
public class TestWfsPost extends HttpServlet {
    
    /**
     * The path at which TestWfsPost is exposed. Used to find the full location of GeoServer
     * without doing complex and error prone string building
     */
    static final String TEST_WFS_POST_PATH = "/TestWfsPost";
    
    static final Logger LOGGER = Logging.getLogger(TestWfsPost.class);
    
    /**
     * Initializes the servlet.
     *
     * @param config DOCUMENT ME!
     *
     * @throws ServletException DOCUMENT ME!
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Destroys the servlet.
     */
    public void destroy() {
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return DOCUMENT ME!
     */
    public String getServletInfo() {
        return "Tests a WFS post request using a form entry.";
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws ServletException DOCUMENT ME!
     * @throws IOException DOCUMENT ME!
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String requestString = request.getParameter("body");
        String urlString = request.getParameter("url");
        boolean doGet = (requestString == null) || requestString.trim().equals("");

        if ((urlString == null)) {
            PrintWriter out = response.getWriter();
            StringBuffer urlInfo = request.getRequestURL();

            if (urlInfo.indexOf("?") != -1) {
                urlInfo.delete(urlInfo.indexOf("?"), urlInfo.length());
            }

            String geoserverUrl = urlInfo.substring(0, urlInfo.indexOf("/", 8))
                + request.getContextPath();
            response.setContentType("text/html");
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>TestWfsPost</title>");
            out.println("</head>");
            out.println("<script language=\"JavaScript\">");
            out.println("function doNothing() {");
            out.println("}");
            out.println("function sendRequest() {");
            out.println("  if (checkURL()==true) {");
            out.print("    document.frm.action = \"");
            out.print(urlInfo.toString());
            out.print("\";\n");
            out.println("    document.frm.target = \"_blank\";");
            out.println("    document.frm.submit();");
            out.println("  }");
            out.println("}");
            out.println("function checkURL() {");
            out.println("  if (document.frm.url.value==\"\") {");
            out.println("    alert(\"Please give URL before you sumbit this form!\");");
            out.println("    return false;");
            out.println("  } else {");
            out.println("    return true;");
            out.println("  }");
            out.println("}");
            out.println("function clearRequest() {");
            out.println("document.frm.body.value = \"\";");
            out.println("}");
            out.println("</script>");
            out.println("<body>");
            out.println("<form name=\"frm\" action=\"JavaScript:doNothing()\" method=\"POST\">");
            out.println("<table align=\"center\" cellspacing=\"2\" cellpadding=\"2\" border=\"0\">");
            out.println("<tr>");
            out.println("<td><b>URL:</b></td>");
            out.print("<td><input name=\"url\" value=\"");
            out.print(geoserverUrl);
            out.print("/wfs/GetFeature\" size=\"70\" MAXLENGTH=\"100\"/></td>\n");
            out.println("</tr>");
            out.println("<tr>");
            out.println("<td><b>Request:</b></td>");
            out.println("<td><textarea cols=\"60\" rows=\"24\" name=\"body\"></textarea></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("<table align=\"center\">");
            out.println("<tr>");
            out.println(
                "<td><input type=\"button\" value=\"Clear\" onclick=\"clearRequest()\"></td>");
            out.println(
                "<td><input type=\"button\" value=\"Submit\" onclick=\"sendRequest()\"></td>");
            out.println("<td></td>");
            out.println("</tr>");
            out.println("</table>");
            out.println("</form>");
            out.println("</body>");
            out.println("</html>");
            out.close();
        } else {
            response.setContentType("application/xml");

            BufferedReader xmlIn = null;
            PrintWriter xmlOut = null;
            StringBuffer sbf = new StringBuffer();
            String resp = null;

            try {
                URL u = new URL(urlString);
                validateURL(request, urlString, getProxyBaseURL() );
                java.net.HttpURLConnection acon = (java.net.HttpURLConnection) u.openConnection();
                acon.setAllowUserInteraction(false);

                if (!doGet) {
                    //System.out.println("set to post");
                    acon.setRequestMethod("POST");
                    acon.setRequestProperty("Content-Type", "application/xml");
                } else {
                    //System.out.println("set to get");
                    acon.setRequestMethod("GET");
                }

                acon.setDoOutput(true);
                acon.setDoInput(true);
                acon.setUseCaches(false);

                //SISfixed - if there was authentication info in the request,
                //           Pass it along the way to the target URL
                //DJB: applied patch in GEOS-335
                String authHeader = request.getHeader("Authorization");

                String username = request.getParameter("username");

                if ((username != null) && !username.trim().equals("")) {
                    String password = request.getParameter("password");
                    String up = username + ":" + password;
                    byte[] encoded = Base64.encodeBase64(up.getBytes());
                    authHeader = "Basic " + new String(encoded);
                }

                if (authHeader != null) {
                    acon.setRequestProperty("Authorization", authHeader);
                }

                if (!doGet) {
                    xmlOut = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(acon.getOutputStream())));
                    xmlOut = new java.io.PrintWriter(acon.getOutputStream());

                    xmlOut.write(requestString);
                    xmlOut.flush();
                }

                // Above 400 they're all error codes, see:
                // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
                if (acon.getResponseCode() >= 400) {
                    PrintWriter out = response.getWriter();
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<servlet-exception>");
                    out.println("HTTP response: " + acon.getResponseCode() + "\n"
                        + URLDecoder.decode(acon.getResponseMessage(), "UTF-8"));
                    out.println("</servlet-exception>");
                    out.close();
                } else {
                    // xmlIn = new BufferedReader(new InputStreamReader(
                    // acon.getInputStream()));
                    // String line;

                    // System.out.println("got encoding from acon: "
                    // + acon.getContentType());
                    response.setContentType(acon.getContentType());
                    response.setHeader("Content-disposition",
                        acon.getHeaderField("Content-disposition"));

                    OutputStream output = response.getOutputStream();
                    int c;
                    InputStream in = acon.getInputStream();

                    while ((c = in.read()) != -1)
                        output.write(c);

                    in.close();
                    output.close();
                }

                //while ((line = xmlIn.readLine()) != null) {
                //    out.print(line);
                //}
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Failure dealing with the request", e);
                PrintWriter out = response.getWriter();
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.println("<servlet-exception>");
                out.println(ResponseUtils.encodeXML(e.toString()));
                out.println("</servlet-exception>");
                out.close();
            } finally {
                try {
                    if (xmlIn != null) {
                        xmlIn.close();
                    }
                } catch (Exception e1) {
                    LOGGER.log(Level.FINE, "Internal failure dealing with the request", e1);
                    PrintWriter out = response.getWriter();
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<servlet-exception>");
                    out.println(ResponseUtils.encodeXML(e1.toString()));
                    out.println("</servlet-exception>");
                    out.close();
                }

                try {
                    if (xmlOut != null) {
                        xmlOut.close();
                    }
                } catch (Exception e2) {
                    LOGGER.log(Level.FINE, "Internal failure dealing with the request", e2);
                    PrintWriter out = response.getWriter();
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<servlet-exception>");
                    out.println(ResponseUtils.encodeXML(e2.toString()));
                    out.println("</servlet-exception>");
                    out.close();
                }
            }
        }
    }

    String getProxyBaseURL() {
        GeoServer geoServer = (GeoServer) GeoServerExtensions.bean("geoServer");
        if( geoServer != null ){
            geoServer.getGlobal().getSettings().getProxyBaseUrl();
        }
        return null;
    }

    void validateURL(HttpServletRequest request, String url, String proxyBase) {
        if(proxyBase != null) {
            if(!url.startsWith(proxyBase)) {
                throw new IllegalArgumentException("Invalid url requested, the demo requests should be hitting: " + proxyBase);
            }
        } else {
            // use the requested url then, and remove the TestWfsPort
            String requestedUrl = request.getRequestURL().toString();
            // this should not happen, but let's not make it an open proxy if it does
            if(!requestedUrl.endsWith(TEST_WFS_POST_PATH)) {
                throw new IllegalStateException("Unepected, the TestWfsPost was accessed by a path not ending with TestWfsPost: " + requestedUrl);
            }
            String base = requestedUrl.substring(0, requestedUrl.lastIndexOf(TEST_WFS_POST_PATH));
            if(!url.startsWith(base)) {
                throw new IllegalArgumentException("Invalid url requested, the demo requests should be hitting: " + base);
            }
        }
    }
}
