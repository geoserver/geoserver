/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.ProfileImpl;
import org.geotools.feature.type.SchemaImpl;
import org.geotools.xs.XS;
import org.geotools.xs.XSSchema;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.Schema;

/** A profile of {@link XSSchema} which makes the java class to type mapping unique. */
public class XSProfile extends TypeMappingProfile {
    static Set profiles = new HashSet();

    static {
        Set proper = new HashSet();
        proper.add(name(XS.BYTE)); // Byte.class
        proper.add(name(XS.HEXBINARY)); // byte[].class
        proper.add(name(XS.SHORT)); // Short.class
        proper.add(name(XS.INT)); // Integer.class
        proper.add(name(XS.FLOAT)); // Float.class
        proper.add(name(XS.LONG)); // Long.class
        proper.add(name(XS.QNAME)); // Qname.class
        proper.add(name(XS.DATE)); // java.sql.Date.class
        proper.add(name(XS.DATETIME)); // java.sql.Timestamp.class
        proper.add(name(XS.TIME)); // java.sql.Time.class
        proper.add(name(XS.BOOLEAN)); // Boolean.class
        proper.add(name(XS.DOUBLE)); // Double.class
        proper.add(name(XS.STRING)); // String.class
        proper.add(name(XS.INTEGER)); // BigInteger.class
        proper.add(name(XS.DECIMAL)); // BigDecimal.class
        proper.add(name(XS.ANYURI)); // URI.class
        profiles.add(new ProfileImpl(new XSSchema(), proper));

        // date mappings between java and xml schema are kind of messed up, so
        // we create a custom schema which also contains a mapping for
        // java.util.Date
        Schema additional = new SchemaImpl(XS.NAMESPACE);
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        ab.setName("date");
        ab.setBinding(Date.class);

        additional.put(name(XS.DATETIME), ab.buildType());
        profiles.add(new ProfileImpl(additional, Collections.singleton(name(XS.DATETIME))));

        // profile.add(name(XS.ANYTYPE)); //Map.class
    }

    static Name name(QName qName) {
        return new NameImpl(qName.getNamespaceURI(), qName.getLocalPart());
    }

    public XSProfile() {
        super(profiles);
    }
}
