/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geotools.xsd.PropertyExtractor;
import org.geotools.xsd.SchemaIndex;
import org.geotools.xsd.Schemas;
import org.opengis.feature.type.Name;

/**
 * Extracts properties from an instance of {@link PropertyType}.
 *
 * <p>In a sense this class retypes {@link PropertyType#getValue()} to a new xml type so that the
 * encoder can encode it properly.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class PropertyTypePropertyExtractor implements PropertyExtractor {
    /** index for looking up xml types */
    SchemaIndex index;

    public PropertyTypePropertyExtractor(SchemaIndex index) {
        this.index = index;
    }

    public boolean canHandle(Object object) {
        return object instanceof PropertyType;
    }

    public List properties(Object object, XSDElementDeclaration element) {
        PropertyType property = (PropertyType) object;

        List properties = new ArrayList(2);

        // the Name particle we can use as is
        properties.add(
                new Object[] {
                    Schemas.getChildElementParticle(element.getType(), "Name", false),
                    property.getName()
                });

        // the Value particle we must retype

        // first guess its type
        QName newTypeName = guessValueType(property.getValue());
        XSDTypeDefinition type =
                (newTypeName != null) ? index.getTypeDefinition(newTypeName) : null;

        if (type != null) {
            // create a new particle based on the new type
            XSDElementDeclaration value = XSDFactory.eINSTANCE.createXSDElementDeclaration();
            value.setName("Value");
            value.setTypeDefinition(type);

            XSDParticle particle = XSDFactory.eINSTANCE.createXSDParticle();
            particle.setMinOccurs(1);
            particle.setMaxOccurs(1);
            particle.setContent(value);

            properties.add(new Object[] {particle, property.getValue()});
        } else {
            // coudl not determine new type, just fall back to xs:anyType
            Object[] p =
                    new Object[] {
                        Schemas.getChildElementParticle(element.getType(), "Value", false),
                        property.getValue()
                    };
            properties.add(p);
        }

        return properties;
    }

    private QName guessValueType(Object value) {
        Class clazz = value.getClass();
        List profiles = Arrays.asList(new Object[] {new XSProfile(), new GML3Profile()});

        for (Iterator it = profiles.iterator(); it.hasNext(); ) {
            TypeMappingProfile profile = (TypeMappingProfile) it.next();
            Name name = profile.name(clazz);

            if (name != null) {
                return new QName(name.getNamespaceURI(), name.getLocalPart());
            }
        }

        return null;
    }
}
