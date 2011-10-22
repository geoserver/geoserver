/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.logging.Logging;
import org.geotools.xml.PropertyExtractor;
import org.geotools.xs.XSConfiguration;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Extracts the extra four properties out of a versioned data type. To be used
 * for encoding GetVersionedFeature output
 * @author Andrea Aime
 *
 */
public class VersionedFeaturePropertyExtractor implements PropertyExtractor {
    private static final String XSD_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final Logger LOGGER = Logging
            .getLogger("org.geoserver.wfsv.xml.v1_1_0");

    private static final XSDParticle CR_VERSION;

    private static final XSDParticle CR_AUTHOR;

    private static final XSDParticle CR_DATE;

    private static final XSDParticle CR_MESSAGE;
    
    private static final XSDParticle LU_VERSION;

    private static final XSDParticle LU_AUTHOR;

    private static final XSDParticle LU_DATE;

    private static final XSDParticle LU_MESSAGE;

    static {
        XSDSchema schema = new XSConfiguration().schema();
        CR_VERSION = particle(schema, "creationVersion", XSD_SCHEMA, "string", true, 0, 1);
        CR_AUTHOR = particle(schema, "createdBy", XSD_SCHEMA, "string", true, 0, 1);
        CR_DATE = particle(schema, "creationDate", XSD_SCHEMA, "dateTime", true, 0, 1);
        CR_MESSAGE = particle(schema, "creationMessage", XSD_SCHEMA, "string", true, 0, 1);
        LU_VERSION = particle(schema, "lastUpdateVersion", XSD_SCHEMA, "string", true, 0, 1);
        LU_AUTHOR = particle(schema, "lastUpdatedBy", XSD_SCHEMA, "string", true, 0, 1);
        LU_DATE = particle(schema, "lastUpdateDate", XSD_SCHEMA, "dateTime", true, 0, 1);
        LU_MESSAGE = particle(schema, "lastUpdateMessage", XSD_SCHEMA, "string", true, 0, 1);
    }

    static XSDParticle particle(XSDSchema schema, String elementName,
            String typeNS, String typeName, boolean nillable, int minOccurs,
            int maxOccurs) {
        XSDFactory factory = XSDFactory.eINSTANCE;
        XSDElementDeclaration element = factory.createXSDElementDeclaration();
        element.setName(elementName);
        element.setNillable(nillable);

        XSDTypeDefinition type = schema.resolveTypeDefinition(typeNS, typeName);
        element.setTypeDefinition(type);

        XSDParticle particle = factory.createXSDParticle();
        particle.setMinOccurs(minOccurs);
        particle.setMaxOccurs(maxOccurs);
        particle.setContent(element);
        return particle;
    }

    Catalog catalog;

    public VersionedFeaturePropertyExtractor(Catalog catalog) {
        this.catalog = catalog;
    }

    public boolean canHandle(Object object) {
        try {
            if (!(object instanceof SimpleFeature)
                    || object instanceof FeatureCollection)
                return false;

            SimpleFeature f = (SimpleFeature) object;
            FeatureTypeInfo info = catalog.getFeatureTypeByName(
                f.getFeatureType().getName().getNamespaceURI(), 
                f.getFeatureType().getTypeName() 
            );
            return info != null
                    && info.getFeatureSource(null,null) instanceof VersioningFeatureSource;
        } catch (Exception e) {
            LOGGER
                    .log(
                            Level.FINE,
                            "Error occurred trying to determine versioning status of a feature type",
                            e);
            return false;
        }
    }

    public List properties(Object object, XSDElementDeclaration elem) {
        SimpleFeature f = (SimpleFeature) object;
        List particles = new ArrayList();
        particles.add(particleValue(f, CR_VERSION));
        particles.add(particleValue(f, CR_AUTHOR));
        particles.add(particleValue(f, CR_DATE));
        particles.add(particleValue(f, CR_MESSAGE));
        particles.add(particleValue(f, LU_VERSION));
        particles.add(particleValue(f, LU_AUTHOR));
        particles.add(particleValue(f, LU_DATE));
        particles.add(particleValue(f, LU_MESSAGE));
        return particles;
    }

    private Object[] particleValue(SimpleFeature f, XSDParticle particle) {
        return new Object[] { particle, f.getAttribute(((XSDElementDeclaration) particle.getContent()).getName()) };
    }

}
