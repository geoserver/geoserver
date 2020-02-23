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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.URLs;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GETMAPValidator {

    public GETMAPValidator() {}

    /**
     * validates against the "normal" location of the schema (ie.
     * ".../capabilities/sld/StyleLayerDescriptor.xsd" uses the geoserver_home patch
     */
    public List validateGETMAP(InputStream xml) {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        Resource schema = loader.get("data/capabilities/sld/GetMap.xsd");
        File schemaFile = schema.file();
        try {
            return validateGETMAP(xml, URLs.fileToUrl(schemaFile));
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
     * returns a better formated error message - suitable for framing. There's a more complex
     * version in StylesEditorAction.
     *
     * <p>This will kick out a VERY LARGE errorMessage.
     */
    public static String getErrorMessage(Reader xml, List errors) {
        return SLDValidator.getErrorMessage(xml, errors);
    }

    public List validateGETMAP(InputStream xml, URL SchemaUrl) {
        return validateGETMAP(new InputSource(xml), SchemaUrl);
    }

    public List validateGETMAP(InputSource xml, ServletContext servContext) {

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        Resource schema = loader.get("data/capabilities/sld/GetMap.xsd");
        File schemaFile = schema.file();

        //        File schemaFile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
        //                "/data/capabilities/sld/GetMap.xsd");

        try {
            return validateGETMAP(xml, URLs.fileToUrl(schemaFile));
        } catch (Exception e) {
            ArrayList al = new ArrayList();
            al.add(new SAXException(e));

            return al;
        }
    }

    /**
     * validate a GETMAP against the schema
     *
     * @param xml input stream representing the GETMAP file
     * @param SchemaUrl location of the schemas. Normally use
     *     ".../capabilities/sld/StyleLayerDescriptor.xsd"
     * @return list of SAXExceptions (0 if the file's okay)
     */
    public List validateGETMAP(InputSource xml, URL SchemaUrl) {
        EntityResolverProvider provider = GeoServerExtensions.bean(EntityResolverProvider.class);
        return ResponseUtils.validate(xml, SchemaUrl, true, provider.getEntityResolver());
    }
}
