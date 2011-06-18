/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDSchemaLocationResolver;


public class WFSSchemaLocationResolver implements XSDSchemaLocationResolver {
    public String resolveSchemaLocation(XSDSchema xsdSchema, String namespaceURI,
        String schemaLocationURI) {
        if (schemaLocationURI == null) {
            return null;
        }

        //if no namespace given, assume default for the current schema
        if (((namespaceURI == null) || "".equals(namespaceURI)) && (xsdSchema != null)) {
            namespaceURI = xsdSchema.getTargetNamespace();
        }

        if ("http://www.opengis.net/wfs".equals(namespaceURI)) {
            if (schemaLocationURI.endsWith("WFS-basic.xsd")) {
                return getClass().getResource("WFS-basic.xsd").toString();
            }

            if (schemaLocationURI.endsWith("WFS-transaction.xsd")) {
                return getClass().getResource("WFS-transaction.xsd").toString();
            }
        }

        return null;
    }
}
