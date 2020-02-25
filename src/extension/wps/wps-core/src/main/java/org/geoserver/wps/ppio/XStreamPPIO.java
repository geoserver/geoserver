/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import java.io.InputStream;
import javax.xml.namespace.QName;
import org.geoserver.config.util.SecureXStream;
import org.xml.sax.ContentHandler;

/**
 * Turns beans into xml using XStream (one way). By default it strips package names and have tags
 * start with a capital letter, subclasses can be created to override such behavior
 *
 * @author Andrea Aime - GeoSolutions
 */
public class XStreamPPIO extends XMLPPIO {

    protected XStreamPPIO(Class type, QName element) {
        super(type, type, element);
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        // prepare xml encoding
        XStream xstream = buildXStream();

        // bind with the content handler
        SaxWriter writer = new SaxWriter();
        writer.setContentHandler(handler);

        // write out xml
        xstream.marshal(object, writer);
    }

    /**
     * Subclasses can override the XStream configuration here. By default XStream is setup to strip
     * package names, have tags starts with a capital letter, and flatten out collections
     */
    protected SecureXStream buildXStream() {
        SecureXStream stream =
                new SecureXStream() {
                    protected MapperWrapper wrapMapper(MapperWrapper next) {
                        return new UppercaseTagMapper(new PackageStrippingMapper(next));
                    };
                };

        return stream;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("XML parsing is not supported");
    }

    /** Strips the package names from classes */
    protected static class PackageStrippingMapper extends MapperWrapper {
        public PackageStrippingMapper(Mapper wrapped) {
            super(wrapped);
        }

        public String serializedClass(Class type) {
            return type.getName().replaceFirst(".*\\.", "");
        }
    }

    protected static class UppercaseTagMapper extends MapperWrapper {

        public UppercaseTagMapper(Mapper wrapped) {
            super(wrapped);
        }

        public String serializedMember(Class type, String memberName) {
            char startChar = memberName.charAt(0);
            if (Character.isLowerCase(startChar)) {
                if (memberName.length() > 1) {
                    return Character.toUpperCase(startChar) + memberName.substring(1);
                } else {
                    return String.valueOf(Character.toUpperCase(startChar));
                }
            } else {
                return memberName;
            }
        }

        public String realMember(Class type, String serialized) {
            String fieldName = super.realMember(type, serialized);
            try {
                type.getDeclaredField(fieldName);
                return fieldName;
            } catch (NoSuchFieldException e) {
                char startChar = fieldName.charAt(0);
                if (fieldName.length() > 1) {
                    return Character.toLowerCase(startChar) + fieldName.substring(1);
                } else {
                    return String.valueOf(Character.toLowerCase(startChar));
                }
            }
        }
    }
}
