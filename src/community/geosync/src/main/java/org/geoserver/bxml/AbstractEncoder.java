package org.geoserver.bxml;

import java.io.IOException;
import java.util.Date;

import javax.xml.namespace.QName;

import org.geotools.feature.type.DateUtil;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.springframework.util.Assert;

public abstract class AbstractEncoder<T> implements Encoder<T> {

    public AbstractEncoder() {
        super();
    }

    /**
     * @see org.geoserver.bxml.Encoder#encode(T, org.gvsig.bxml.stream.BxmlStreamWriter)
     */
    @Override
    public abstract void encode(final T obj, final BxmlStreamWriter w) throws IOException;

    protected void startElement(BxmlStreamWriter w, QName element) {
        try {
            w.writeStartElement(element.getNamespaceURI(), element.getLocalPart());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void endElement(BxmlStreamWriter w) {
        try {
            w.writeEndElement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void element(BxmlStreamWriter w, QName element, boolean encodeIfNull, Date date) {
        element(w, element, encodeIfNull, date, false);
    }

    /**
     * Encodes a {@code java.util.Date} using {@link DateUtil#serializeDateTime(long, boolean)} as
     * the closest to <a href="http://tools.ietf.org/html/rfc3339">rfc3339</a> that I know of.
     */
    protected final void element(BxmlStreamWriter w, QName element, boolean encodeIfNull,
            Date date, boolean stringTable) {
        if (date != null) {
            String value = DateUtil.serializeDateTime(date.getTime(), true);
            element(w, element, encodeIfNull, value, stringTable);
        }
    }

    protected final void element(BxmlStreamWriter w, QName element, boolean encodeIfNull,
            String value) {
        element(w, element, encodeIfNull, value, false);
    }

    protected void element(BxmlStreamWriter w, QName element, boolean encodeIfNull, String value,
            boolean stringTable) {
        element(w, element, encodeIfNull, value, stringTable, false, (String[]) null);
    }

    protected final void element(BxmlStreamWriter w, QName element, boolean encodeIfNull,
            String value, String... attributes) {
        element(w, element, encodeIfNull, value, false, false, attributes);
    }

    /**
     * @param w
     * @param element
     * @param encodeIfNull
     * @param value
     * @param stringTable
     *            if {@code true} try using a string table reference to encode the value (use only
     *            for highly repetitive values)
     * @param stringTableAtts
     *            if {@code true} try using a string table reference to encode the value (use only
     *            for highly repetitive attributes)
     * @param attributes
     */
    protected void element(BxmlStreamWriter w, QName element, boolean encodeIfNull, String value,
            boolean stringTable, boolean stringTableAtts, String... attributes) {
        if (value == null && !encodeIfNull) {
            return;
        }
        try {
            w.writeStartElement(element.getNamespaceURI(), element.getLocalPart());
            attributes(w, stringTableAtts, attributes);
            if (value != null) {
                if (stringTable && w.supportsStringTableValues()) {
                    long stringTableReference = w.getStringTableReference(value);
                    w.writeStringTableValue(stringTableReference);
                } else {
                    w.writeValue(value);
                }
            }
            w.writeEndElement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void attributes(BxmlStreamWriter w, String... attributes) {
        attributes(w, false, attributes);
    }

    protected void attributes(BxmlStreamWriter w, boolean stringTable, String... attributes) {

        if (attributes != null && attributes.length > 0) {
            Assert.isTrue(attributes.length % 2 == 0,
                    "Didn't get an even set of attributes key/value pairs");
            try {
                long[] valueRefs = null;
                if (w.supportsStringTableValues()) {
                    valueRefs = new long[attributes.length];
                    for (int i = 0; i < attributes.length; i += 2) {
                        long valueRef = w.getStringTableReference(attributes[i + 1]);
                        valueRefs[i] = valueRef;
                    }
                }
                for (int i = 0; i < attributes.length; i += 2) {
                    w.writeStartAttribute("", attributes[i]);
                    if (w.supportsStringTableValues()) {
                        w.writeStringTableValue(valueRefs[i]);
                    } else {
                        w.writeValue(attributes[i + 1]);
                    }
                }
                w.writeEndAttributes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}