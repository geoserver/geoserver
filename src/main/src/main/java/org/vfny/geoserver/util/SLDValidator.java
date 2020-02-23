/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Created on April 20, 2005
 *
 */
package org.vfny.geoserver.util;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class SLDValidator {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver");

    EntityResolver entityResolver;

    public SLDValidator() {}

    /** Validates against the SLD schema in the classpath */
    public List validateSLD(InputStream xml) {
        return validateSLD(new InputSource(xml));
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public static String getErrorMessage(InputStream xml, List errors) {
        return getErrorMessage(new InputStreamReader(xml), errors);
    }

    /**
     * returns a better formated error message - suitable for framing. There's a more complex
     * version in StylesEditorAction. This will kick out a VERY LARGE errorMessage.
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

            // check for lineNumber -1 errors  --> invalid XML
            if (errors.size() > 0) {
                SAXParseException sax = (SAXParseException) errors.get(0);

                if (sax.getLineNumber() < 0) {
                    result.append("   INVALID XML: " + sax.getLocalizedMessage() + "\n");
                    result.append(" \n");
                    exceptionNum = 1; // skip ahead (you only ever get one error in this case)
                }
            }

            while (line != null) {
                line = line.replace('\n', ' ');
                line = line.replace('\r', ' ');

                String header = linenumber + ": ";
                result.append(header + line + "\n"); // record the current line

                boolean keep_going = true;

                while (keep_going) {
                    if ((exceptionNum < errors.size())) {
                        SAXParseException sax = (SAXParseException) errors.get(exceptionNum);

                        if (sax.getLineNumber() <= linenumber) {
                            String head = "---------------------".substring(0, header.length() - 1);
                            String body =
                                    "--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";

                            int colNum = sax.getColumnNumber(); // protect against col 0 problems

                            if (colNum < 1) {
                                colNum = 1;
                            }

                            if (colNum > body.length()) {
                                body =
                                        body + body + body + body + body
                                                + body; // make it longer (not usually required, but
                                // might be for SLD_BODY=... which is all
                                // one line)

                                if (colNum > body.length()) {
                                    colNum = body.length();
                                }
                            }

                            result.append(head + body.substring(0, colNum - 1) + "^\n");
                            result.append(
                                    "       (line "
                                            + sax.getLineNumber()
                                            + ", column "
                                            + sax.getColumnNumber()
                                            + ")"
                                            + sax.getLocalizedMessage()
                                            + "\n");
                            exceptionNum++;
                        } else {
                            keep_going = false; // report later (sax.getLineNumber() > linenumber)
                        }
                    } else {
                        keep_going = false; // no more errors to report
                    }
                }

                line = reader.readLine(); // will be null at eof
                linenumber++;
            }

            for (int t = exceptionNum; t < errors.size(); t++) {
                SAXParseException sax = (SAXParseException) errors.get(t);
                result.append(
                        "       (line "
                                + sax.getLineNumber()
                                + ", column "
                                + sax.getColumnNumber()
                                + ")"
                                + sax.getLocalizedMessage()
                                + "\n");
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

    /**
     * validate a .sld against the schema
     *
     * @param xml input stream representing the .sld file
     * @return list of SAXExceptions (0 if the file's okay)
     */
    public List validateSLD(InputSource xml) {
        URL schemaURL = SLDValidator.class.getResource("/schemas/sld/StyledLayerDescriptor.xsd");
        return ResponseUtils.validate(xml, schemaURL, false, entityResolver);
    }
}
