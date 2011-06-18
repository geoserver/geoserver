/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

/*
 * Created on April 20, 2005
 *
 */
package org.vfny.geoserver.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.apache.xerces.parsers.SAXParser;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public class SLDValidator {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver");

    public SLDValidator() {
    }

    /**
     * validates against the "normal" location of the schema (ie.
     * ".../schemas/sld/StyleLayerDescriptor.xsd" uses the geoserver_home
     * patch
     *
     * @param xml
     * @param baseUrl GeoServer base URL
     *
     * @return
     */
    public List validateSLD(InputStream xml, String baseUrl) {
        // a riminder not to use the data directory for the schemas
        //String url = GeoserverDataDirectory.getGeoserverDataDirectory(servContext).toString();
        return validateSLD(new InputSource(xml), baseUrl);
    }

    public static String getErrorMessage(InputStream xml, List errors) {
        return getErrorMessage(new InputStreamReader(xml), errors);
    }

    /**
     * returns a better formated error message - suitable for framing. There's
     * a more complex version in StylesEditorAction. This will kick out a VERY
     * LARGE errorMessage.
     *
     * @param xml
     * @param errors
     *
     * @return DOCUMENT ME!
     */
    public static String getErrorMessage(Reader xml, List errors) {
        BufferedReader reader = null;
        StringBuffer result = new StringBuffer();
        result.append("Your SLD is not valid.\n");
        result.append(
            "Most common problems are: \n(1) no namespaces - use <ows:GetMap>, <sld:Rule>, <ogc:Filter>, <gml:Point>  - the part before the ':' is important\n");
        result.append("(2) capitialization - use '<And>' not '<and>' \n");
        result.append("(3) Order - The order of elements is important \n");
        result.append(
            "(4) Make sure your first tag imports the correct namespaces.  ie. xmlns:sld=\"http://www.opengis.net/sld\" for EVERY NAMESPACE \n");
        result.append("\n");

        try {
            reader = new BufferedReader(xml);

            String line = reader.readLine();
            int linenumber = 1;
            int exceptionNum = 0;

            //check for lineNumber -1 errors  --> invalid XML
            if (errors.size() > 0) {
                SAXParseException sax = (SAXParseException) errors.get(0);

                if (sax.getLineNumber() < 0) {
                    result.append("   INVALID XML: " + sax.getLocalizedMessage() + "\n");
                    result.append(" \n");
                    exceptionNum = 1; // skip ahead (you only ever get one error in this case)
                }
            }

            while (line != null) {
                line.replace('\n', ' ');
                line.replace('\r', ' ');

                String header = linenumber + ": ";
                result.append(header + line + "\n"); // record the current line

                boolean keep_going = true;

                while (keep_going) {
                    if ((exceptionNum < errors.size())) {
                        SAXParseException sax = (SAXParseException) errors.get(exceptionNum);

                        if (sax.getLineNumber() <= linenumber) {
                            String head = "---------------------".substring(0, header.length() - 1);
                            String body = "--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";

                            int colNum = sax.getColumnNumber(); //protect against col 0 problems

                            if (colNum < 1) {
                                colNum = 1;
                            }

                            if (colNum > body.length()) {
                                body = body + body + body + body + body + body; // make it longer (not usually required, but might be for SLD_BODY=... which is all one line)

                                if (colNum > body.length()) {
                                    colNum = body.length();
                                }
                            }

                            result.append(head + body.substring(0, colNum - 1) + "^\n");
                            result.append("       (line " + sax.getLineNumber() + ", column "
                                + sax.getColumnNumber() + ")" + sax.getLocalizedMessage() + "\n");
                            exceptionNum++;
                        } else {
                            keep_going = false; //report later (sax.getLineNumber() > linenumber)
                        }
                    } else {
                        keep_going = false; // no more errors to report
                    }
                }

                line = reader.readLine(); //will be null at eof
                linenumber++;
            }

            for (int t = exceptionNum; t < errors.size(); t++) {
                SAXParseException sax = (SAXParseException) errors.get(t);
                result.append("       (line " + sax.getLineNumber() + ", column "
                    + sax.getColumnNumber() + ")" + sax.getLocalizedMessage() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }

    /*public List validateSLD(InputStream xml, String SchemaUrl) {
        return validateSLD(new InputSource(xml), SchemaUrl);
    }*/

    /*public List validateSLD(InputSource xml, ServletContext servContext) {
        File schemaFile = new File(servContext.getRealPath("/"),
        "/schemas/sld/StyledLayerDescriptor.xsd");

        try {
            return validateSLD(xml, schemaFile.toURL().toString());
        } catch (Exception e) {
            ArrayList al = new ArrayList();
            al.add(new SAXException(e));

            return al;
        }
    }*/

    /**
     * validate a .sld against the schema
     *
     * @param xml input stream representing the .sld file
     * @param baseURL 
     * @param SchemaUrl location of the schemas. Normally use
     *        ".../schemas/sld/StyleLayerDescriptor.xsd"
     *
     * @return list of SAXExceptions (0 if the file's okay)
     */
    public List validateSLD(InputSource xml, String baseURL) {
        SAXParser parser = new SAXParser();

        try {
            String schemaUrl = null;
            if (baseURL != null) {
                String schemaLoction = ResponseUtils.buildSchemaURL(baseURL, "sld/StyledLayerDescriptor.xsd");
                // this takes care of spaces in the path to the file
                URL schemaFile = new URL(schemaLoction);
    
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(new StringBuffer("Validating SLD with ").append(schemaFile.toString())
                                                                        .toString());
                }
    
                schemaUrl = schemaFile.toString();
            }
            else {
                File file = GeoServerExtensions.bean(GeoServerResourceLoader.class)
                    .find("schemas", "sld", "StyledLayerDescriptor.xsd");
                if (file == null) {
                    throw new RuntimeException("base url not specified and unable to find internal SLD schema");
                }
                
                schemaUrl = DataUtilities.fileToURL(file).toString();
            }

            //     1. tell the parser to validate the XML document vs the schema
            //     2. does not validate the schema (the GML schema is *not* valid.  This is
            //        			an OGC blunder)
            //     3. tells the validator that the tags without a namespace are actually
            //        			SLD tags.
            //     4. tells the validator to 'override' the SLD schema that a user may
            //        			include with the one inside geoserver.
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking",
                false);

            parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
                schemaUrl);
            parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                "http://www.opengis.net/sld " + schemaUrl);

            //parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation","http://www.opengis.net/ows "+SchemaUrl);
            Validator handler = new Validator();
            parser.setErrorHandler(handler);
            parser.parse(xml);

            return handler.errors;
        } catch (java.io.IOException ioe) {
            ArrayList al = new ArrayList();
            al.add(new SAXParseException(ioe.getLocalizedMessage(), null));

            return al;
        } catch (SAXException e) {
            ArrayList al = new ArrayList();
            al.add(new SAXParseException(e.getLocalizedMessage(), null));

            return al;
        }
    }

    // errors in the document will be put in "errors".
    // if errors.size() ==0  then there were no errors.
    private class Validator extends DefaultHandler {
        public ArrayList errors = new ArrayList();

        public void error(SAXParseException exception)
            throws SAXException {
            errors.add(exception);
        }

        public void fatalError(SAXParseException exception)
            throws SAXException {
            errors.add(exception);
        }

        public void warning(SAXParseException exception)
            throws SAXException {
            //do nothing
        }
    }
}
