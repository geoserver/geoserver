/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.wfs.WFSInfo;
import org.geotools.filter.v1_0.OGC;
import org.geotools.gml2.GML;
import org.geotools.xs.XS;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Utility class for writing out wfs xml documents.
 *
 * <p>Usage:
 *
 * <pre>
 *         WFS wfs = ...;
 *  OutputStream output = ...;
 *
 *         WfsXmlWriter writer = new WfsXmlWriter.WFS1_0( wfs, output );
 *
 *  //declare application schema namespaces
 *  writer.getNamespaceSupport().declareNamespace( "cdf", "http://cite.opengeospatial.org/cite" );
 *
 *  //write the document
 *  writer.openTag( "wfs", "GetCapabilities" );
 *  ...
 *  //write the response
 *  writer.closeTag( "wfs", "GetCapabilities" );
 *  ....
 *
 *  //close the writer
 *  writer.close();
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class WfsXmlWriter {
    /** wfs configuration */
    WFSInfo wfs;

    /** The output stream */
    OutputStream output;

    /** The character encoding. */
    String charSetEncoding;

    /** Declared namespaces */
    NamespaceSupport namespaceSupport;

    /** HashMap schemaLocations */
    HashMap schemaLocations;

    /** The wfs version */
    String version;

    /** writer */
    BufferedWriter writer;

    public WfsXmlWriter(WFSInfo wfs, OutputStream output) {
        this.wfs = wfs;
        this.output = output;

        // default to wfs configured charset
        charSetEncoding = wfs.getGeoServer().getSettings().getCharset();

        // schema locations
        schemaLocations = new HashMap();

        // declare the namespaces ( wfs is default )
        namespaceSupport = new NamespaceSupport();
        namespaceSupport.declarePrefix("xs", XS.NAMESPACE);
        namespaceSupport.declarePrefix("ogc", OGC.NAMESPACE);
        namespaceSupport.declarePrefix("gml", GML.NAMESPACE);

        namespaceSupport.declarePrefix("wfs", org.geoserver.wfs.xml.v1_0_0.WFS.NAMESPACE);
        namespaceSupport.declarePrefix("", org.geoserver.wfs.xml.v1_0_0.WFS.NAMESPACE);
    }

    public void setCharSetEncoding(String charSetEncoding) {
        this.charSetEncoding = charSetEncoding;
    }

    public NamespaceSupport getNamespaceSupport() {
        return namespaceSupport;
    }

    private void init() throws IOException {
        // namespace declarations
        for (Enumeration e = namespaceSupport.getDeclaredPrefixes(); e.hasMoreElements(); ) {
            String pre = (String) e.nextElement();
            String uri = namespaceSupport.getURI(pre);

            if ("".equals(pre)) {
                attribute("xmlns" + pre, uri);
            } else {
                attribute("xmlns:" + pre, uri);
            }
        }

        // schema locations
        if (!schemaLocations.isEmpty()) {
            StringBuffer buffer = new StringBuffer();

            for (Iterator e = schemaLocations.entrySet().iterator(); e.hasNext(); ) {
                Map.Entry entry = (Entry) e.next();
                String uri = (String) entry.getKey();
                String location = (String) entry.getValue();

                buffer.append(uri + " " + location);

                if (e.hasNext()) {
                    buffer.append(" ");
                }
            }

            attribute("xs:schemaLocation", buffer.toString());
        }
    }

    private void attribute(String name, String value) throws IOException {
        writer.write(" " + name + "=\"" + value + "\"");
    }

    public void openTag(String prefix, String name) throws IOException {
        openTag(prefix, name, null);
    }

    public void openTag(String prefix, String name, String[] attributes) throws IOException {
        boolean root = writer == null;

        if (root) {
            writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    output, wfs.getGeoServer().getSettings().getCharset()));

            // write the processing instruction
            writer.write("<?xml version=\"1.0\" encoding=\"" + charSetEncoding + "\"?>");
        }

        // write the element
        if (prefix != null) {
            writer.write("<" + prefix + ":" + name);
        } else {
            writer.write("<" + name);
        }

        if (root) {
            init();
        }

        if (attributes != null) {
            for (int i = 0; i < attributes.length; i += 2) {
                attribute(attributes[i], attributes[i + 1]);
            }
        }

        writer.write(">");
    }

    public void text(String text) throws IOException {
        writer.write(text);
    }

    public void closeTag(String prefix, String name) throws IOException {
        // write the element
        if (prefix != null) {
            writer.write("</" + prefix + ":" + name + ">");
        } else {
            writer.write("</" + name + ">");
        }
    }

    public void close() throws IOException {
        // close the writer
        writer.flush();
        writer.close();

        writer = null;
    }

    public static class WFS1_0 extends WfsXmlWriter {
        public WFS1_0(WFSInfo wfs, OutputStream output) {
            super(wfs, output);

            // set the schema location
            schemaLocations.put(
                    org.geoserver.wfs.xml.v1_0_0.WFS.NAMESPACE,
                    ResponseUtils.appendPath(
                            wfs.getSchemaBaseURL(), "wfs/1.0.0/WFS-transaction.xsd"));

            version = "1.0.0";
        }
    }

    public static class WFS1_1 extends WfsXmlWriter {
        public WFS1_1(WFSInfo wfs, OutputStream output) {
            super(wfs, output);

            // add the ows namespace
            namespaceSupport.declarePrefix("ows", OWS.NAMESPACE);

            // set the schema location
            schemaLocations.put(
                    org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE,
                    ResponseUtils.appendPath(wfs.getSchemaBaseURL(), "wfs/1.1.0/wfs.xsd"));

            version = "1.1.0";
        }
    }
}
