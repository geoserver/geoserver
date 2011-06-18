/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

/*
 * Created on April 20, 2005
 *
 */
package org.vfny.geoserver.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.xerces.parsers.SAXParser;
import org.geotools.data.wms.request.GetMapRequest;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public class GETMAPValidator {
    public GETMAPValidator() {
    }

    /**
     *  validates against the "normal" location of the schema (ie. ".../capabilities/sld/StyleLayerDescriptor.xsd"
     *  uses the geoserver_home patch
     * @param xml
     * @param req
     * @return
     */
    public List validateGETMAP(InputStream xml) {
        File schemaFile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                "/data/capabilities/sld/GetMap.xsd");

        try {
            return validateGETMAP(xml, schemaFile.toURL().toString());
        } catch (Exception e) {
            ArrayList al = new ArrayList();
            al.add(new SAXException(e));

            return al;
        }
    }

    public static String getErrorMessage(InputStream xml, List errors) {
        return getErrorMessage(new InputStreamReader(xml), errors);
    }

    /**
       *  returns a better formated error message - suitable for framing.
       * There's a more complex version in StylesEditorAction.
       *
       * This will kick out a VERY LARGE errorMessage.
       *
       * @param xml
       * @param errors
       */
    public static String getErrorMessage(Reader xml, List errors) {
        return SLDValidator.getErrorMessage(xml, errors);
    }

    public List validateGETMAP(InputStream xml, String SchemaUrl) {
        return validateGETMAP(new InputSource(xml), SchemaUrl);
    }

    public List validateGETMAP(InputSource xml, ServletContext servContext) {
        File schemaFile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                "/data/capabilities/sld/GetMap.xsd");

        try {
            return validateGETMAP(xml, schemaFile.toURL().toString());
        } catch (Exception e) {
            ArrayList al = new ArrayList();
            al.add(new SAXException(e));

            return al;
        }
    }

    /**
     *  validate a GETMAP against the schema
     *
     * @param xml  input stream representing the GETMAP file
     * @param SchemaUrl location of the schemas. Normally use ".../capabilities/sld/StyleLayerDescriptor.xsd"
     * @return list of SAXExceptions (0 if the file's okay)
     */
    public List validateGETMAP(InputSource xml, String SchemaUrl) {
        SAXParser parser = new SAXParser();

        try {
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking",
                false);

            parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
                SchemaUrl);
            // parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation","http://www.opengis.net/sld "+SchemaUrl);
            parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                "http://www.opengis.net/ows " + SchemaUrl);

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
            if (!(exception.getMessage()
                               .startsWith("TargetNamespace.2: Expecting no namespace, but the schema document has a target name"))) {
                errors.add(exception);
            }
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
