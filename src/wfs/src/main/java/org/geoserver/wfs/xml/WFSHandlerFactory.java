/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.eclipse.xsd.XSDAttributeDeclaration;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.xsd.impl.AttributeHandler;
import org.geotools.xsd.impl.DocumentHandler;
import org.geotools.xsd.impl.ElementHandler;
import org.geotools.xsd.impl.ElementHandlerImpl;
import org.geotools.xsd.impl.Handler;
import org.geotools.xsd.impl.HandlerFactory;
import org.geotools.xsd.impl.ParserHandler;

/**
 * Special handler factory which creates handlers for elements which are defined as wfs feature
 * types.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WFSHandlerFactory implements HandlerFactory {
    static Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    /** Catalog reference */
    Catalog catalog;

    /** Schema Builder */
    FeatureTypeSchemaBuilder schemaBuilder;

    public WFSHandlerFactory(Catalog catalog, FeatureTypeSchemaBuilder schemaBuilder) {
        this.catalog = catalog;
        this.schemaBuilder = schemaBuilder;
    }

    public DocumentHandler createDocumentHandler(ParserHandler parser) {
        return null;
    }

    public ElementHandler createElementHandler(QName name, Handler parent, ParserHandler parser) {
        String namespaceURI = name.getNamespaceURI();

        if (namespaceURI == null) {
            // assume default
            namespaceURI = catalog.getDefaultNamespace().getURI();
        }

        try {
            // look for a FeatureType
            FeatureTypeInfo meta = catalog.getFeatureTypeByName(namespaceURI, name.getLocalPart());

            if (meta != null) {
                // found it
                XSDSchema schema = schemaBuilder.build(meta, null);

                for (Iterator e = schema.getElementDeclarations().iterator(); e.hasNext(); ) {
                    XSDElementDeclaration element = (XSDElementDeclaration) e.next();

                    if (name.getLocalPart().equals(element.getName())) {
                        return new ElementHandlerImpl(element, parent, parser);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error building schema", e);
        }

        return null;
    }

    public ElementHandler createElementHandler(
            XSDElementDeclaration e, Handler parent, ParserHandler parser) {
        return null;
    }

    public AttributeHandler createAttributeHandler(
            XSDAttributeDeclaration a, Handler parent, ParserHandler parser) {
        return null;
    }
}
